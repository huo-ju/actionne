(ns actionne.core-test
  (:require [clojure.test :refer :all]
            [actionne.core :refer :all]
            [clara.rules :refer :all]))

(def rules1
"Ver 1.0.0
Namespace huoju/actionne_twitter
Desc testsnsrules
Do delete created_at laterthan 1 hour category = str:reply favorite_count < 20 id != str:1111111111111111111 id != str:1111111111111111112
")

(def items1
[{:id "1173326296325251083", :object {:id "1173326296325251083", :favorite_count 1, :retweet_count 0, :text "this is my test reply 1", :created_at 1568577763000, :media_urls [], :category "reply"} :original {}},
{:id "1173323074497363969", :object {:id "1173293074497363969", :favorite_count 4, :retweet_count 0, :text "this is my test tweet 2" :created_at 1568569842000, :media_urls [], :category "tweet"} :original {}} ]
) 

(def items2
[{:id "1173326296325251083", :object {:id "1111111111111111111", :favorite_count 1, :retweet_count 0, :text "this is my test reply 1", :created_at 1568577763000, :media_urls [], :category "reply"} :original {}},
{:id "1173323074497363969", :object {:id "1173293074497363969", :favorite_count 4, :retweet_count 0, :text "this is my test tweet 2" :created_at 1568569842000, :media_urls [], :category "tweet"} :original {}} ]
) 


(deftest verify_item1 
  (testing "item1 match delete constraints in rules1"
    (let [transformedscript (scripttransform rules1)]
        (let [facts (map (fn [item] (->Msgs (:id item) (:object item) (:original item))) items1)]
            (let [session (-> (mk-session 'actionne.core transformedscript)
                              (insert-all (into [] facts))
                              (fire-rules))]
                (let [actionmsgs (query session get-actionmsgs)]
                (let [{{action :action id :id original :original} :?msg} (first actionmsgs)]
                    (is (= "1173326296325251083" id))
                )))))))

(deftest verify_item12
  (testing "item1 not match any constraints in rules1"
    (let [transformedscript (scripttransform rules1)]
        (let [facts (map (fn [item] (->Msgs (:id item) (:object item) (:original item))) items2)]
            (let [session (-> (mk-session 'actionne.core transformedscript)
                              (insert-all (into [] facts))
                              (fire-rules))]
                (let [actionmsgs (query session get-actionmsgs)]
                (let [{{action :action id :id original :original} :?msg} (first actionmsgs)]
                    (is (= nil id))
                )))))))

