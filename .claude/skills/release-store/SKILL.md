---
name: release-store
description: Automate the full DiceRoller release workflow — ask for the version, optionally regenerate the store screenshots, build the signed AAB, create the GitHub release, and publish the bundle, release notes and (only if asked) listing text and images to Google Play. Use when the user wants to publish a new version to the Play Store, including when they ask for a release bundle.
argument-hint: "[major|minor|hotfix]  (default: minor)"
allowed-tools: Bash Read Edit Glob Grep Write AskUserQuestion
---

Automate the full DiceRoller release workflow, from version bump to Google Play.
**Never pick the version yourself**: Step 5 always asks.

## Input

`$ARGUMENTS` contains the release type: `major`, `minor`, or `hotfix`.
Default to `minor` if the argument is absent or unrecognised. It only sets the
recommended option in Step 5; the user decides.

---

## Step 1 — Parse the release type

```bash
RELEASE_TYPE="${ARGUMENTS:-minor}"
if [[ "$RELEASE_TYPE" != "major" && "$RELEASE_TYPE" != "minor" && "$RELEASE_TYPE" != "hotfix" ]]; then
  RELEASE_TYPE="minor"
fi
echo "Release type: $RELEASE_TYPE"
```

---

## Step 2 — Read the current version from `app/build.gradle.kts`

```bash
CURRENT_CODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
CURRENT_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts)
echo "Current: versionCode=$CURRENT_CODE  versionName=$CURRENT_NAME"
```

---

## Step 3 — Calculate the candidate versions

Compute all three bumps so Step 5 can offer each with its real numbers:

```bash
IFS='.' read -r VER_MAJOR VER_MINOR VER_PATCH <<< "$CURRENT_NAME"
VER_PATCH="${VER_PATCH:-0}"
NEW_CODE=$((CURRENT_CODE + 1))
MAJOR_NAME="$((VER_MAJOR + 1)).0.0"
MINOR_NAME="${VER_MAJOR}.$((VER_MINOR + 1)).0"
HOTFIX_NAME="${VER_MAJOR}.${VER_MINOR}.$((VER_PATCH + 1))"
echo "major=$MAJOR_NAME minor=$MINOR_NAME hotfix=$HOTFIX_NAME code=$NEW_CODE"
```

---

## Step 4 — Preflight: signing and the Play service account key

```bash
PLAY_KEY="${PLAY_SERVICE_ACCOUNT:-$HOME/.config/todolist/play-service-account.json}"
test -f "$PLAY_KEY" && echo "Play key: $PLAY_KEY" || echo "MISSING Play key: $PLAY_KEY"
grep -c '^RELEASE_KEY' ~/.gradle/gradle.properties   # expect 4 (names only; never print values)
```

If either is missing, **stop before changing anything** and point the user at
"Publishing to Google Play" and "Signing" in `store-assets/README.md`. Never look for
the key anywhere else, and never print its contents or the signing passwords.

---

## Step 5 — Ask the user

Ask all four in **one** `AskUserQuestion` call. Every option carries a `preview` that
shows what that choice produces (the user's global instructions require it on every
option).

1. **Version** — "Release $CURRENT_NAME (code $CURRENT_CODE) as …?" Options: the
   `$RELEASE_TYPE` bump (Recommended), the two other bumps with their resulting
   numbers, and "Don't release". Preview: the `build.gradle.kts` diff and the tag.
2. **Track** — "Which Play track does it go to?" Options: `alpha` (the closed test,
   Recommended while the app is in closed testing), `production` (store users, after
   Google's review) and `internal`. Preview: who receives the build.
3. **Listing text** — "Update the store title and descriptions?" Options: "No — keep
   the Console's text" (Recommended) and "Yes — upload `store-assets/listing/`".
   Preview for yes: the titles and short descriptions that will replace the Console's.
4. **Images** — "Update store images?" Options: "None" (Recommended), "Screenshots —
   regenerate and upload", "Icon and feature graphic", "Both". Preview: the files
   that would be uploaded (30 screenshots: `{en-US,fr-FR}/` × phone, tablet-7,
   tablet-10 × 5; or `ic_launcher_play_store.png` + `feature-graphic.png` in both
   languages) and the Play slots they replace; for None, "listing images unchanged".

If the user picks "Don't release", stop. Keep the answers as `NEW_NAME`, `TRACK`,
`LISTING` (`yes`/`no`), `SCREENSHOTS` (`yes`/`no`) and `GRAPHICS` (`yes`/`no`).
**Upload nothing the user did not choose here.**

---

## Step 6 — Patch `app/build.gradle.kts`

```bash
sed -i "s/versionCode\s*=\s*${CURRENT_CODE}/versionCode = ${NEW_CODE}/" app/build.gradle.kts
sed -i "s/versionName\s*=\s*\"${CURRENT_NAME}\"/versionName = \"${NEW_NAME}\"/" app/build.gradle.kts
grep -E 'versionCode|versionName' app/build.gradle.kts
```

---

## Step 7 — Regenerate the screenshots (only if the user chose them)

Follow "The screenshots" in `store-assets/README.md`: one phone emulator, resized for
each form factor.

```bash
export ANDROID_HOME=~/Android/Sdk
DEV=$($ANDROID_HOME/platform-tools/adb devices | awk '/^emulator-/{print $1; exit}')
ANDROID_SERIAL=$DEV ./gradlew installDebug
python3 store-assets/tools/reshoot-store.py "$DEV"
find store-assets/screenshots -name '*.png' | wc -l     # expect 30
```

If no emulator is running, boot the phone AVD with `run_in_background` and wait for
`sys.boot_completed`.

**Look at every screenshot before going further.** The roll is real and the tool
cannot judge a capture. Check each one for: the intended screen fully laid out (no
mid-animation tumble), no system or crash dialog, the right language per folder, dark
only on `04-dark`, and the roll's total on screen. If any shot fails, fix the cause
and reshoot. Never publish a set you have not seen. Then:

```bash
git add store-assets/screenshots
git commit -m "chore(store): regenerate screenshots for ${NEW_NAME}"
```

---

## Step 8 — Build the signed App Bundle

```bash
./gradlew bundleRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab | head -1   # "jar verified."
```

The release build is not minified, so there is no mapping file. If the build fails or
the bundle is unsigned, show the error and stop.

---

## Step 9 — Commit and push the version bump

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to ${NEW_NAME} (code ${NEW_CODE})"
git push
```

No AI attribution in the commit message.

---

## Step 10 — Create the GitHub release

The GitHub release archives every bundle shipped.

```bash
TAG="v${NEW_NAME}"
gh release create "$TAG" --title "$TAG" --generate-notes
gh release upload "$TAG" app/build/outputs/bundle/release/app-release.aab --clobber
```

---

## Step 11 — Write the Play release notes

List what changed since the previous release (conventional commits; keep `feat`/`fix`):

```bash
PREV_TAG=$(gh release list --limit 2 --json tagName --jq '.[1].tagName')
git log "${PREV_TAG}..HEAD" --oneline --no-merges | grep -E '^[a-f0-9]+ (feat|fix)'
```

If there is no previous tag, ask the user where the last release was cut.

Rewrite them as user-facing notes: what the user can now do or sees differently, in
plain imperative language, with no issue/PR numbers, hashes or class names. Merge
commits that describe the same change; omit invisible ones. If nothing is visible:
`- Améliorations internes et corrections mineures.` / `- Internal improvements and minor fixes.`

**Play rejects notes over 500 characters per language.** Write French first (it runs
longer) and aim for ≤ 460 characters each.

```bash
NOTES="<your scratchpad directory>/notes"
mkdir -p "$NOTES"
cat > "$NOTES/fr-FR.txt" <<'EOF'
- …
EOF
cat > "$NOTES/en-US.txt" <<'EOF'
- …
EOF
wc -m "$NOTES/fr-FR.txt" "$NOTES/en-US.txt"
```

---

## Step 12 — Publish to Google Play

One edit, one commit. Validate first, then publish only what Step 5 chose:

```bash
FLAGS="--bundle app/build/outputs/bundle/release/app-release.aab --track $TRACK \
  --release-name $NEW_NAME --notes-dir $NOTES \
  $([ "$LISTING" = yes ] && echo --listing) \
  $([ "$SCREENSHOTS" = yes ] && echo --screenshots) \
  $([ "$GRAPHICS" = yes ] && echo --graphics)"
python3 store-assets/tools/publish-play.py $FLAGS --validate-only
python3 store-assets/tools/publish-play.py $FLAGS
```

On failure the script deletes the edit, so nothing half-done reaches the listing:
- **"Changes cannot be sent for review automatically"**: re-run with
  `--changes-not-sent-for-review` and tell the user to press *Send for review* in the
  Console.
- **"version code … already been used"**: do not bump again on your own; report it.
- **Only drafts accepted** (an app never published): re-run with `--status draft` and
  tell the user to start the rollout in the Console.
- **401 / 403**: the service account lacks permissions on this app; see
  `store-assets/README.md`.

Any other failure: show the error and stop. The GitHub release and the pushed version
already exist, so only this step needs re-running once the cause is fixed.

---

## Summary output

```
✓ Version bumped  : $CURRENT_NAME (code $CURRENT_CODE) → $NEW_NAME (code $NEW_CODE)
✓ AAB built       : app/build/outputs/bundle/release/app-release.aab
✓ GitHub release  : https://github.com/emmanuel-h/DiceRoller/releases/tag/$TAG
✓ Google Play     : $NEW_NAME on the $TRACK track
✓ Store listing   : text updated | unchanged;  images: screenshots / icon+graphic | unchanged
✓ Release notes   : fr-FR <n> chars, en-US <n> chars
```

Then show both release-note blocks, and say what was cut to fit if anything was.
