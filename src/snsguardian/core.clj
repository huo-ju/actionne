(ns snsguardian.core
  (:gen-class)
  (:require [instaparse.core :as insta]
            [clara.rules.accumulators :as acc]
            [clara.rules :refer :all]
  ))

(defrecord Msgs [id target obj])

(defrecord DelMsgs [id])

;;(defrecord Total [value])

;;(defrule matchdelmsgs
;;  ;;[?msg <- Msgs (= "reply" type)]
;;  [?msg <- Msgs (>= 20 (:like obj))]
;;  =>
;;  (insert! (->DelMsgs (:id ?msg))))

(defquery get-delmsgs []
 [?msgs <- DelMsgs]
)

(defquery get-msgs []
 [?msgs <- Msgs]
)

;;(defquery get-total []
;; [?total <- Total]
;;)


;;(defrule totalval
;;  [?total <- (acc/sum :value) :from [Msgs (= 1 2)]]
;;  =>
;;(insert! (->Total ?total)))


(def example-rules
  "Desc testsnsrules
   Del reply like > 5 
")
   ;;Del reply time > 10min
   ;;Del tweet time > 1day and like < 5
(def parser
  (insta/parser
   "<statement> = [ desc | del | test]+
    desc = <'Desc'> space utf8stre
    del  = <'Del'> target space clause [logic clause]*
    test  = <'Test'> target
    clause = string symbol digit ?[unit]
    unit = 'min' | 'day';
    logic = 'and' | 'or' | 'not';
    symbol = '>' | '<' | '=' | '>=' | '<=' | '!=';
    target = 'tweet' | 'reply';
    <percent> = #'[0-9]\\d?(?:\\.\\d{1,2})?%';
    <string> = #'[A-Za-z0-9_-]+';
    <space> = <#'[ ]+'>;
    <utf8str> = #'([^\r\n\"\\\\]|\\s\\\\.,)+';
    <utf8stre>= #'([^\r\n\"]|\\s\\\\.,)+';
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
                "!=" `not=})

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
;;[?msg <- Msgs (= "reply" type)]
   :clause (fn [property operator value & unit ]
    {
        :type Msgs
        ;:constraints [(list operator value (if (= unit nil) "" (apply str unit)))]

        :constraints [(list `= (symbol "?msgid") (symbol "id")) (list operator  (list (keyword property) (symbol "obj")) value)]
    }
   )
   :symbol symbol-operator
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
                      (insert (->Msgs "msg1" "reply" {:like 10 :rt 20}))
                      (insert (->Msgs "msg2" "reply" {:like 1 :rt 10}))
                      (insert (->Msgs "msg3" "reply" {:like 3 :rt 10})) 
                      (fire-rules))]
        (println "====")
        (clojure.pprint/pprint (query session get-delmsgs))
        ;;(clojure.pprint/pprint (query session get-total))
        )
    )

)

