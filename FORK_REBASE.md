# Contours rebase onto upstream main

The `contours-fork-sync` branch starts at upstream `bcc34529` and replays
Contours' original commits from `cbf2a2d..c15f2002` in order. `contours` and
`origin/contours` retain the original commits and hashes. The rebased hashes
below differ because each commit now has a different parent.

## Which changes still matter

| Area | Review after replay |
| --- | --- |
| Legacy offline observers, tile count, timeout | Still fork-specific; upstream's recent offline status changes concern the separate non-legacy manager. |
| Location event throttle, sensor-only compass, native location pause/resume | Still fork-specific. Upstream added heading events, but its separate SDK provider would duplicate a location subscription; the fork's sensor-only path is retained. |
| Camera follow controls and bounds behavior | Upstream now handles partial padding and has other camera fixes. The fork's imperative follow transitions and bounds calculation remain, combined with upstream's camera hooks. Visually verify bounds and transitions on a device before release. |
| ShapeSource JSON cache, nullable Android layer positions, SDK version export | Still fork-specific. |
| Android style-value test (`3c7bc922`) | Upstream already has the equivalent fix. Its rebased commit is empty and can be omitted in a future cleanup if exact commit-message history is not needed. |
| Lockfile-only commit (`ce5de030`) | No longer applies to the current upstream dependency graph. It was dropped during replay; the final integration commit refreshes the lockfile. |

## Original-to-rebased commit map

| Original | Rebased | Subject |
| --- | --- | --- |
| `33d33bf7` | `d4d1751e` | feat(offlineLegacyManager) add observers |
| `991677d2` | `7581bb7f` | Add requiredTileCount |
| `1f6017f8` | `8b41334b` | Increase default timeout for offline legacy downloads |
| `640a6db1` | `72172d8e` | fix: type errors |
| `f7e519d6` | `08a2a172` | fix: reduce location update and prevent heading updates if disabled |
| `2a950be9` | `39578c3a` | fix(location) event throttling on android |
| `ff98c888` | `ba22b37f` | fix: type error |
| `e85ced6f` | `eb1c5c34` | fix(perf): prevent shape source from creating a new json string on every props update |
| `c96283d3` | `e8ccb500` | fix: camera padding issues |
| `def68a6e` | `5de5842d` | publish config |
| `6bbffe3e` | `df5337be` | feat: ability to completly pause location updates to the Mapbox view |
| `1a49140e` | `42c25631` | add CONTOURS.md |
| `a375de08` | `456a9dce` | Update package.json |
| `40bfdfae` | `43977940` | fix(location): pause/resume to run on main thread and drop pause count |
| `5714c738` | `a9b8f8bf` | publish |
| `ce5de030` | Superseded by lockfile refresh | yarn.lock |
| `55430bc2` | `7cd39417` | fix(line-layer): handle null values on android |
| `8d3f61f6` | `63e424b2` | publish: 10.3.5-contours.1 |
| `d4a77839` | `9d43a521` | feat: add imperative camera followUser* properties |
| `a0fd0ec6` | `c713d074` | fix(follow): allow transition to complete on follow cancel |
| `a71b6765` | `5bad748b` | fix(follow): supersede active camera animations |
| `ae4fc0c4` | `7ba4ff92` | ignore declartive props for followUser and add ability to set max animation duration |
| `6f2b8137` | `42b667f4` | fix(example): upgrade worklets to match renanimated use |
| `f7cc2a8b` | `7fda54a5` | fix(example): alias @gnative/rnmapbox for example |
| `3f7b8b2f` | `fe8759c9` | fix(example): use impreative updates to follow mode |
| `3c7bc922` | `3cdfa391` (empty; upstream has fix) | test(android): fix string literals in style value tests |
| `271408a5` | `9dffe607` | fix(android): preserve SDK compass for native location tracking |
| `d5361719` | `15258cad` | fix(android): expose compass heading in JS location updates |
| `bf122837` | `79ee12c5` | chore(example): update iOS dependency lockfile |
| `28863d1c` | `a3658273` | fix(android): native location puck and heading indicator style |
| `f829316d` | `c6556b22` | fix(android): avoid duplicate heading location updates |
| `cb8e4e7b` | `150244fa` | fix(location): separate Android heading indicator visibility |
| `e59f68d6` | `2f072afb` | fix(location): simplify native bearing configuration |
| `08d3f60e` | `ae975255` | fix(android): build list devices |
| `47184467` | `7817bfb4` | publish |
| `875195f0` | `c6212fc0` | fix(android): remove obsolete v11 compatibility imports |
| `e5d088dc` | `fb8f1e29` | feat: expose installed Mapbox SDK version |
| `c15f2002` | `080e2d30` | publish |

To see only Contours commits after upstream, run
`git log --oneline upstream/main..contours-fork-sync`. To inspect how the
patches changed during replay, run
`git range-diff cbf2a2d..c15f2002 upstream/main..contours-fork-sync`.
The final integration commit records dependency, generated-code, test and
example corrections separately from the replayed commits.
