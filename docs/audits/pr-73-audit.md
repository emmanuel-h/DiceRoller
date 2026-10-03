# Audit: PR #73 — Empty the pool from the roll bar and after each roll

**PR:** [#73](https://github.com/emmanuel-h/DiceRoller/pull/73) · squash-merged as `f3d9d50`, 15 August 2026
**Closes:** #67 (closed on merge) · **Size:** 10 files, +742 / −26

## Headline finding

**No pipeline agent ran for this PR. The work was done directly in a single interactive session.**

This is the **fourth consecutive substantive PR** to skip the agent chain, after
[#43](pr-43-audit.md) (bypassed silently), [#65](pr-65-audit.md) (bypassed by explicit
user instruction) and [#71](pr-71-audit.md) (bypassed with no stated opt-out).

The #71 audit concluded that three in a row was "a statement about the pipeline: it is
not the default path any more." A fourth does not add information about the pipeline —
that question is settled. What it changes is the standing of the recommendations: every
audit so far has proposed making bypass visible, and the bypass rate since is 4 of 4.
**The recommendations themselves are now the thing that is not working**, and this audit
treats that as its primary subject rather than repeating them a fourth time.

### Evidence

Three independent sources agree, and the first is *not* sufficient on its own:

| Source | Observation |
|---|---|
| `/tmp/dice-roller-pipeline.log` | 7 lines, all `STOPPED: unknown`, spanning 14–15 August. **Zero `STARTED` lines** and zero agent names. |
| Session transcript (617 lines) | **Zero `Task` tool calls and zero `Skill` calls.** Every occurrence of `the-boss` / `the-spy` / `okay-boss` in the file is agent-listing metadata, `CLAUDE.md` prose, or this hook's own prompt — not one invocation. |
| `.claude/spy/activity.jsonl` | 138 entries for session `64757c83`; **135 carry `agent_type: null`, none names an agent.** |

**On the log's reliability.** The `STOPPED: unknown` lines must not be read as "agents ran
but were not named." The `SubagentStart`/`SubagentStop` hooks resolve
`.subagent_type // .agent_name // "unknown"`, and that expression has been yielding
`unknown` since at least 8 August — the defect the #43 audit flagged as unfixed. A log of
`unknown` entries is therefore *ambiguous by construction* and cannot by itself distinguish
"no agents" from "unlogged agents." The finding above rests on the transcript and
`activity.jsonl`, which are unambiguous. Had only the log been available, the honest
report would have been "inputs insufficient."

The session began with the bare prompt `Implement https://github.com/emmanuel-h/DiceRoller/issues/67`
— not `/okay-boss`, the only entry point that engages the orchestrator.

## What the pipeline would have covered

Stated as **gaps against each agent's remit**, inferred from the merged artifacts alone.
No claim is made about any agent's behaviour, because none ran.

| Agent | Remit covered by the direct session? |
|---|---|
| the-herald | n/a — #67 already existed and was specified. |
| the-artist | **Partial.** Placement is argued in the PR body (outlined vs filled, glyph vs sentence, 40dp vs full width, hidden vs disabled) and carries a real 360×640dp fit assertion. Reasoned, but self-reviewed. |
| the-sage | **Yes, informally.** The state invariant is documented in `docs/features/clear-pool.md` and `CLAUDE.md`. Written alongside the code, not ahead of it. |
| the-craftsman | **Yes.** Conventional commit, full PR body, docs included. |
| the-inquisitor | **No. Largest gap** — see below. |
| the-guardian | **Partial.** 13 unit + 7 instrumented tests, 133 green on an API 36.1 emulator — but authored by the implementer, not adversarially. |
| the-scribe | **Yes.** `docs/features/clear-pool.md`, `docs/testing/clear-pool.md`, `CLAUDE.md`. |

## Quality of the delivered work

Assessed from the diff, PR body and docs — independent of how it was produced, and it is good:

- **A deliberate invariant, documented as such.** "A non-null result implies an empty pool"
  is stated, and its consequence — that `clearPool`'s and `removeCustomDie`'s result-clearing
  clauses become unreachable — is acknowledged rather than hidden. Keeping them *because they
  state the rule* is a defensible call, explicitly made.
- **A dropped test is explained, not quietly deleted.** "Incrementing at the cap preserves the
  result" needed a result and a maxed count simultaneously, which the new invariant forbids;
  the rule it guarded is noted as still held by its decrement-at-floor twin. This is the
  disclosure most likely to be omitted, and it was not.
- **A known consequence is pinned by a test.** No-reroll is called out as deliberate and
  guarded against regressing into a double-record in history.
- **Costs are quantified.** The ✕ takes 52dp from the Roll button, and the longest label still
  fits on one line at 360dp.

Two points a reviewer should have pressed, neither disqualifying:

1. **Deliberately unreachable code** is a maintenance liability — a future reader cannot tell
   the clauses are dead without reconstructing the invariant. A test asserting the invariant
   directly, or a comment at each clause, would carry the intent more cheaply.
2. **Clearing the pool on every roll is a product change beyond #67's ask** ("a control to clear
   the pool"). It arrived mid-session from user feedback and is right, but it widened scope
   without the issue being updated.

## The review gap, stated precisely

**PR #73 was created at 17:56:23 and merged at 17:57:42 — 79 seconds later, with 0 reviews
and 0 status checks.** The branch was deleted on merge.

This is not a pipeline problem and would not be fixed by running the pipeline. `statusCheckRollup`
has been empty for every audited PR: **#43, #65, #71 and #73 all merged with no automated
verification on GitHub's side.** The test evidence in the PR body is real but is the author's
own local report, unreproduced by any third party. Adding CI is the one recommendation that
pays off whether or not the pipeline is ever used again — and it is the one that has been
carried unactioned across all four audits.

## Improvement suggestions

Prior audits' suggestions are not repeated; they are superseded by (1).

1. **Decide the pipeline's status explicitly, then make the config match.** Four of four PRs
   bypassed it. Either it is the intended path — in which case the gap is enforcement, and a
   `UserPromptSubmit` hook that intercepts an issue URL without `/okay-boss` is the minimum —
   or it is not, in which case `.claude/agents/` and the `CLAUDE.md` pipeline section are stale
   and should be pruned. The current middle state produces an audit per PR whose main finding is
   that the documented process was not followed, which is noise, not signal.
2. **Add CI. Fourth request.** A workflow running `./gradlew testDebugUnitTest lint` on every PR
   converts the author's local claims into independent evidence. Highest value on this list and
   independent of everything above.
3. **Fix the pipeline log, or stop relying on it.** `.subagent_type // .agent_name` has produced
   only `unknown` since 8 August, so the log cannot answer the question this audit exists to ask.
   Either correct the field names against the actual hook payload or retire the log in favour of
   `.claude/spy/activity.jsonl`, which does carry `session_id` and a usable `agent_type`. Keeping
   a log that cannot distinguish "no agents ran" from "logging is broken" actively invites a
   fabricated audit.
4. **Scope audit inputs per PR.** Neither the log nor `activity.jsonl` records a branch or PR
   number; both are global and cumulative. Recording the current branch per entry would make
   "what happened for PR #73" answerable directly rather than by session-id correlation.
5. **Require a self-review pause on any PR merged within minutes of opening.** A 79-second
   open-to-merge window leaves no point at which review could occur even in principle. For
   solo work, `/code-review` before merge is the cheapest substitute for the-inquisitor.
