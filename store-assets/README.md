# Store assets

Everything the Play Console listing needs, and the tooling that regenerates it. The
layout and tools follow Todolist's `store-assets/`.

```
listing/
  ic_launcher_play_store.png 512×512 high-res icon
  feature-graphic.png        1024×500
  make-listing-graphics.py   draws both from res/drawable/ic_launcher_*.xml
  title-*.txt                en-US, fr-FR (30 characters)
  full-description-*.txt     en-US, fr-FR (4000 characters is the Play ceiling)
  short-description-*.txt    en-US, fr-FR (80 characters)
screenshots/
  en-US/, fr-FR/             one set per listing language, named by Play's code
    phone-0*.png             1080×2160
    tablet-7/                1200×1920
    tablet-10/               1600×2560
tools/
  make-demo-prefs.py         writes the DataStore file the screenshots start from
  reshoot-store.py           drives the app in both languages at every size
  publish-play.py            uploads bundle, listing, screenshots and testers to Play
```

## The graphics

`make-listing-graphics.py` translates the launcher's vector layers to SVG and renders
them with `rsvg-convert`, so the store icon cannot drift from the app's. The feature
graphic sets the name in Cinzel, from `res/font/`. Re-run it when the icon changes.
They reach Play only with `publish-play.py --graphics`; `--listing` sends text alone.

## The screenshots

```bash
./gradlew installDebug
python3 store-assets/tools/reshoot-store.py emulator-5554
```

One phone AVD, resized for each form factor (tablet AVDs draw a stray handle bar). Each
session force-stops the app and replaces `files/datastore/dice_settings.preferences_pb`
through `run-as`, so language, theme, colour, custom dice (D7, D100) and four history
entries are fixed, and sound and shake are off. The roll itself is real, so the values
change every run. **Look at every image before publishing.**

The phone size is 1080×2160, Play's 2:1 limit, rather than 1920 tall: at 1920 the roll's
total falls below the fold (the ≤680dp case in `docs/features/custom-dice.md`). Pools are
kept to two die types for the same reason.

Demo mode (9:30, full battery, wi-fi) is re-entered after every resize, because the
density change restarts part of SystemUI and left duplicated status-bar glyphs.

## Positioning and tags

The title carries the RPG and tabletop search terms (*JDR*, *plateau* in French);
the descriptions stay plain. Third-party trademarks (D&D, Warhammer) are kept out on
purpose: Play's metadata policy and a single trademark complaint can pull the listing.

Tags are not in the publishing API; they are set in the Console under *Grow → Store
presence → Store settings → Tags* (up to 5, from Google's list). Pick the ones closest
to: role-playing, tabletop, board, dice, tools.

## Publishing to Google Play

`tools/publish-play.py` uses the same service-account key as Todolist
(`~/.config/todolist/play-service-account.json`, or `$PLAY_SERVICE_ACCOUNT`); the
account needs this app added under *Users and permissions*. Everything goes into one
edit; a failure deletes it. `--dry-run` prints the calls, `--validate-only` has Play
check the edit and discard it.

```bash
python3 store-assets/tools/publish-play.py --listing --screenshots   # add --graphics for icon/feature graphic
python3 store-assets/tools/publish-play.py --bundle app/build/outputs/bundle/release/app-release.aab \
    --track alpha --release-name 1.0.0 --status draft --notes-dir notes/ \
    --testers testers-community@googlegroups.com
```

The closed test is the `alpha` track, with the same Google Group as Todolist's.
The first release (1.0.0, 2026-10-04) went up with `--status draft`: Play accepts only
drafts until an app has been published once, and the rollout is started in the Console.
Later releases use the default `completed`. A language with no listing yet is created
under the existing title.

**What the API cannot do**, and has to happen in the Console: create the app, fill in
*App content* (privacy policy, ads, data safety, content rating, target audience), and
set a testing track's countries (`countryAvailability` is read-only).

The privacy policy is `privacy-policy.md` at the repository root:
https://github.com/emmanuel-h/DiceRoller/blob/main/privacy-policy.md

## Signing

The release build reads `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and
`KEY_PASSWORD` from the gitignored `local.properties`; without them it is unsigned.
This is the upload key only. Play App Signing holds the app key.
