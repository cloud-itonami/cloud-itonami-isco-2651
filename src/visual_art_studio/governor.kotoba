(ns visual-art-studio.governor
  "VisualArtGovernor — the independent document-integrity/licensing layer
  for the ISCO-08 2651 independent visual artist / manga studio actor.
  Wired as its own `:govern` node in `visual-art-studio.actor`'s StateGraph,
  downstream of `:advise` — the Advisor has no notion of work provenance,
  genko document validity, or licensing terms, so this MUST be a separate
  system able to reject a proposal (itonami actor pattern, per
  ADR-2607011000 / CLAUDE.md Actors section).

  The document check is NOT bespoke: a delivered 原稿 (genko) document is
  validated against `kami.mangaka.genko`'s node-type vocabulary from the
  kotoba-lang `kami-genko` craft lib (ADR-2607020300 / ADR-2607023000) —
  a doc carrying node types outside the genko document model is
  structurally refused.

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. work provenance — the request's work must be registered.
    2. no-actuation    — proposal :effect must be :propose.
    3. genko validity  — a :deliver-page doc must only contain known
       genko node types.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    4. :ship-original / :license-artwork — physical originals and
       licensing terms are human-signed.
    5. low confidence (< `confidence-floor`)."
  (:require [kami.mangaka.genko :as genko]
            [visual-art-studio.store :as store]))

(def confidence-floor 0.6)

(defn unknown-node-types
  "Node types appearing in the genko doc that are outside
  `kami.mangaka.genko/node-types`."
  [doc]
  (->> (:pages doc)
       (mapcat :nodes)
       (map :type)
       (remove genko/node-types)
       distinct
       vec))

(defn- hard-violations [{:keys [request proposal]} work-record]
  (let [unknown (when (= :deliver-page (:op proposal))
                  (unknown-node-types (:doc request)))]
    (cond-> []
      (nil? work-record)
      (conj {:rule :no-work :detail (str "未登録 work " (:work-id request))})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (seq unknown)
      (conj {:rule :invalid-genko-node
             :detail (str "genko document model 外の node type: " unknown)}))))

(def ^:private human-signed-ops #{:ship-original :license-artwork})

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a `store`
  implementing `visual-art-studio.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request _context proposal store]
  (let [work-record (store/work store (:work-id request))
        hard (hard-violations {:request request :proposal proposal} work-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        signed? (contains? human-signed-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not signed?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? signed?))}))
