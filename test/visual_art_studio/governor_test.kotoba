(ns visual-art-studio.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [visual-art-studio.governor :as governor]
            [visual-art-studio.store :as store]))

(defn- fresh-store []
  (-> (store/mem-store)
      (store/register-work! {:work/id "work:ghost-hacker"
                             :work/title "Ghost Hacker"
                             :originals? true})))

(def valid-doc
  {:name "gh-page-01"
   :pages [{:id "p1" :name "P1"
            :youshi {:id "y1" :type "b4manga" :visible true}
            :nodes [{:id "n1" :type "panel" :visible true :data {:_nid "n1"}}
                    {:id "n2" :type "fukidashi" :visible true :data {:_nid "n2"}}
                    {:id "n3" :type "text" :visible true :data {:_nid "n3"}}]}]})

(def invalid-doc
  (assoc-in valid-doc [:pages 0 :nodes 1 :type] "blob-monster"))

(defn- proposal [op] {:op op :effect :propose :stake :low :confidence 0.95})

(deftest ok-on-valid-page-delivery
  (let [v (governor/check {:work-id "work:ghost-hacker" :doc valid-doc}
                          {} (proposal :deliver-page) (fresh-store))]
    (is (:ok? v))))

(deftest hard-holds
  (testing "unregistered work"
    (let [v (governor/check {:work-id "work:no-such" :doc valid-doc}
                            {} (proposal :deliver-page) (fresh-store))]
      (is (:hard? v))
      (is (some #(= :no-work (:rule %)) (:violations v)))))
  (testing "non-propose effect"
    (let [v (governor/check {:work-id "work:ghost-hacker" :doc valid-doc}
                            {} (assoc (proposal :deliver-page) :effect :write!)
                            (fresh-store))]
      (is (:hard? v))))
  (testing "doc with a node type outside the kami-genko document model"
    (let [v (governor/check {:work-id "work:ghost-hacker" :doc invalid-doc}
                            {} (proposal :deliver-page) (fresh-store))]
      (is (:hard? v))
      (is (some #(= :invalid-genko-node (:rule %)) (:violations v)))
      (is (= ["blob-monster"] (governor/unknown-node-types invalid-doc))))))

(deftest escalations
  (testing "original shipment is human-signed"
    (let [v (governor/check {:work-id "work:ghost-hacker"}
                            {} (proposal :ship-original) (fresh-store))]
      (is (not (:hard? v)))
      (is (:escalate? v))))
  (testing "artwork licensing is human-signed"
    (let [v (governor/check {:work-id "work:ghost-hacker"}
                            {} (proposal :license-artwork) (fresh-store))]
      (is (:escalate? v))))
  (testing "low confidence"
    (let [v (governor/check {:work-id "work:ghost-hacker" :doc valid-doc}
                            {} (assoc (proposal :deliver-page) :confidence 0.2)
                            (fresh-store))]
      (is (:escalate? v)))))
