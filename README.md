# Shadow Injector — SketchWare Pro project (`.swb`)

A ready-to-import SketchWare Pro project: anime-styled dark UI, animated background,
login screen, injector panel, floating overlay menu with Shizuku status.

**Deliverable:** `dist/Shadow Injector v1.2 (com.shadow.injector, 3).swb`  
Simple-named copy for downloading: `dist/ShadowInjector_v1.2.swb`

Zero external libraries, zero local libs — it imports and builds with a stock
SketchWare Pro install.

---

## 1. Install on your phone

1. Copy `dist/ShadowInjector_v1.2.swb` to your phone.
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

The dialog is **invincible**: tapping outside does nothing, the BACK key is
swallowed, and the only exits are the three buttons. Leave the checkbox
unticked and it comes back on the next launch.

Telegram link: `https://t.me/+BBimnHMiSvpiYTBl`
(change `TELEGRAM_URL` at the top of `src/data/files/java/UpdateDialog.java`)

### Screen 1 — `MainActivity` (login)
Anime logo, `USERNAME` / `PASSWORD` fields (password is masked), `LOGIN` button.
Wrong input shows a red message + toast.

Also has a **"Join Telegram for updates"** pill at the bottom.

**Default credentials: `admin` / `1234`** — but they are *not printed anywhere
on the screen*, and they are not stored in the project either. See
[Passwords](#7-passwords-are-not-stored-in-the-project).
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

Tap **INJECT** and a 62dp round icon appears. Tap it → the menu opens.

* **Draggable anywhere** — grab the floating icon, the menu title bar, any empty
  part of the panel, *or* start the drag on any button (a press that doesn't
  move still counts as a tap). The bubble is clamped so it can never be lost
  off-screen.
* **BUTTON 1..4 are ON/OFF toggles** — the whole row flips from dim slate
  (`toggle_off.xml`) to a neon green→cyan glow (`toggle_on.xml`), the label
  reads `ON`/`OFF`, and the state is saved in `shadow_prefs` so it survives a
  restart of the overlay.
* **—** HIDE collapses back to the icon, **✕** CLOSE stops the overlay.
* The status row re-checks Shizuku when tapped.

### Shizuku (`ShizukuCheck.java` + the bundled Shizuku API)

The real Shizuku API (`rikka.shizuku.*`, v12.1.0, MIT) is bundled under
`src/data/files/java/`, so the app is a first-class Shizuku client:

* `data/permission` declares **`moe.shizuku.manager.permission.API_V23`** and
  `app_components.txt` registers **`rikka.shizuku.ShizukuProvider`** — that pair
  is exactly what makes **Shadow Injector appear in Shizuku Manager →
  Authorized applications**, so you can grant it there.
* `GRANT SHIZUKU ACCESS` calls `Shizuku.requestPermission(...)`.
* Detection uses **`Shizuku.pingBinder()`**, not a `/proc` scan. The binder only
  lives while the *server* is up, which fixes the old false
  "Shizuku is running" (a `/proc` hit fired whenever the Shizuku manager process
  merely existed — including when it was open but the server was never started,
  or was just sitting cached in the background). It also waits up to 1.5 s for
  Shizuku to push the binder to the provider, so a check fired immediately after
  launch isn't a false negative.
* `CHECK SHIZUKU` still toasts exactly `Shizuku is running` /
  `Shizuku isn't running`.

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

## 7. Passwords are not stored in the project

The login check is a single call:

```java
AuthCore.verify(et_username.getText().toString(),
                et_password.getText().toString())
```

`AuthCore.java` contains **no username and no password**. It holds a
**salted, 4096-round SHA-256** digest, split into XOR-scrambled byte tables that
are re-assembled at runtime, compared in constant time. `strings` on the DEX
turns up nothing usable, and the generated block code contains no credentials
either (so they aren't sitting in `MainActivity.java`).

```
$ strings classes.dex | grep -i -E 'admin|1234'
(nothing)
```

To change the credentials:

```bash
python3 build/make_auth.py <username> <password>
python3 build/build.py
```

Be realistic about the threat model: this stops casual snooping and `strings`,
and makes patching annoying, but **any offline check can eventually be bypassed
by someone who patches the APK**. If you need real enforcement, validate the
login against a server. You can additionally turn on ProGuard in SketchWare
(*Project → Settings → ProGuard*); it's left off here because it isn't exercised
by this build.

## 8. Changelog

**v1.2**
* Shizuku: bundled the real API → the app now appears in Shizuku Manager's
  authorized list and can be granted there, plus a `GRANT SHIZUKU ACCESS` button.
* Shizuku: detection via `pingBinder()` — no more false "is running".
* Menu **and** floating icon are draggable anywhere (clamped to the screen).
* BUTTON 1..4 became ON/OFF toggles with a colour flip + saved state.
* Credentials moved into `AuthCore` (salted iterated SHA-256); no plaintext
  anywhere and nothing printed on the login screen.
* First-launch popup is invincible (no back, no tap-outside).
* Your logo is now the real app icon, in-app logo and floating icon.

**v1.1** — fixed the 6 `this`-in-listener compile errors, new neon button
styles, Telegram link + first-launch popup.
