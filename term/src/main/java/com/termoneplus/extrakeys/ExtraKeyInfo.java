/*
 * Copyright (C) 2025 Roumen Petrov.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.termoneplus.extrakeys;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class ExtraKeyInfo {
    private String key;
    private String display;
    private String macro;
    private String popup;

    public ExtraKeyInfo(String key) {
        this.key = key;
    }

    public ExtraKeyInfo(String key, String display) {
        this.key = key;
        this.display = display;
    }

    public ExtraKeyInfo(String key, String display, String macro, String popup) {
        this.key = key;
        this.display = display;
        this.macro = macro;
        this.popup = popup;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getMacro() {
        return macro;
    }

    public void setMacro(String macro) {
        this.macro = macro;
    }

    public String getPopup() {
        return popup;
    }

    public void setPopup(String popup) {
        this.popup = popup;
    }

    public String getDisplayLabel() {
        if (!TextUtils.isEmpty(display)) {
            return display;
        }
        if (key != null) {
            String upper = key.toUpperCase();
            switch (upper) {
                case "UP":
                case "DPAD_UP":
                    return "↑";
                case "DOWN":
                case "DPAD_DOWN":
                    return "↓";
                case "LEFT":
                case "DPAD_LEFT":
                    return "←";
                case "RIGHT":
                case "DPAD_RIGHT":
                    return "→";
                case "PAGEUP":
                case "PAGE_UP":
                case "PGUP":
                    return "PGUP";
                case "PAGEDOWN":
                case "PAGE_DOWN":
                case "PGDN":
                    return "PGDN";
                case "KEYBOARD":
                case "KEY":
                    return "KEY";
                case "ENTER":
                case "RET":
                    return "ENTER";
                case "BACKSPACE":
                case "BKSP":
                    return "BKSP";
                case "DELETE":
                case "DEL":
                    return "DEL";
                case "INSERT":
                case "INS":
                    return "INS";
                default:
                    return key;
            }
        }
        if (macro != null) {
            return macro;
        }
        return "";
    }

    public Object toJSONValue() {
        if (TextUtils.isEmpty(display) && TextUtils.isEmpty(macro) && TextUtils.isEmpty(popup) && key != null) {
            return key;
        }
        JSONObject json = new JSONObject();
        try {
            if (!TextUtils.isEmpty(key)) json.put("key", key);
            if (!TextUtils.isEmpty(display)) json.put("display", display);
            if (!TextUtils.isEmpty(macro)) json.put("macro", macro);
            if (!TextUtils.isEmpty(popup)) json.put("popup", popup);
        } catch (JSONException e) {
            // ignore
        }
        return json;
    }
}
