# Audit: PR #71 — Move the CC BY credit into an About sheet

**PR:** [#71](https://github.com/emmanuel-h/DiceRoller/pull/71) · squash-merged as `f4fe06a`, 15 August 2026
**Closes:** #66 (closed on merge)

## Headline finding

**No pipeline agent ran for this PR. The work was done directly in a single interactive session.**

This is the **third consecutive substantive PR** to skip the agent chain, after
[#43](pr-43-audit.md) (bypassed silently) and [#65](pr-65-audit.md) (bypassed by
explicit user instruction). Unlike #65, this session carries no stated opt-out —
the user simply pasted the issue URL and the work proceeded directly, with no
invocation of `okay-boss` or any subagent.

Three data points in a row is no longer a signal about individual PRs. It is a
statement about the pipeline: **it is not the default path any more, and has not
been for three PRs.**

### Evidence

**Pipeline log (`/tmp/dice-roller-pipeline.log`) — 6 lines, entire contents:**

```
[2026-08-14T11:05:52Z] STOPPED: unknown
[2026-08-14T12:31:57Z] STOPPED: unknown
[2026-08-14T14:02:36Z] STOPPED: unknown
[2026-08-14T14:27:01Z] STOPPED: unknown
[2026-08-15T17:07:45Z] STOPPED: unknown
[2026-08-15T17:10:58Z] STOPPED: unknown
```

Every line is a bare `STOPPED: unknown`. There is not one agent name, start
event, handoff, or output record in the file — a `grep -icE "agent|the-boss|the-spy|architect|dev|review"`
returns **0**. The two `2026-08-15` entries bracket this PR's work (commit
`65c40c7` at 17:10:47, merge at 17:11:38), so the log *was* being written during
this session — it recorded only session stops, never agent activity.

**Session transcript (521 lines):** zero `Task` tool invocations, zero
`subagent_type` values of any kind. No subagent was spawned at any point.

**Therefore this audit makes no claim about which agents ran, what they
produced, or how well they followed their workflows — none ran.** The sections
below assess the work on its own merits instead.

## What actually happened

The full user-side flow, in order:

1. `implement https://github.com/emmanuel-h/DiceRoller/issues/66`
2. *"CC by 4.0 link should be done on the same line than the other exact same wording -> gain place. Also, not sure the OpenGameArt listing is useful here."*
3. *"Artwork should take one line, reduce it again"*
4. `/ship` (skill: commit + push)
5. *"merge and close the issue"*

A direct implement → two rounds of human design review → ship → merge loop. The
design iteration that a pipeline design agent would nominally own was performed
**by the user, in review, in two passes** — and it materially changed the
output: the OpenGameArt listing was dropped and the artwork credit was
compressed to a single line, with the trailing "CC BY 4.0" doing double duty as
the link rather than occupying its own row.

## Quality of outputs

Assessed against issue #66's stated acceptance criteria:

| #66 requirement | Met | Notes |
|---|---|---|
| Footer gone; bottom bar holds Roll button alone | ✅ | `MainActivity.kt` −23/+35 |
| Credit still reachable, not competing with main flow | ✅ | ⓘ button → `AboutSheet` |
| Keeps author + license, stays discoverable | ✅ | Pinned outside the swatch row's h-scroll so it cannot scroll away |
| Design pass on placement (no top app bar to hang on) | ✅ | Placement reasoned in the commit body, not just asserted |
| Check whether freed space should go to result/stepper | ✅ | ~24dp absorbed by the weighted result band, no code change |

**Strengths:**

- **The commit message explains *why*, and the reasoning is falsifiable.** The
  placement argument — the swatch row's height is already set by its 44dp touch
  targets, so the button is free vertically, and "a placement costing 22dp to
  save 24dp would have been theatre" — is the kind of justification that
  survives review.
- **Scope discipline.** `isAboutVisible` + `showAbout`/`dismissAbout` are
  visibility-only; a ViewModel test explicitly asserts a roll survives opening
  the sheet. The feature does not reach into pool, result, or log state.
- **A latent gap was closed, not just the ticket.** The app had no license of
  its own; `LICENSE.md` (Apache 2.0) was added, and the sheet shows both
  licenses so the artwork's CC BY is not mistaken for the app's.
- **`buildConfig = true`** so the displayed version cannot drift from
  `versionName` — a small correctness choice with no prompting.
- **Test coverage moved with the change.** 3 new ViewModel tests, 4 new screen
  tests, and the four existing fit-tests were **repointed** to assert the About
  button where they previously asserted the pinned credit — rather than being
  deleted as newly-failing.
- **Documentation kept in sync:** 6 existing docs updated alongside 2 new ones,
  including `docs/licenses/third-party-assets.md`, the file that made the credit
  mandatory in the first place.

**Weaknesses:**

- **The instrumented tests were never run.** No device or emulator was available
  (`adb` not installed). This is disclosed honestly in both the PR body and
  `docs/testing/about-sheet.md` — but it means the 4 new screen tests, the 4
  repointed fit tests, and the 2 `FantasyDiceArtUiTest` changes are **unverified
  code**. The change was merged on `./gradlew test`, `lint`, and
  `assembleDebugAndroidTest` (compilation only) alone. The single highest-risk
  fact about this PR.
- **`AboutSheet.kt` is 323 lines** for a three-line sheet. Not reviewed here for
  factoring, but it is the natural place for cruft to settle.
- **The design iteration cost two extra round-trips.** Both user corrections
  (link placement, artwork on one line) point the same direction — economy of
  space — which is the issue's own stated motivation. A first pass that had read
  #66's intent more aggressively would likely have landed there unprompted.

## Steps skipped, or done by the wrong actor

Since no agent ran, "wrong agent" does not apply. What was displaced:

- **Design review → the user.** Performed manually across two correction rounds.
  Effective, but it spends human attention on exactly what the pipeline's design
  step exists to absorb.
- **Instrumented verification → nobody.** Not deferred to an agent, not run;
  simply unavailable in the environment. It is *documented*, which is the right
  handling of a gap that cannot be closed, but the gap is open.
- **Pre-merge code review → not evident.** No `/code-review` invocation and no
  review agent in a 521-line transcript. 323 new lines of Compose merged
  unreviewed by anything other than the author.

## Improvement suggestions

1. **Fix the pipeline log or retire it.** Six `STOPPED: unknown` lines is a log
   that costs writes and yields nothing. It records neither which agents ran nor
   that none did — an audit has to reconstruct that from the transcript. Either
   emit real start/handoff/output events, or drop the file and treat the session
   transcript as the record of truth.
2. **Decide the pipeline's status explicitly.** Three consecutive PRs have
   bypassed it, one with a documented complaint about permission friction and
   flow defects. It is now the exception, not the default. Either fix the
   friction #65 named, or shrink the chain to the parts that earn their cost —
   but stop treating a bypassed pipeline as an anomaly worth auditing each time.
3. **Make "instrumented tests unrun" a merge-blocking checkbox.** This is the
   second PR-relevant appearance of an unverifiable-UI-test gap. A PR template
   line — *instrumented tests: run / not run + why* — makes the risk a decision
   rather than a paragraph in the body. Longer term, an emulator in CI removes
   the question.
4. **Feed the design constraint in up front.** Both user corrections were
   space-economy calls, on a ticket whose whole premise is reclaiming vertical
   space. Stating "optimise aggressively for space; justify every line that
   survives" at kickoff would likely have collapsed two review rounds into zero.
5. **Run a code review before merge when the diff adds a 300+ line component.**
   `/code-review` exists in this repo's skill set and was not used. It is the
   cheapest available substitute for the review step the pipeline is not
   currently providing.

---

*Audit generated post-merge by the-spy. Agent-activity claims are drawn from
`/tmp/dice-roller-pipeline.log` and the session transcript; where those record
nothing, this report says so rather than inferring.*
