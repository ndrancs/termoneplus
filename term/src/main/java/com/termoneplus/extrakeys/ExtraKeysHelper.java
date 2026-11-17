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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ExtraKeysHelper {
    public static final String PREF_EXTRA_KEYS = "extra_keys_config";
    public static final String TERMUX_DIR = ".termux";
    public static final String TERMUX_PROPERTIES = "termux.properties";

    public static File getPropertiesFile(String homePath) {
        if (homePath == null) return null;
        File dir = new File(homePath, TERMUX_DIR);
        return new File(dir, TERMUX_PROPERTIES);
    }

    public static ExtraKeysConfig loadConfig(Context context, String homePath) {
        // 1. Try reading from .termux/termux.properties
        File propFile = getPropertiesFile(homePath);
        if (propFile != null && propFile.exists()) {
            String propValue = readExtraKeysFromPropertiesFile(propFile);
            if (propValue != null && !propValue.trim().isEmpty()) {
                return ExtraKeysConfig.parse(propValue);
            }
        }

        // 2. Try reading from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String prefVal = prefs.getString(PREF_EXTRA_KEYS, null);
        if (prefVal != null && !prefVal.trim().isEmpty()) {
            return ExtraKeysConfig.parse(prefVal);
        }

        // 3. Default
        return ExtraKeysConfig.getDefaultConfig();
    }

    public static void saveConfig(Context context, String homePath, ExtraKeysConfig config) {
        String jsonString = config.toJSONString();

        // 1. Save to SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_EXTRA_KEYS, jsonString).apply();

        // 2. Save to .termux/termux.properties
        if (homePath != null) {
            File propFile = getPropertiesFile(homePath);
            if (propFile != null) {
                File dir = propFile.getParentFile();
                if (dir != null && !dir.exists()) {
                    dir.mkdirs();
                }
                updatePropertiesFile(propFile, config.toTermuxPropertyString());
            }
        }
    }

    public static String readRawPropertiesFile(String homePath) {
        File propFile = getPropertiesFile(homePath);
        if (propFile != null && propFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(propFile))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (IOException ignored) {
            }
        }
        return "";
    }

    public static void saveRawPropertiesFile(Context context, String homePath, String rawContent) {
        if (homePath != null) {
            File propFile = getPropertiesFile(homePath);
            if (propFile != null) {
                File dir = propFile.getParentFile();
                if (dir != null && !dir.exists()) {
                    dir.mkdirs();
                }
                try (PrintWriter writer = new PrintWriter(new FileWriter(propFile))) {
                    writer.print(rawContent);
                    writer.flush();
                } catch (IOException ignored) {
                }
            }
        }

        ExtraKeysConfig config = ExtraKeysConfig.parse(rawContent);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_EXTRA_KEYS, config.toJSONString()).apply();
    }

    private static String readExtraKeysFromPropertiesFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            boolean readingExtraKeys = false;

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) continue;

                if (!readingExtraKeys) {
                    if (trimmed.startsWith("extra-keys")) {
                        int eq = trimmed.indexOf('=');
                        if (eq != -1) {
                            String val = trimmed.substring(eq + 1).trim();
                            sb.append(val);
                            if (val.endsWith("\\")) {
                                readingExtraKeys = true;
                            } else {
                                break;
                            }
                        }
                    }
                } else {
                    sb.append("\n").append(trimmed);
                    if (!trimmed.endsWith("\\")) {
                        break;
                    }
                }
            }
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static void updatePropertiesFile(File file, String extraKeysPropertyBlock) {
        List<String> lines = new ArrayList<>();
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                boolean inExtraKeys = false;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!inExtraKeys) {
                        if (trimmed.startsWith("extra-keys")) {
                            int eq = trimmed.indexOf('=');
                            if (eq != -1 && trimmed.endsWith("\\")) {
                                inExtraKeys = true;
                            }
                            continue;
                        }
                        lines.add(line);
                    } else {
                        if (!trimmed.endsWith("\\")) {
                            inExtraKeys = false;
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (String l : lines) {
                writer.println(l);
            }
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).trim().isEmpty()) {
                writer.println();
            }
            writer.println("# Extra keys configuration");
            writer.println(extraKeysPropertyBlock);
            writer.flush();
        } catch (IOException ignored) {
        }
    }
}
