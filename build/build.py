#!/usr/bin/env python3
"""
Builds a SketchWare Pro .swb (project backup) for "Shadow Injector".

.swb layout (reverse engineered from Sketchware Pro's BackupFactory):

    project                                  <- AES encrypted project metadata
    data/project_config                      <- plain JSON
    data/permission                          <- plain JSON array
    data/proguard, data/proguard-rules.pro   <- plain
    data/stringfog                           <- plain JSON
    data/file                                <- AES encrypted  (activities list)
    data/view                                <- AES encrypted  (layouts)
    data/logic                               <- AES encrypted  (block logic)
    data/resource                            <- AES encrypted  (images/sounds/fonts)
    data/library                             <- AES encrypted  (built in libraries)
    data/view_root                           <- plain JSON (root element of each layout)
    data/custom_blocks                       <- plain JSON
    data/java                                <- empty file
    data/Injection/androidmanifest/app_components.txt
    data/files/java/*.java                   <- compiled into the app
    data/files/resource/layout/*.xml         <- compiled as layouts
    data/files/resource/drawable/*.xml       <- compiled as drawables
    resources/images/*  resources/icons/icon.png
    local_libs/*                             <- (none, this project has zero dependencies)

Encryption: AES/CBC/PKCS5Padding, key = IV = b"sketchwaresecure".
"""

import json
import os
import shutil
import sys
import zipfile
from datetime import datetime

from Crypto.Cipher import AES

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, "src")
OUT_DIR = os.path.join(ROOT, "dist")

KEY = b"sketchwaresecure"

# --------------------------------------------------------------------------
# project identity
# --------------------------------------------------------------------------
PKG = "com.shadow.injector"
APP_NAME = "Shadow Injector"
WS_NAME = "shadow_injector"
VER_NAME = "1.0"
VER_CODE = "1"
SC_ID = "600"

# credentials checked on the login screen (edit these!)
LOGIN_USER = "admin"
LOGIN_PASS = "1234"

# --------------------------------------------------------------------------
# helpers
# --------------------------------------------------------------------------


def aes_encrypt(text: str) -> bytes:
    """AES/CBC/PKCS5Padding with key == IV == b"sketchwaresecure"."""
    cipher = AES.new(KEY, AES.MODE_CBC, KEY)
    data = text.strip().encode("utf-8")
    pad_len = 16 - (len(data) % 16)
    return cipher.encrypt(data + bytes([pad_len]) * pad_len)


def col(argb: int) -> int:
    """0xAARRGGBB -> signed java int."""
    if argb >= 0x100000000:
        raise ValueError("colour out of range")
    return argb - 0x100000000 if argb >= 0x80000000 else argb


def layout(**kw):
    base = {
        "backgroundColor": 16777215,  # 0xffffff == "no background"
        "borderColor": -16740915,
        "gravity": 0,
        "height": -2,
        "layoutGravity": 0,
        "marginBottom": 0,
        "marginLeft": 0,
        "marginRight": 0,
        "marginTop": 0,
        "orientation": -1,
        "paddingBottom": 0,
        "paddingLeft": 0,
        "paddingRight": 0,
        "paddingTop": 0,
        "weight": 0,
        "weightSum": 0,
        "width": -2,
    }
    base.update(kw)
    if "backgroundResource" in base:
        # keep the key even when a drawable is used
        pass
    return base


def text(**kw):
    base = {
        "hint": "",
        "hintColor": -10453621,
        "imeOption": 0,
        "inputType": 1,
        "line": 0,
        "singleLine": 0,
        "text": "",
        "textColor": -16777216,
        "textFont": "default_font",
        "textSize": 12,
        "textType": 0,
    }
    base.update(kw)
    return base


def image(res_name="default_image", scale="CENTER", rotate=0):
    return {"resName": res_name, "rotate": rotate, "scaleType": scale}


# view type constants (com.besome.sketch.beans.ViewBean)
T_LINEAR = 0
T_RELATIVE = 1
T_BUTTON = 3
T_TEXTVIEW = 4
T_EDITTEXT = 5
T_IMAGEVIEW = 6
T_FAB = 16


def bean(id_, type_, convert, parent, index, layout_=None, text_=None, image_=None):
    return {
        "adSize": "",
        "adUnitId": "",
        "alpha": 1.0,
        "checked": 0,
        "choiceMode": 0,
        "clickable": 1,
        "convert": convert,
        "customView": "",
        "dividerHeight": 1,
        "enabled": 1,
        "firstDayOfWeek": 1,
        "id": id_,
        "image": image_ if image_ is not None else image(),
        "indeterminate": "false",
        "index": index,
        "inject": "",
        "layout": layout_ if layout_ is not None else layout(),
        "max": 100,
        "parent": parent,
        "parentAttributes": {},
        "parentType": 0,
        "preId": id_,
        "preIndex": index,
        "preParent": parent,
        "preParentType": 0,
        "progress": 0,
        "progressStyle": "?android:progressBarStyle",
        "scaleX": 1.0,
        "scaleY": 1.0,
        "spinnerMode": 1,
        "text": text_ if text_ is not None else text(),
        "translationX": 0.0,
        "translationY": 0.0,
        "type": type_,
    }


def fab_bean():
    """The placeholder FAB entry Sketchware stores for every layout."""
    return {
        "adSize": "",
        "adUnitId": "",
        "alpha": 1.0,
        "checked": 0,
        "choiceMode": 0,
        "clickable": 1,
        "convert": "",
        "customView": "",
        "dividerHeight": 1,
        "enabled": 1,
        "firstDayOfWeek": 1,
        "id": "_fab",
        "image": {"rotate": 0, "scaleType": "CENTER"},
        "indeterminate": "false",
        "index": 0,
        "inject": "",
        "layout": layout(
            backgroundColor=16777215,
            gravity=0,
            height=-2,
            layoutGravity=85,
            marginBottom=16,
            marginLeft=16,
            marginRight=16,
            marginTop=16,
            orientation=-1,
            width=-2,
        ),
        "max": 100,
        "parentAttributes": {},
        "parentType": -1,
        "preIndex": 0,
        "preParentType": 0,
        "progress": 0,
        "progressStyle": "?android:progressBarStyle",
        "scaleX": 1.0,
        "scaleY": 1.0,
        "spinnerMode": 1,
        "text": text(),
        "translationX": 0.0,
        "translationY": 0.0,
        "type": T_FAB,
    }


# --------------------------------------------------------------------------
# block helpers
# --------------------------------------------------------------------------

BLOCK_COUNTER = [0]


def block(op_code, spec, block_type=" ", params=None, color=-13851166,
          sub1=-1, sub2=-1, next_=-1, type_name=""):
    BLOCK_COUNTER[0] += 1
    return {
        "color": color,
        "id": str(BLOCK_COUNTER[0]),
        "nextBlock": next_,
        "opCode": op_code,
        "parameters": params if params is not None else [],
        "spec": spec,
        "subStack1": sub1,
        "subStack2": sub2,
        "type": block_type,
        "typeName": type_name,
    }


def ref(b):
    return "@" + b["id"]


# block palette shortcuts (spec strings taken from real Sketchware projects)
C_YELLOW = -1988310      # control
C_GREEN = -10701022      # operators
C_BLUE = -13851166       # components / general
C_ORANGE = -1147626      # variables
C_VIEW = -11899692       # view blocks


def b_raw(code):
    """add source directly <java>   (statement)"""
    return block("addSourceDirectly", "add source directly %s.inputOnly", " ",
                 [code], C_GREEN)


def b_raw_b(code):
    """add source directly <java>   (boolean expression)"""
    return block("addSourceDirectly", "add source directly %s.inputOnly", "b",
                 [code], C_GREEN)


def b_raw_s(code):
    """add source directly <java>   (string expression)"""
    return block("addSourceDirectly", "add source directly %s.inputOnly", "s",
                 [code], C_GREEN)


def b_toast(msg_or_ref):
    return block("doToast", "Toast %s", " ", [msg_or_ref], C_BLUE)


def b_if(cond_ref, then_id):
    return block("if", "if %b then", "c", [cond_ref], C_YELLOW, sub1=then_id)


def b_if_else(cond_ref, then_id, else_id):
    return block("ifElse", "if %b then", "e", [cond_ref], C_YELLOW,
                 sub1=then_id, sub2=else_id)


def b_equals(a, b):
    return block("stringEquals", "%s equals %s", "b", [a, b], C_GREEN)


def b_get_text(view_id, kind="edittext"):
    return block("getText", "%m." + kind + " getText", "s", [view_id], C_VIEW)


def b_set_text(view_id, value):
    return block("setText", "%m.textview setText %s", " ", [view_id, value], C_VIEW)


def b_set_screen(activity):
    return block("intentSetScreen", "%m.intent setScreen %m.activity", " ",
                 ["intent", activity], C_BLUE)


def b_put_extra(key, value):
    return block("intentPutExtra", "%m.intent putExtra key %s value %s", " ",
                 ["intent", key, value], C_BLUE)


def b_start_activity():
    return block("startActivity", "StartActivity %m.intent", " ", ["intent"], C_BLUE)


def b_finish():
    return block("finishActivity", "finishActivity", " ", [], C_BLUE)


# --------------------------------------------------------------------------
# layouts
# --------------------------------------------------------------------------

C_TEXT = col(0xFFEDE9FE)
C_DIM = col(0xFF8B80B8)
C_VIOLET = col(0xFFB14CFF)
C_CYAN = col(0xFF22D3EE)
C_DANGER = col(0xFFFF4D6D)
C_DARK = col(0xFF07050E)
C_FAINT = col(0xFF4C4370)
C_MUTED = col(0xFF5B5080)
C_HINT = col(0xFF6B5F8F)


def main_views():
    v = []
    # container that hosts the animated background + the real content
    v.append(bean("rel_root", T_RELATIVE, "RelativeLayout", "root", 0,
                  layout(width=-1, height=-1, orientation=-1)))
    v.append(bean("linear_content", T_LINEAR, "LinearLayout", "rel_root", 0,
                  layout(width=-1, height=-1, orientation=1, gravity=17,
                         paddingLeft=24, paddingRight=24,
                         paddingTop=24, paddingBottom=24)))

    # --- logo + title -----------------------------------------------------
    v.append(bean("img_logo", T_IMAGEVIEW, "ImageView", "linear_content", 0,
                  layout(width=96, height=96, layoutGravity=1, marginBottom=14),
                  image_=image("logo", "FIT_CENTER")))
    v.append(bean("tv_title", T_TEXTVIEW, "TextView", "linear_content", 1,
                  layout(width=-2, height=-2, layoutGravity=1),
                  text(text="SHADOW INJECTOR", textSize=24, textType=1,
                       textColor=C_TEXT)))
    v.append(bean("tv_sub", T_TEXTVIEW, "TextView", "linear_content", 2,
                  layout(width=-2, height=-2, layoutGravity=1, marginTop=6),
                  text(text="Enter your credentials to continue", textSize=12,
                       textColor=C_DIM)))

    # --- login card -------------------------------------------------------
    v.append(bean("card_login", T_LINEAR, "LinearLayout", "linear_content", 3,
                  layout(width=-1, height=-2, orientation=1, marginTop=26,
                         paddingLeft=18, paddingRight=18,
                         paddingTop=18, paddingBottom=18,
                         backgroundResource="card_bg")))
    v.append(bean("tv_label_user", T_TEXTVIEW, "TextView", "card_login", 0,
                  layout(width=-2, height=-2, marginBottom=6),
                  text(text="USERNAME", textSize=10, textType=1,
                       textColor=C_VIOLET)))
    v.append(bean("et_username", T_EDITTEXT, "EditText", "card_login", 1,
                  layout(width=-1, height=-2, paddingLeft=12, paddingRight=12,
                         paddingTop=12, paddingBottom=12,
                         backgroundResource="input_bg"),
                  text(hint="Enter username", textSize=14, textColor=C_TEXT,
                       hintColor=C_HINT, inputType=1, singleLine=1)))
    v.append(bean("tv_label_pass", T_TEXTVIEW, "TextView", "card_login", 2,
                  layout(width=-2, height=-2, marginTop=16, marginBottom=6),
                  text(text="PASSWORD", textSize=10, textType=1,
                       textColor=C_VIOLET)))
    v.append(bean("et_password", T_EDITTEXT, "EditText", "card_login", 3,
                  layout(width=-1, height=-2, paddingLeft=12, paddingRight=12,
                         paddingTop=12, paddingBottom=12,
                         backgroundResource="input_bg"),
                  text(hint="Enter password", textSize=14, textColor=C_TEXT,
                       hintColor=C_HINT, inputType=129, singleLine=1)))

    # --- actions ----------------------------------------------------------
    v.append(bean("btn_login", T_BUTTON, "Button", "linear_content", 4,
                  layout(width=-1, height=48, marginTop=18,
                         backgroundResource="btn_primary"),
                  text(text="L O G I N", textSize=15, textType=1,
                       textColor=C_DARK)))
    v.append(bean("tv_hint", T_TEXTVIEW, "TextView", "linear_content", 5,
                  layout(width=-2, height=-2, layoutGravity=1, marginTop=12),
                  text(text="Default login - %s / %s" % (LOGIN_USER, LOGIN_PASS),
                       textSize=10, textColor=C_MUTED)))
    v.append(bean("tv_msg", T_TEXTVIEW, "TextView", "linear_content", 6,
                  layout(width=-2, height=-2, layoutGravity=1, marginTop=10),
                  text(text="", textSize=12, textType=1, textColor=C_DANGER)))
    v.append(bean("tv_footer", T_TEXTVIEW, "TextView", "linear_content", 7,
                  layout(width=-2, height=-2, layoutGravity=1, marginTop=22),
                  text(text="v%s  -  Shadow Injector" % VER_NAME, textSize=10,
                       textColor=C_FAINT)))
    return v


def injector_views():
    v = []
    v.append(bean("rel_root", T_RELATIVE, "RelativeLayout", "root", 0,
                  layout(width=-1, height=-1, orientation=-1)))
    v.append(bean("linear_content", T_LINEAR, "LinearLayout", "rel_root", 0,
                  layout(width=-1, height=-1, orientation=1, gravity=1,
                         paddingLeft=22, paddingRight=22,
                         paddingTop=22, paddingBottom=22)))

    # --- header -----------------------------------------------------------
    v.append(bean("linear_top", T_LINEAR, "LinearLayout", "linear_content", 0,
                  layout(width=-1, height=-2, orientation=0, gravity=16)))
    v.append(bean("img_avatar", T_IMAGEVIEW, "ImageView", "linear_top", 0,
                  layout(width=46, height=46),
                  image_=image("logo", "FIT_CENTER")))
    v.append(bean("linear_greet", T_LINEAR, "LinearLayout", "linear_top", 1,
                  layout(width=0, height=-2, weight=1, orientation=1,
                         paddingLeft=12)))
    v.append(bean("tv_welcome", T_TEXTVIEW, "TextView", "linear_greet", 0,
                  layout(width=-2, height=-2),
                  text(text="Welcome back,", textSize=11, textColor=C_DIM)))
    v.append(bean("tv_username", T_TEXTVIEW, "TextView", "linear_greet", 1,
                  layout(width=-2, height=-2),
                  text(text="Shadow", textSize=17, textType=1, textColor=C_TEXT)))
    v.append(bean("tv_badge", T_TEXTVIEW, "TextView", "linear_top", 2,
                  layout(width=-2, height=-2, paddingLeft=8, paddingRight=8,
                         paddingTop=4, paddingBottom=4,
                         backgroundResource="badge_bg"),
                  text(text="PRO", textSize=9, textType=1, textColor=C_DARK)))

    # --- shizuku card -----------------------------------------------------
    v.append(bean("card_shizuku", T_LINEAR, "LinearLayout", "linear_content", 1,
                  layout(width=-1, height=-2, orientation=1, marginTop=24,
                         paddingLeft=16, paddingRight=16,
                         paddingTop=16, paddingBottom=16,
                         backgroundResource="card_bg")))
    v.append(bean("tv_shizuku_label", T_TEXTVIEW, "TextView", "card_shizuku", 0,
                  layout(width=-2, height=-2),
                  text(text="SHIZUKU STATUS", textSize=10, textType=1,
                       textColor=C_VIOLET)))
    v.append(bean("tv_shizuku", T_TEXTVIEW, "TextView", "card_shizuku", 1,
                  layout(width=-1, height=-2, marginTop=8),
                  text(text="Tap the button to check", textSize=14, textType=1,
                       textColor=C_DIM)))
    v.append(bean("btn_check", T_BUTTON, "Button", "card_shizuku", 2,
                  layout(width=-1, height=44, marginTop=14,
                         backgroundResource="btn_ghost"),
                  text(text="CHECK SHIZUKU", textSize=12, textType=1,
                       textColor=C_TEXT)))

    # --- injector card ----------------------------------------------------
    v.append(bean("card_inject", T_LINEAR, "LinearLayout", "linear_content", 2,
                  layout(width=-1, height=-2, orientation=1, marginTop=16,
                         paddingLeft=16, paddingRight=16,
                         paddingTop=16, paddingBottom=16,
                         backgroundResource="card_bg")))
    v.append(bean("tv_inject_label", T_TEXTVIEW, "TextView", "card_inject", 0,
                  layout(width=-2, height=-2),
                  text(text="INJECTION PANEL", textSize=10, textType=1,
                       textColor=C_CYAN)))
    v.append(bean("tv_inject_desc", T_TEXTVIEW, "TextView", "card_inject", 1,
                  layout(width=-1, height=-2, marginTop=6),
                  text(text="Launch the floating menu to control the injector",
                       textSize=11, textColor=C_DIM)))
    v.append(bean("btn_inject", T_BUTTON, "Button", "card_inject", 2,
                  layout(width=-1, height=52, marginTop=14,
                         backgroundResource="btn_primary"),
                  text(text="I N J E C T", textSize=16, textType=1,
                       textColor=C_DARK)))
    v.append(bean("btn_stop", T_BUTTON, "Button", "card_inject", 3,
                  layout(width=-1, height=44, marginTop=10,
                         backgroundResource="btn_danger"),
                  text(text="STOP OVERLAY", textSize=12, textType=1,
                       textColor=C_DANGER)))

    v.append(bean("tv_tip", T_TEXTVIEW, "TextView", "linear_content", 3,
                  layout(width=-2, height=-2, layoutGravity=1, marginTop=20),
                  text(text="Tip: drag the floating icon to move it",
                       textSize=10, textColor=C_FAINT)))
    return v


def build_view_file():
    out = []
    for xml, views in (("main.xml", main_views()), ("injector.xml", injector_views())):
        out.append("@" + xml)
        for b in views:
            out.append(json.dumps(b, separators=(",", ":"), ensure_ascii=False))
    for xml in ("main.xml", "injector.xml"):
        out.append("@" + xml + "_fab")
        out.append(json.dumps(fab_bean(), separators=(",", ":"), ensure_ascii=False))
    return "\n".join(out) + "\n"


# --------------------------------------------------------------------------
# block logic
# --------------------------------------------------------------------------

ATTACH_BG = (
    'if (android.os.Build.VERSION.SDK_INT >= 21) { '
    'getWindow().setStatusBarColor(0xff0a0616); '
    'getWindow().setNavigationBarColor(0xff07050e); } '
    '((android.widget.RelativeLayout) findViewById(R.id.rel_root)).addView('
    'new AnimeBackground(this), 0, '
    'new android.widget.RelativeLayout.LayoutParams(-1, -1));'
)

CHECK_SHIZUKU = (
    'ShizukuCheck.checkAsync(new ShizukuCheck.Callback() { '
    'public void onResult(final boolean running) { '
    'tv_shizuku.setText(ShizukuCheck.getStatusText(running)); '
    'tv_shizuku.setTextColor(ShizukuCheck.getStatusColor(running)); '
    'android.widget.Toast.makeText(getApplicationContext(), '
    'ShizukuCheck.getStatusText(running), '
    'android.widget.Toast.LENGTH_SHORT).show(); } });'
)


def main_activity_logic():
    """Login screen logic."""
    # ---- onCreate --------------------------------------------------------
    on_create = []
    attach = b_raw(ATTACH_BG)
    on_create.append(attach)

    # ---- btn_login onClick ----------------------------------------------
    blocks = []

    user_get = b_get_text("et_username")
    pass_get = b_get_text("et_password")
    user_eq = b_equals(ref(user_get), LOGIN_USER)
    pass_eq = b_equals(ref(pass_get), LOGIN_PASS)

    # success chain
    msg_ok = b_set_text("tv_msg", "Access granted")
    toast_ok = b_toast("Welcome back")
    set_screen = b_set_screen("InjectorActivity")
    put_extra = b_put_extra("user", ref(user_get))
    shizuku = b_raw("ShizukuCheck.showStatus(this);")
    start = b_start_activity()
    finish = b_finish()
    msg_ok["nextBlock"] = int(toast_ok["id"])
    toast_ok["nextBlock"] = int(set_screen["id"])
    set_screen["nextBlock"] = int(put_extra["id"])
    put_extra["nextBlock"] = int(shizuku["id"])
    shizuku["nextBlock"] = int(start["id"])
    start["nextBlock"] = int(finish["id"])

    # wrong password
    msg_bad_pass = b_set_text("tv_msg", "Wrong password")
    toast_bad_pass = b_toast("Access denied")
    msg_bad_pass["nextBlock"] = int(toast_bad_pass["id"])

    # wrong username
    msg_bad_user = b_set_text("tv_msg", "Unknown username")
    toast_bad_user = b_toast("Access denied")
    msg_bad_user["nextBlock"] = int(toast_bad_user["id"])

    inner_if = b_if_else(ref(pass_eq), int(msg_ok["id"]), int(msg_bad_pass["id"]))
    outer_if = b_if_else(ref(user_eq), int(inner_if["id"]), int(msg_bad_user["id"]))

    blocks = [outer_if, user_eq, user_get,
              inner_if, pass_eq, pass_get,
              msg_ok, toast_ok, set_screen, put_extra, shizuku, start, finish,
              msg_bad_pass, toast_bad_pass,
              msg_bad_user, toast_bad_user]

    return {
        "var": [],
        "func": [],
        "components": [{"componentId": "intent", "param1": "", "param2": "",
                        "param3": "", "type": 1}],
        "events": [
            {"eventName": "initializeLogic", "eventType": 0,
             "targetId": "onCreate", "targetType": 0},
            {"eventName": "onClick", "eventType": 1,
             "targetId": "btn_login", "targetType": 3},
        ],
        "sections": {
            "onCreate_initializeLogic": on_create,
            "btn_login_onClick": blocks,
        },
    }


def injector_activity_logic():
    # ---- onCreate --------------------------------------------------------
    attach = b_raw(ATTACH_BG)
    greet = b_raw(
        'String _shadowUser = getIntent().getStringExtra("user"); '
        'if (_shadowUser != null && _shadowUser.length() > 0) { '
        'tv_username.setText(_shadowUser); }'
    )
    attach["nextBlock"] = int(greet["id"])
    on_create = [attach, greet]

    # ---- btn_check onClick ----------------------------------------------
    check = [b_raw(CHECK_SHIZUKU)]

    # ---- btn_inject onClick ---------------------------------------------
    can_draw = b_raw_b("FloatingService.canDrawOverlays(this)")
    launch = b_raw("FloatingService.start(this);")
    toast_launch = b_toast("Floating menu launched")
    shizuku = b_raw("ShizukuCheck.showStatus(this);")
    launch["nextBlock"] = int(toast_launch["id"])
    toast_launch["nextBlock"] = int(shizuku["id"])

    ask_perm = b_raw("FloatingService.requestOverlayPermission(this);")
    toast_perm = b_toast("Allow display over other apps to use the menu")
    ask_perm["nextBlock"] = int(toast_perm["id"])

    inject_if = b_if_else(ref(can_draw), int(launch["id"]), int(ask_perm["id"]))
    inject = [inject_if, can_draw, launch, toast_launch, shizuku,
              ask_perm, toast_perm]

    # ---- btn_stop onClick ------------------------------------------------
    stop = b_raw("FloatingService.stop(this);")
    toast_stop = b_toast("Overlay stopped")
    stop["nextBlock"] = int(toast_stop["id"])
    stop_blocks = [stop, toast_stop]

    return {
        "var": [],
        "func": [],
        "components": [],
        "events": [
            {"eventName": "initializeLogic", "eventType": 0,
             "targetId": "onCreate", "targetType": 0},
            {"eventName": "onClick", "eventType": 1,
             "targetId": "btn_check", "targetType": 3},
            {"eventName": "onClick", "eventType": 1,
             "targetId": "btn_inject", "targetType": 3},
            {"eventName": "onClick", "eventType": 1,
             "targetId": "btn_stop", "targetType": 3},
        ],
        "sections": {
            "onCreate_initializeLogic": on_create,
            "btn_check_onClick": check,
            "btn_inject_onClick": inject,
            "btn_stop_onClick": stop_blocks,
        },
    }


def build_logic_file():
    out = []
    for activity, logic in (("MainActivity", main_activity_logic()),
                            ("InjectorActivity", injector_activity_logic())):
        java = activity + ".java"
        out.append("@" + java + "_var")
        for line in logic["var"]:
            out.append(line)
        out.append("@" + java + "_func")
        for line in logic["func"]:
            out.append(line)
        out.append("@" + java + "_components")
        for c in logic["components"]:
            out.append(json.dumps(c, separators=(",", ":")))
        out.append("@" + java + "_events")
        for e in logic["events"]:
            out.append(json.dumps(e, separators=(",", ":")))
        for key, blocks in logic["sections"].items():
            out.append("@" + java + "_" + key)
            for b in blocks:
                out.append(json.dumps(b, separators=(",", ":"),
                                      ensure_ascii=False))
    return "\n".join(out) + "\n"


# --------------------------------------------------------------------------
# the remaining project files
# --------------------------------------------------------------------------

def build_project_json():
    return json.dumps({
        "custom_icon": True,
        "sc_ver_code": VER_CODE,
        "my_ws_name": WS_NAME,
        "color_accent": float(col(0xFF22D3EE)),
        "my_app_name": APP_NAME,
        "sc_ver_name": VER_NAME,
        "sc_id": SC_ID,
        "color_primary": float(col(0xFFB14CFF)),
        "color_control_highlight": float(col(0xFF8B5CF6)),
        "color_control_normal": float(col(0xFF3A2F5C)),
        "my_sc_reg_dt": datetime.now().strftime("%Y%m%d%H%M%S"),
        "sketchware_ver": 150,
        "isIconAdaptive": False,
        "my_sc_pkg_name": PKG,
        "color_primary_dark": float(col(0xFF6D28D9)),
    }, separators=(",", ":"))


PROJECT_CONFIG = json.dumps({
    "min_sdk": "21",
    "enable_bridgeless_themes": "false",
    "enable_viewbinding": "false",
    "app_class": ".SketchApplication",
    "target_sdk": "33",
    "disable_old_methods": "false",
    "xml_command": "true",
}, separators=(",", ":"))

PERMISSION = json.dumps([
    "android.permission.INTERNET",
    "android.permission.SYSTEM_ALERT_WINDOW",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.WAKE_LOCK",
], separators=(",", ":"))

PROGUARD = json.dumps({"debug": "false", "enabled": "false"},
                      separators=(",", ":"))

PROGUARD_RULES = "-repackageclasses\n-ignorewarnings\n-dontwarn\n-dontnote\n"

STRINGFOG = json.dumps({"enabled": "false"}, separators=(",", ":"))

CUSTOM_BLOCKS = "[]"

VIEW_ROOT = json.dumps({
    "main.xml": {
        "class_name": "LinearLayout",
        "attributes": {
            "android:layout_width": "match_parent",
            "android:layout_height": "match_parent",
            "android:orientation": "vertical",
            "android:fitsSystemWindows": "true",
            "android:background": "#07050E",
        },
    },
    "injector.xml": {
        "class_name": "LinearLayout",
        "attributes": {
            "android:layout_width": "match_parent",
            "android:layout_height": "match_parent",
            "android:orientation": "vertical",
            "android:fitsSystemWindows": "true",
            "android:background": "#07050E",
        },
    },
}, separators=(",", ":"))

APP_COMPONENTS = (
    '<service\n'
    '    android:name=".FloatingService"\n'
    '    android:enabled="true"\n'
    '    android:exported="false" />\n'
)


def build_file_file():
    act = {"fileName": "main", "fileType": 0, "keyboardSetting": 0,
           "options": 0, "orientation": 0, "theme": -1}
    inj = {"fileName": "injector", "fileType": 0, "keyboardSetting": 0,
           "options": 0, "orientation": 0, "theme": -1}
    lines = ["@activity",
             json.dumps(act, separators=(",", ":")),
             json.dumps(inj, separators=(",", ":")),
             "@customview"]
    return "\n".join(lines) + "\n"


def build_resource_file():
    lines = ["@images",
             json.dumps({"resFullName": "logo.png", "resName": "logo",
                         "resType": 1}, separators=(",", ":")),
             json.dumps({"resFullName": "floating_icon.png",
                         "resName": "floating_icon",
                         "resType": 1}, separators=(",", ":")),
             "@sounds", "@fonts"]
    return "\n".join(lines) + "\n"


def build_library_file():
    def lib(lib_type, use):
        return json.dumps({"adUnits": [], "appId": "", "data": "",
                           "libType": lib_type, "reserved1": "", "reserved2": "",
                           "reserved3": "", "testDevices": [], "useYn": use},
                          separators=(",", ":"))

    return "\n".join(["@firebaseDB", lib(0, "N"),
                      "@compat", lib(1, "Y"),
                      "@admob", lib(2, "N"),
                      "@googleMap", lib(3, "N")]) + "\n"


# --------------------------------------------------------------------------
# assembly
# --------------------------------------------------------------------------

def main():
    tmp = os.path.join(HERE, "_tmp")
    if os.path.isdir(tmp):
        shutil.rmtree(tmp)
    os.makedirs(tmp)

    # 1. static tree from src/
    for item in os.listdir(SRC):
        s = os.path.join(SRC, item)
        d = os.path.join(tmp, item)
        if os.path.isdir(s):
            shutil.copytree(s, d)
        else:
            shutil.copy2(s, d)

    data = os.path.join(tmp, "data")
    os.makedirs(data, exist_ok=True)

    def w(rel, content, binary=False):
        path = os.path.join(tmp, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        if binary:
            with open(path, "wb") as f:
                f.write(content)
        else:
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(content)

    # 2. encrypted files
    w("project", aes_encrypt(build_project_json()), binary=True)
    w("data/file", aes_encrypt(build_file_file()), binary=True)
    w("data/view", aes_encrypt(build_view_file()), binary=True)
    w("data/logic", aes_encrypt(build_logic_file()), binary=True)
    w("data/resource", aes_encrypt(build_resource_file()), binary=True)
    w("data/library", aes_encrypt(build_library_file()), binary=True)

    # 3. plain files
    w("data/project_config", PROJECT_CONFIG)
    w("data/permission", PERMISSION)
    w("data/proguard", PROGUARD)
    w("data/proguard-rules.pro", PROGUARD_RULES)
    w("data/stringfog", STRINGFOG)
    w("data/custom_blocks", CUSTOM_BLOCKS)
    w("data/view_root", VIEW_ROOT)
    w("data/java", "")
    w("data/Injection/androidmanifest/app_components.txt", APP_COMPONENTS)

    # 4. zip it up
    if os.path.isdir(OUT_DIR):
        shutil.rmtree(OUT_DIR)
    os.makedirs(OUT_DIR, exist_ok=True)
    out_name = "%s v%s (%s, %s).swb" % (APP_NAME, VER_NAME, PKG, VER_CODE)
    out_path = os.path.join(OUT_DIR, out_name)

    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as z:
        for root, _dirs, files in os.walk(tmp):
            for name in files:
                full = os.path.join(root, name)
                rel = os.path.relpath(full, tmp)
                z.write(full, rel)

    shutil.rmtree(tmp)
    print("built: %s (%d bytes)" % (out_path, os.path.getsize(out_path)))
    return out_path


if __name__ == "__main__":
    sys.exit(0 if main() else 1)
