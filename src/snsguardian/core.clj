(ns snsguardian.core
  (:gen-class)
  (:require [clojure.string :as string]
            [instaparse.core :as insta]
            [clara.rules.accumulators :as acc]
            [clara.rules :refer :all]
  ))

(defrecord Msgs [id target obj])

(defrecord DelMsgs [id])
(defrecord ActionMsgs [action id])

(defquery get-delmsgs []
 [?msgs <- DelMsgs]
)

(defquery get-actionmsgs []
 [?msgs <- ActionMsgs]
)

(defquery get-msgs []
 [?msgs <- Msgs]
)


(def example-rules
  "Desc testsnsrules
   Del reply like > 5 rt > 10
   Do remove like > 5 rt > 10
   Do notify like = 1
   Do notify rt = 15
   Do show name include str:aaa
")

(def parser
  (insta/parser
   "<statement> = [ desc | del | action | test]+
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
        ;:constraints [`(= 1 1)] 
    })
   :test (fn [& target]{
        :name "test"
        :lhs target;'~clauses
        :rhs `(insert! (->DelMsgs ~(symbol "?target")))
    }) 
   :del (fn [target & clauses]
             {:name "del"
            :lhs clauses;'~clauses
            :rhs `(insert! (->DelMsgs ~(symbol "?msgid")))})
   :action (fn [action & clauses]
             {
            :lhs clauses;'~clauses
            :rhs `(insert! (->ActionMsgs ~action ~(symbol "?msgid")))})
})

;(defrule find-tweet
;    [Msgs (= ?target target) (= target "tweet")] 
;=>
;    (insert! (->DelMsgs ?target))
;)
;;(defn doparse [input]
;;  (->> (parser input) (insta/transform transform-options)))

(defn -main [& args]
    ;;(clojure.pprint/pprint "hello 2")
    ;;(clojure.pprint/pprint example-rules)

    (let [parse-tree (parser example-rules)]
        ;;(clojure.pprint/pprint parse-tree)
        (clojure.pprint/pprint (insta/transform transform-options parse-tree))

        (let [session (-> (mk-session 'snsguardian.core (insta/transform transform-options parse-tree))
                      (insert (->Msgs "msg1" "reply" {:like 10 :rt 12 :name "test"}))
                      (insert (->Msgs "msg2" "reply" {:like 1 :rt 10 :name "111paaabbb"}))
                      (insert (->Msgs "msg3" "reply" {:like 6 :rt 15 :name "111222"})) 
                      (fire-rules))]
        (println "====")
        (clojure.pprint/pprint (query session get-delmsgs))
        (clojure.pprint/pprint (query session get-actionmsgs))
        ;;(clojure.pprint/pprint (query session get-total))
        )
    )

)

