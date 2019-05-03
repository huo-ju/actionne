(ns snsguardian.plugins.twitter
  (:gen-class)
  (:require 
            [config.core :refer [env]]
            [twttr.api :as api]
            [twttr.auth :refer :all]
  )
)


(def creds (map->UserCredentials  (:twitter env) ))


(defn fetchtweets []
    (let [tweets (api/statuses-user-timeline creds :params {:screen_name "virushuo"})]
        (doseq [tweet tweets] 
            (prn tweet)
            (prn "====")
        )
        
    )
)
