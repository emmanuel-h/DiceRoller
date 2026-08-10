# Audit: PR #65 — Fit the whole dice screen in one view

**PR:** [#65](https://github.com/emmanuel-h/DiceRoller/pull/65) · squash-merged as `71e3947`, 10 August 2026
**Closes:** #62, #63, #64 (all completed)

## Headline finding

**No pipeline agent ran for this PR — by explicit user instruction.**

Unlike [PR #43](pr-43-audit.md), where the pipeline was bypassed silently, here the
user opted out deliberately and gave a reason: *"without okay-boss. Use a more
lightweight process. Take the best of it, but don't use all those subagents that
constantly need permissions, and the flow is not perfect and has defect."*

This is a **product decision about the pipeline**, not a workflow slip, and it is the
second consecutive substantive PR to skip the chain. Two data points in a row is a
signal about the pipeline's cost, not about this change.

### Evidence

| Signal | Observation |
|---|---|
| `/tmp/dice-roller-pipeline.log` | 40 bytes, one line: `[2026-08-09T18:35:57Z] STOPPED: unknown`. No subagent lifecycle events. |
| Session transcript | Design proposal, implementation, test repair, device verification and merge all performed directly in the main session. |
| `git log` | One squash commit, no per-agent commits. |

## What ran instead

The parts of the pipeline that carry the value were kept; the parts that cost
permission prompts were dropped.

| Pipeline stage | Substitute | Kept? |
|---|---|---|
| the-artist (design) | Three ASCII whole-screen options presented before any code, chosen by the user, then a detailed state-by-state sketch | ✅ |
| the-sage (architecture) | Layout structure reasoned through inline (band budget in dp, `weight` vs. flow layout, scrollable `Column` replacing `LazyColumn`) | ✅ |
| the-craftsman (implementation) | Direct implementation, conventional commit | ✅ |
| the-inquisitor (PR review) | **Not obtained.** No independent review of a 721-line change. | ❌ |
| the-guardian (tests) | Tests written by the same session that wrote the code — the independence the role exists for was not obtained | ⚠️ |
| the-scribe (docs) | `CLAUDE.md` architecture section corrected in the same commit | ✅ |

## Findings

### 1. The post-merge audit hook fires on every Bash command

The hook fired **thirteen times during this session**, on `./gradlew`, `adb`, `git
stash` and `find` commands. No PR had been merged in any of those cases; only the
final `gh pr merge` was genuine.

The cause is *not* the missing-colon matcher that `pr-43-audit.md` reported — that
one is already fixed. The current config is:

```json
{ "matcher": "Bash", "hooks": [{ "type": "agent", "if": "Bash(gh pr merge:*)", ... }] }
```

`matcher` gates on the tool name only, so every Bash call reaches the hook; the
per-hook `if` is supposed to narrow it and evidently does not for `type: "agent"`.
An in-session probe could not confirm the correct syntax: two command-type hooks
added with each candidate `if` form never fired at all, matching or not, because the
settings watcher does not reload edits mid-session. **The syntax question is
therefore still open** and needs a fresh session (or `/hooks`) to settle.

**Mitigation applied instead of a syntax guess:** the `if` was left as-is —
it fails *open*, which is the safe direction, since a wrong guess could fail *closed*
and silently skip real merges. The agent prompt now self-gates: step 1 reads
`.tool_input.command` and exits immediately unless it starts with `gh pr merge`.
Misfires become silent no-ops whether or not `if` ever works.

**Why it matters beyond noise:** a hook that asserts "a PR was just merged" on every
shell command is a standing invitation to fabricate an audit of a merge that never
happened. It was declined twelve times this session; a less careful run produces
twelve fictional audit reports. The prompt now carries an explicit accuracy rule
forbidding invented agent activity.

### 2. PR #61 shipped a red instrumented suite

Seven `DiceResultDisplayTest` cases failed on `main` before this branch — confirmed
by stashing the work and running the suite against unmodified `main`.

They search the *merged* semantics tree for nodes inside a row that deliberately
merges its descendants into one accessibility node; they need `useUnmergedTree` and
could never have passed. Issue #64 suspected the suite had never been run on
hardware. It had not.

This is the concrete cost of the-guardian's tests never being executed: a suite that
looks like coverage, is cited as coverage, and asserts nothing. Repaired in this PR.

### 3. Device verification was treated as an acceptance criterion, not a formality

#64 required verification on a real device. Done on a phone emulator (411×914dp):
the acceptance pool built by tapping real chip halves, plus the 20×D20 extreme.
Both fit without scrolling. This is what caught the results block floating
mid-band, which no test asserted and no preview made obvious.

## Suggested improvements

1. **Settle the `if` syntax for agent hooks in a fresh session** (finding 1). Confirm
   whether `Bash(gh pr merge:*)` narrows a `type: "agent"` hook at all; if it does
   not, the hook needs a command-type gate or the self-gating prompt is the only
   defence. The accuracy rule — state plainly when no agents ran, never infer
   behaviour from an empty log — is now in the prompt; both audits so far have had to
   lead with "no agent ran".
2. **Make the-guardian run its tests.** A test suite that has never executed is worse
   than no suite. Require `connectedAndroidTest` output in the PR before review.
3. **Decide the pipeline's fate.** Two consecutive PRs skipped it, the second
   explicitly for friction. Either reduce it to the stages that pay for themselves —
   independent *review* and independent *test execution* are the two that would have
   caught finding 2 — or retire it. Keeping a nine-agent chain that is routinely
   bypassed means the audits keep reporting its absence.
