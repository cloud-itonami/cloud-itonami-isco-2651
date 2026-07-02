# cloud-itonami-isco-2651

Open Occupation Blueprint for **ISCO-08 2651**: Visual Artists (painters,
sculptors, cartoonists — including manga artists / 漫画家).

This repository designs a forkable OSS business for an independent visual
artist or manga studio: the studio keeps its own storyboard, page, character
and licensing records instead of renting a closed studio-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a print-and-scan robot performs physical proof
printing, original-art (原画) scanning and artwork packaging under an actor
that proposes actions and an independent **Visual Art Governor** that gates
them. The governor never dispatches hardware itself; `:high`/`:safety-critical`
actions (such as using copyrighted reference without license verification, or
shipping an original artwork) require human sign-off.

A live sample of the operator console (robotics safety console, shared
template) is rendered in
[docs/samples/operator-console.html](docs/samples/operator-console.html) —
pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
story brief + character sheets + page order
        |
        v
Art Advisor -> Visual Art Governor -> draw/compose/deliver, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or license artwork without governor approval and audit
evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `2651`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

Craft libraries (public, kotoba-lang):
[`kami-genko`](https://github.com/kotoba-lang/kami-genko) (manga-editor
document model) and the
[`kami-mangaka-*`](https://github.com/kotoba-lang/kami-mangaka-page-clj)
family (page composition / lettering / scene / render / reader). The private
reference implementation is gftdcojp's `ai-gftd-mangaka` actor
(ADR-2607023000: コードは kotoba-lang、職能は cloud-itonami-isco、商売は
gftdcojp).

## Reference actor (`:maturity :implemented`)

Full itonami Actor pattern (like
[`cloud-itonami-isco-6130`](https://github.com/cloud-itonami/cloud-itonami-isco-6130) /
[`-2652`](https://github.com/cloud-itonami/cloud-itonami-isco-2652)): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph` with Advisor and Governor as distinct nodes and human-in-the-loop
interrupt/resume. The governor validates delivered 原稿 (genko) documents
against the [`kami-genko`](https://github.com/kotoba-lang/kami-genko)
document-model vocabulary (kotoba-lang craft lib, ADR-2607020300 /
ADR-2607023000) — **a doc carrying node types outside the genko model is a
HARD hold**, and page-delivery commits carry page/node counts derived from
the same model.

- HARD → `:hold`: unregistered work, non-`:propose` effect, invalid genko
  node types.
- ESCALATE → `:request-approval` (human-signed): original-artwork shipment,
  artwork licensing, low confidence.

```bash
clojure -M:test
```

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
