(ns snsguardian.core
  (:gen-class)
  (:require [clojure.string :as string]
            [instaparse.core :as insta]
            [clara.rules.accumulators :as acc]
            [clara.rules :refer :all]
  ))

(defrecord Msgs [id target obj])

(defrecord ActionMsgs [action id])


(defquery get-actionmsgs []
 [?msg <- ActionMsgs]
)

(defquery get-msgs []
 [?msg <- Msgs]
)


(def example-rules
  "Ver 1.0.0
   Namespace huoju/twitter
   Desc testsnsrules
   Del reply like > 5 rt > 10
   Do remove like > 5 rt > 10
   Do notify like = 1
   Do notify rt = 15
   Do show name include str:aaa
")

(def parser
  (insta/parser
   "<statement> = ver namespace [ desc | del | action | test]+
    ver = <'Ver'> space MAJOR <'.'> MINOR <'.'> PATCH [META]
    namespace = <'Namespace'> space NSIDENTIFIER <'/'> NSLOCALNAME
    desc = <'Desc'> space utf8stre
    action = <'Do'> string space clause [clause]*
    del  = <'Del'> target space clause [clause]*
    test  = <'Test'> target
    clause = string symbol [ digit | strvar ] ?[unit]
    unit = 'min' | 'day';
    logic = 'and' | 'or' | 'not';
    symbol = '>' | '<' | '=' | '>=' | '<=' | '!=' | 'include';
    target = 'tweet' | 'reply';
    <percent> = #'[0-9]\\d?(?:\\.\\d{1,2})?%';
    <string> = #'[A-Za-z0-9_-]+';
    <space> = <#'[ ]+'>;
    <utf8str> = #'([^\r\n\"\\\\]|\\s\\\\.,)+';
    <utf8stre> = #'([^\r\n\"]|\\s\\\\.,)+';
    strvar = <'str:'> utf8stre
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

(def symbol-operator {"=" `=
                ">" `>
                "<" `<
                ">=" `>=
                "<=" `<=
                "!=" `not=
                "include" `string/includes?
                })

(def logic-operator {"and" `:and
"or" `:or
"not" `:not
})

(def msg-types
  {"msgs" Msgs
  })

(def transform-options
    {
   :desc (fn [thedesc]
              {:name "desc"
               :lhs ()
               :rhs `(print-desc ~thedesc)})
   :namespace (fn [identifier localname] (into {} [identifier localname]))
   :unit read-string
   :clause (fn [property operator value & unit ]
    {
        :type Msgs
        :constraints [(list `= (symbol "?msgid") (symbol "id")) (list operator  (list (keyword property) (symbol "obj")) value)]
    }
   )
   :symbol symbol-operator
   :strvar (fn [input]
    :lhs ()
    :rhs `~input
   )
   :logic logic-operator
   :digit #(Integer/parseInt %)
   :target (fn [target-type] {
        :type Msgs
        :constraints [ (list `= (symbol "?target") (symbol "id")) (list `= (symbol "target") target-type)] 
    })
   :action (fn [action & clauses]
             {
            :lhs clauses;'~clauses
            :rhs `(insert! (->ActionMsgs ~action ~(symbol "?msgid")))})
})

(defn doaction [dslns msg]
    (println "do action:")
    (let [{identifier :NSIDENTIFIER  localname :NSLOCALNAME} dslns]
        (let [{ {action :action id :id} :?msg} msg]
        (println "run ns: " (str identifier "." localname))
        (println action id))
    )
)
(defn -main [& args]

    (let [parse-tree (parser example-rules)]
        
        (let [transformed (insta/transform transform-options parse-tree)]
            (clojure.pprint/pprint transformed)
            (let [[ver dslns]  transformed]
                (let [session (-> (mk-session 'snsguardian.core transformed)
                              (insert (->Msgs "msg1" "reply" {:like 10 :rt 12 :name "test"}))
                              (insert (->Msgs "msg2" "reply" {:like 1 :rt 10 :name "111paaabbb"}))
                              (insert (->Msgs "msg3" "reply" {:like 6 :rt 15 :name "111222"})) 
                              (fire-rules))]
                (println "====action")
                (let [actionmsgs (query session get-actionmsgs)]
                    (map (fn [msg] (doaction dslns msg)) actionmsgs)
                )
                )
            )
        )
    )
)

