(ns snsguardian.approutes
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer [GET POST PUT DELETE]]
            [ring.util.http-response :refer [ok not-found created]]
            [tea-time.core :as tt]
  )
)

(def app-routes
  [
    (GET "/hello" []
      (ok {:message (str "Hello " )}))
    (GET "/stop" []
      (tt/stop!)
      (ok {:message (str "stop" )}))
   ])
