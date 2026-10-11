# Shadow Injector — SketchWare Pro project (`.swb`)

A ready-to-import SketchWare Pro project: anime-styled dark UI, animated background,
login screen, injector panel, floating overlay menu with Shizuku status.

**Deliverable:** `dist/Shadow Injector v1.1 (com.shadow.injector, 2).swb`  
Simple-named copy for downloading: `dist/ShadowInjector_v1.1.swb`

Zero external libraries, zero local libs — it imports and builds with a stock
SketchWare Pro install.

---

## 1. Install on your phone

1. Copy `dist/ShadowInjector_v1.1.swb` to your phone.
2. SketchWare Pro → **My Projects** → ⋮ (top right) → **Restore from backup** /
   **Import project** → pick the `.swb`.
3. Open **Shadow Injector** → press the **run** button to build the APK.

First run on Android 6+: grant **"Display over other apps"** when the INJECT
button asks for it (the app opens the right settings page for you).

---

## 2. What the app does

### First launch — update popup
Shown once on the very first app open (`UpdateDialog`):

* **JOIN CHANNEL** → opens the Telegram invite
* **CLOSE** → dismiss and continue into the app
* **EXIT** → closes the app
* **Do not show again** checkbox → remembered in SharedPreferences

Telegram link: `https://t.me/+BBimnHMiSvpiYTBl`
(change `TELEGRAM_URL` at the top of `src/data/files/java/UpdateDialog.java`)

### Screen 1 — `MainActivity` (login)
Anime logo, `USERNAME` / `PASSWORD` fields (password is masked), `LOGIN` button.
Wrong input shows a red message + toast.

Also has a **"Join Telegram for updates"** pill at the bottom.

**Default credentials: `admin` / `1234`**
Change them in `build/build.py` (`LOGIN_USER`, `LOGIN_PASS`) and rebuild — or
just edit the two `equals` blocks in the `btn_login` onClick event inside
SketchWare.

### Screen 2 — `InjectorActivity` (main panel)
* Header with the logged-in username and a `PRO` badge
* **SHIZUKU STATUS** card + `CHECK SHIZUKU` button →
  toasts **"Shizuku is running"** or **"Shizuku isn't running"** and colours the
  status text green / red
* **INJECTION PANEL** with the big `I N J E C T` button → starts the floating
  overlay (asks for the overlay permission first if it's missing) and toasts the
  Shizuku status
* `STOP OVERLAY` → removes the floating icon
* `JOIN TELEGRAM FOR UPDATES` → opens the channel

### Floating overlay (`FloatingService`)
* Draggable floating icon, tap it (a tap, not a drag) to expand the menu
* Menu contains **HIDE** (`—`), **CLOSE** (`✕`), a **Shizuku status row**
  (tap to re-check + toast) and **BUTTON 1 … BUTTON 4**
* `HIDE` collapses back to the icon, `CLOSE` kills the overlay
* Buttons 1–4 currently toast `"BUTTON n activated"` — wire your own payload there

### Shizuku check (`ShizukuCheck.java`)
No Shizuku API library is needed. It checks, in order:
1. scan of `/proc/<pid>/cmdline` for `moe.shizuku.privileged.api`
2. `pidof moe.shizuku.privileged.api`
3. `ps -A`, then `ps`

Runs off the main thread and reports back on the UI thread.

---

## 3. Theme / assets

| Colour | Hex | Used for |
|---|---|---|
| background | `#07050E` | screens |
| surface | `#0F0B1C` – `#14FFFFFF` | cards, inputs |
| primary | `#B14CFF` | violet accents |
| accent | `#22D3EE` | cyan accents |
| text | `#EDE9FE` | main text |
| dim | `#8B80B8` | secondary text |
| danger | `#FF4D6D` | errors / stop |

Drawables live in `src/data/files/resource/drawable/`:
`card_bg`, `input_bg`, `btn_primary`, `btn_ghost`, `btn_danger`, `badge_bg`,
`floating_bg`, `floating_header_bg`, `floating_item_bg`, `floating_icon_bg`,
`ic_stat_shadow`.

### Swapping the images
| File | What it is | Suggested size |
|---|---|---|
| `src/resources/icons/icon.png` | launcher icon | 512×512 |
| `src/resources/images/logo.png` | logo on login + header | 384×384 |
| `src/resources/images/floating_icon.png` | the floating overlay icon | 192×192 |

Replace them and run `python3 build/build.py`, or import the project and use
SketchWare's **Image manager** (image names `logo` and `floating_icon` must stay
the same).

---

## 4. Repo layout

```
dist/… .swb                     <- the deliverable
build/build.py                  <- generates the .swb from src/
build/verify_logic.py           <- simulates Sketchware's block→Java generator
build/verify_view.py            <- checks the layout tree / previews the XML
src/
  data/files/java/
      AnimeBackground.java      <- Canvas particle + sakura background animation
      FloatingService.java      <- overlay Service (drag, menu, Shizuku row)
      ShizukuCheck.java         <- dependency-free Shizuku detection
      UpdateDialog.java         <- first-launch Telegram popup
      AndroidHelper.java        <- View -> Activity/Context resolution
  data/files/resource/layout/floating.xml
  data/files/resource/drawable/*.xml
  data/Injection/androidmanifest/app_components.txt   <- declares the Service
  resources/{icons,images}/…
```

Rebuild:

```bash
pip install pycryptodome
python3 build/build.py
python3 build/verify_logic.py /tmp/logic     # optional sanity check
```

---

## 4b. Gotcha: never use `this` inside a click listener

Blocks run inside an anonymous `View.OnClickListener`, so `this` is the
**listener**, not the Activity — that produces:

    The method start(Context) is not applicable for the arguments
    (new View.OnClickListener(){})

Always write `MainActivity.this` / `InjectorActivity.this` in
**add source directly** blocks that are inside an onClick event.
`onCreate` blocks are fine with `this`, but `<Activity>.this` is used there too
for consistency.

## 5. How the `.swb` is put together

A `.swb` is a plain ZIP. Six members are AES-encrypted, the rest are plain text.

| Member | State |
|---|---|
| `project` | encrypted |
| `data/file`, `data/view`, `data/logic`, `data/resource`, `data/library` | encrypted |
| `data/project_config`, `data/permission`, `data/proguard`, `data/stringfog`, `data/custom_blocks`, `data/view_root`, `data/Injection/**` | plain |
| `data/files/java/*.java` | plain — **compiled into the APK** |
| `data/files/resource/{layout,drawable}/*` | plain — merged into `res/` |
| `resources/{icons,images,fonts,sounds}/` | plain |

**Encryption:** `AES/CBC/PKCS5Padding`, key = IV = `sketchwaresecure`
(`mod.hey.studios.project.backup.BackupFactory`).

On import SketchWare decrypts `project`, assigns a new `sc_id`, then copies
`data/` → `.sketchware/data/<sc_id>/`, `resources/*` → `.sketchware/resources/*/<sc_id>/`
and `project` → `.sketchware/mysc/list/<sc_id>/project`.

---

## 6. Notes

* v1.1 — compile fixes, new neon button styles, Telegram link + first-launch popup
* `minSdk 21`, `targetSdk 33` (33 keeps the foreground-service overlay simple).
* Permissions: `INTERNET`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`,
  `POST_NOTIFICATIONS`, `WAKE_LOCK`.
* The overlay runs as a foreground service with a small ongoing notification
  (required from Android 8 onwards).
* AppCompat is enabled; both activities are `NoActionBar` (dark status bar is set
  at runtime).
