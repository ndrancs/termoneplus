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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ExtraKeysConfig {
    private final List<List<ExtraKeyInfo>> rows = new ArrayList<>();

    public ExtraKeysConfig() {
    }

    public List<List<ExtraKeyInfo>> getRows() {
        return rows;
    }

    public static ExtraKeysConfig getDefaultConfig() {
        ExtraKeysConfig config = new ExtraKeysConfig();

        List<ExtraKeyInfo> row1 = new ArrayList<>();
        row1.add(new ExtraKeyInfo("ESC"));
        row1.add(new ExtraKeyInfo("TAB"));
        row1.add(new ExtraKeyInfo("CTRL"));
        row1.add(new ExtraKeyInfo("ALT"));
        row1.add(new ExtraKeyInfo("-"));
        row1.add(new ExtraKeyInfo("_"));
        row1.add(new ExtraKeyInfo("HOME"));
        row1.add(new ExtraKeyInfo("UP"));
        row1.add(new ExtraKeyInfo("END"));
        config.rows.add(row1);

        List<ExtraKeyInfo> row2 = new ArrayList<>();
        row2.add(new ExtraKeyInfo("KEYBOARD"));
        row2.add(new ExtraKeyInfo("~"));
        row2.add(new ExtraKeyInfo("/"));
        row2.add(new ExtraKeyInfo("|"));
        row2.add(new ExtraKeyInfo("PGUP"));
        row2.add(new ExtraKeyInfo("PGDN"));
        row2.add(new ExtraKeyInfo("LEFT"));
        row2.add(new ExtraKeyInfo("DOWN"));
        row2.add(new ExtraKeyInfo("RIGHT"));
        config.rows.add(row2);

        return config;
    }

    public static ExtraKeysConfig parse(String text) {
        if (TextUtils.isEmpty(text)) {
            return getDefaultConfig();
        }
        String jsonText = text.trim();
        // If text contains 'extra-keys =', extract the right side
        if (jsonText.contains("extra-keys")) {
            int eqIdx = jsonText.indexOf('=');
            if (eqIdx != -1) {
                jsonText = jsonText.substring(eqIdx + 1).trim();
            }
        }

        // Handle line continuations (trailing backslashes)
        jsonText = jsonText.replaceAll("\\\\\\r?\\n", "").trim();

        // Relaxed JSON: replace single quotes with double quotes & normalize
        jsonText = normalizeJson(jsonText);

        ExtraKeysConfig config = new ExtraKeysConfig();
        try {
            JSONArray array = new JSONArray(jsonText);
            if (array.length() == 0) {
                return getDefaultConfig();
            }

            Object first = array.get(0);
            if (first instanceof JSONArray) {
                for (int i = 0; i < array.length(); i++) {
                    JSONArray rowArray = array.getJSONArray(i);
                    List<ExtraKeyInfo> row = parseRow(rowArray);
                    if (!row.isEmpty()) {
                        config.rows.add(row);
                    }
                }
            } else {
                // 1D array - wrap into 1 row
                List<ExtraKeyInfo> row = parseRow(array);
                if (!row.isEmpty()) {
                    config.rows.add(row);
                }
            }
        } catch (Exception e) {
            return getDefaultConfig();
        }

        if (config.rows.isEmpty()) {
            return getDefaultConfig();
        }

        return config;
    }

    private static List<ExtraKeyInfo> parseRow(JSONArray rowArray) throws JSONException {
        List<ExtraKeyInfo> row = new ArrayList<>();
        for (int j = 0; j < rowArray.length(); j++) {
            Object item = rowArray.get(j);
            if (item instanceof JSONObject) {
                JSONObject obj = (JSONObject) item;
                String k = obj.has("key") ? obj.optString("key", null) : null;
                String d = obj.has("display") ? obj.optString("display", null) : null;
                String m = obj.has("macro") ? obj.optString("macro", null) : null;
                String p = obj.has("popup") ? obj.optString("popup", null) : null;
                row.add(new ExtraKeyInfo(k, d, m, p));
            } else if (item instanceof String) {
                row.add(new ExtraKeyInfo((String) item));
            } else {
                row.add(new ExtraKeyInfo(String.valueOf(item)));
            }
        }
        return row;
    }

    private static String normalizeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                sb.append('"');
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                sb.append('"');
                inDoubleQuote = !inDoubleQuote;
            } else {
                sb.append(c);
            }
        }
        String str = sb.toString();
        str = str.replaceAll("(?<=\\{|,|\\s)(key|display|macro|popup)\\s*:", "\"$1\":");
        return str;
    }

    public String toJSONString() {
        JSONArray outer = new JSONArray();
        for (List<ExtraKeyInfo> row : rows) {
            JSONArray inner = new JSONArray();
            for (ExtraKeyInfo info : row) {
                inner.put(info.toJSONValue());
            }
            outer.put(inner);
        }
        return outer.toString();
    }

    public String toPrettyJSONString() {
        try {
            JSONArray outer = new JSONArray();
            for (List<ExtraKeyInfo> row : rows) {
                JSONArray inner = new JSONArray();
                for (ExtraKeyInfo info : row) {
                    inner.put(info.toJSONValue());
                }
                outer.put(inner);
            }
            return outer.toString(2);
        } catch (JSONException e) {
            return toJSONString();
        }
    }

    public String toTermuxPropertyString() {
        StringBuilder sb = new StringBuilder("extra-keys = [ \\\n");
        for (int i = 0; i < rows.size(); i++) {
            List<ExtraKeyInfo> row = rows.get(i);
            sb.append("  [");
            for (int j = 0; j < row.size(); j++) {
                ExtraKeyInfo info = row.get(j);
                Object val = info.toJSONValue();
                if (val instanceof String) {
                    sb.append("'").append(((String) val).replace("'", "\\'")).append("'");
                } else {
                    sb.append(val.toString());
                }
                if (j < row.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            if (i < rows.size() - 1) {
                sb.append(", \\\n");
            } else {
                sb.append(" \\\n");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
