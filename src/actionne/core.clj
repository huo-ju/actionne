(ns actionne.core
  (:gen-class)
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [config.core :refer [env]]
            [instaparse.core :as insta]
            [clara.rules.accumulators :as acc]
            [clara.rules :refer :all]
            [tea-time.core :as tt]
            [compojure.api.sweet :refer [api routes]]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [dynapath.util :as dynapath]
            [clojure.java [classpath :as classpath]]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.java.io :as io]
            [actionne.classpath])
  (:import (java.util Date Locale) java.io.PushbackReader))

(defrecord Msgs [id obj original])

(defrecord ActionMsgs [action id original])

(defquery get-actionmsgs []
  [?msg <- ActionMsgs])

(defquery get-msgs []
  [?msg <- Msgs])

(def parser
  (insta/parser
   "<statement> = ver namespace [ desc | action ]+
    ver = <'Ver'> space MAJOR <'.'> MINOR <'.'> PATCH [META]
    namespace = <'Namespace'> space NSIDENTIFIER <'/'> NSLOCALNAME
    desc = <'Desc'> space utf8stre
    action = <'Do'> string space clause [clause]*
    clause = string symbol [ digit | strvar ] ?[unit]
    unit = 'minute' | 'day' | 'hour';
    logic = 'and' | 'or' | 'not';
    symbol = '>' | '<' | '=' | '>=' | '<=' | '!=' | 'include' | 'notinclude' | 'laterthan';
    <percent> = #'[0-9]\\d?(?:\\.\\d{1,2})?%';
    <string> = #'[A-Za-z0-9_-]+';
    <space> = <#'[ ]+'>;
    <utf8str> = #'([^\r\n\"\\\\]|\\s\\\\.,)+';
    <utf8stre> = #'([^\r\n\"]|\\s\\\\.,)+';
    <utf8strwithoutspace> = #'([^\r\n\" ]|\\s\\\\.,)+';
    strvar = <'str:'> utf8strwithoutspace
    <float> = #'[0-9]+(\\.[0-9]+)?';
    digit = #'[0-9]+';
    MAJOR = digit
    MINOR = digit
    PATCH = digit
    META = #'[0-9A-Za-z-]'
    NSIDENTIFIER = string
    NSLOCALNAME = string
    <nl> =  #'[\r\n]+';
  "
   :auto-whitespace :standard))

(defn print-desc [input]
  (println (str "desc: " input)))

(def df
  (java.text.SimpleDateFormat. "EEE MMM dd HH:mm:ss ZZZZZ yyyy" (Locale. "english")))

(def time-to-minutes
  {"minute" `1
   "minutes" `1
   "hour" `60
   "hours" `60
   "day" `1440
   "days" `1440})

(defn timelaterthan [left right unit]
  (let [offset (- (.getTime (new java.util.Date)) left)]
    (if (>= (quot offset (* 60 1000)) (* right (time-to-minutes unit)))
      true
      false)))

(defn notinclude [left right]
  (not (string/includes? left right)))

(def symbol-operator {"=" `=
                      ">" `>
                      "<" `<
                      ">=" `>=
                      "<=" `<=
                      "!=" `not=
                      "include" `string/includes?
                      "notinclude" `notinclude
                      "laterthan" `timelaterthan})

(def logic-operator {"and" `:and
                     "or" `:or
                     "not" `:not})

(def msg-types
  {"msgs" Msgs})

(def transform-options
  {:desc (fn [thedesc]
           {:name "desc"
            :lhs ()
            :rhs `(print-desc ~thedesc)})
   :namespace (fn [identifier localname] (into {} [identifier localname]))
   :unit read-string
   :clause (fn [property operator value & unit]
             {:type Msgs
              :constraints [(list `= (symbol "?msgid") (symbol "id"))  (list `= (symbol "?original") (symbol "original")) (remove nil? (list operator (list (keyword property) (symbol "obj")) value (if (not= nil unit) (clojure.string/join unit))))]})

   :symbol symbol-operator
   :strvar (fn [input]
             :lhs ()
             :rhs `~input)
   :logic logic-operator
   :digit #(Integer/parseInt %)
   :action (fn [action & clauses]
             {:lhs clauses
              :rhs `(insert! (->ActionMsgs ~action ~(symbol "?msgid") ~(symbol "?original")))})})

(defn doaction [config dslns msg]
  (let [{identifier :NSIDENTIFIER  localname :NSLOCALNAME} dslns]
    (let [{{action :action id :id original :original} :?msg} msg]
      ((resolve (symbol (str localname ".core/" action))) config id original)
      (log/info (str "action: " identifier "." localname "/" action " " id)))))

(defn expand-home [s]
  (if (.startsWith s "~")
    (clojure.string/replace-first s "~" (System/getProperty "user.home"))
    s))

(def homedir
  (expand-home (or (:homedir env) "~/actionne")))

(defn startcheck []
  (if (not (or (.exists (io/file (str homedir "/data"))) (.exists (io/file (str homedir "/config"))) (.exists (io/file (str homedir "/plugins"))) (.exists (io/file (str homedir "/scripts")))))
    (do
      (.mkdirs (io/file (str homedir "/data")))
      (.mkdirs (io/file (str homedir "/config")))
      (.mkdirs (io/file (str homedir "/plugins")))
      (.mkdirs (io/file (str homedir "/scripts")))))

  (log/info (str "using " homedir " as homedir")))

(defn read-config-file [f]
  (try
    (when-let [url (or (io/resource f) (io/file f))]
      (with-open [r (-> url io/reader PushbackReader.)]
        (edn/read r)))
    (catch Exception e
      (log/warn (str "WARNING: failed to parse " f " " (.getLocalizedMessage e))))))

(defn load-config [& name]
  (let [configname (if (nil? name) "default.edn" (first name))]
    (read-config-file (str homedir "/config/" configname))))

(defn load-scripts [scripts]
  (map (fn [item]
         {:name (key item) :interval (val item) :script (slurp (str homedir "/scripts/" (name (first item)) ".act"))}) scripts))

(defn filteritems [transformedscript items]
  (let [facts (map (fn [item]
                     (->Msgs (:id item) (:object item) (:original item))) items)]
    (let [session (-> (mk-session 'actionne.core transformedscript)
                      (insert-all (into [] facts))
                      (fire-rules))]
      (query session get-actionmsgs))))

(defn runtask [pluginname config transformedscript]

  (let [[ver dslns] transformedscript]
    ((resolve (symbol (str pluginname ".core/before"))) ((keyword pluginname) config))
    (let [items ((resolve (symbol (str pluginname ".core/run"))) ((keyword pluginname) config))]
      (let [actionmsgs (filteritems transformedscript items)]
        (try
          (do
            (dorun (map (fn [msg] (doaction config dslns msg)) actionmsgs))
            ((resolve (symbol (str pluginname ".core/success"))) ((keyword pluginname) config)))
          (catch Exception e
            (log/error (str "doaction error: " (.getLocalizedMessage e)))
            (prn e)))
        (log/info "task done.")))))

(defmacro starttaskmacro [pluginname interval config transformedscript]
  `(let [pluginname# ~pluginname interval# ~interval config# ~config transformedscript# ~transformedscript]
     (def pluginname# (tt/every! interval# (bound-fn []
                                             (runtask pluginname# config# transformedscript#))))))
(defn scripttransform [script]
  (let [parse-tree (parser script)]
    (insta/transform transform-options parse-tree)))

(defn -main [& args]
  (log/info "start...")
  (startcheck)
  (tt/start!)
  (let [config (load-config)]
    (let [scripts (load-scripts (:scripts config))]
      (mapv (fn [scriptobj]
              (let [transformedscript (scripttransform (:script scriptobj))]
                (let [[ver dslns] transformedscript]
                  (let [pluginname (:NSLOCALNAME dslns)]
                    (log/info (str "loading... " homedir "/plugins/" pluginname ".jar"))
                    (actionne.classpath/add-classpath (str homedir "/plugins/" pluginname ".jar"))
                    (require (symbol (str pluginname ".core")))
                    (if (= true ((resolve (symbol (str pluginname ".core/startcheck"))) ((keyword pluginname) config)))
                      (starttaskmacro pluginname (:interval scriptobj) config transformedscript)
                      (log/error (str pluginname " plugin startcheck error: ")))
                    (runtask pluginname config transformedscript)))))
            scripts))))


