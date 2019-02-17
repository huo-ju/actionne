(ns snsguardian.core
  (:gen-class)
  (:require [instaparse.core :as insta]
            [clara.rules :refer :all]
  ))


(def example-rules
  "Desc testsnsrules
   Del reply time > 10min
   Del tweet time > 1day and like < 5
  ")

(def parser
  (insta/parser
   "<statement> = [ desc | del ]+
    desc = <'Desc'> space utf8stre
    del  = <'Del'> target space clause [logic clause]*
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

(def transform-options
  {
   :desc (fn [& thedesc]
           {:name "desc"
            :lhs ()
            :rhs `(print-desc ~thedesc)})
   :unit read-string
   :clause (fn [property operator value & unit ]
    {
        :property property 
        :constraints [(list operator value (if (= unit nil) "" (apply str unit)))]
    }
   )
   :symbol symbol-operator
   :digit #(Integer/parseInt %)
   :del (fn [& principallist]
             {:name "del"
            :lhs ()
            :rhs `(process-principals (apply list '~principallist))})
})

;;(defn doparse [input]
;;  (->> (parser input) (insta/transform transform-options)))

(defn -main [& args]
    (clojure.pprint/pprint "hello 2")
    (clojure.pprint/pprint example-rules)

    (let [parse-tree (parser example-rules)]
      (clojure.pprint/pprint (insta/transform transform-options parse-tree)))
)
