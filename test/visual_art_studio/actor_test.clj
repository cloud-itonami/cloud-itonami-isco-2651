(ns visual-art-studio.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [visual-art-studio.actor :as actor]
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

(deftest commits-a-valid-page-delivery
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:work-id "work:ghost-hacker" :op :deliver-page
                 :doc valid-doc :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (let [record (get-in result [:state :record])]
      (is (= {:pages 1 :nodes 3} (:delivered record))))
    (is (= 1 (count (store/records-of st "work:ghost-hacker"))))))

(deftest holds-invalid-genko-doc-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:work-id "work:ghost-hacker" :op :deliver-page
                 :doc (assoc-in valid-doc [:pages 0 :nodes 0 :type] "not-a-node")
                 :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "work:ghost-hacker")))))

(deftest interrupts-original-shipment-until-approved
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:work-id "work:ghost-hacker" :op :ship-original :stake :medium}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "work:ghost-hacker")))
    (testing "approval (= human-signed shipment) resumes to commit"
      (let [resumed (actor/approve! graph "thread-3")]
        (is (= :done (:status resumed)))
        (is (= :ship-original (:op (get-in resumed [:state :record]))))
        (is (= 1 (count (store/records-of st "work:ghost-hacker"))))))))
