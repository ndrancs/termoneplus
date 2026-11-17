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

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.termoneplus.AppCompatActivity;
import com.termoneplus.R;

import java.util.ArrayList;
import java.util.List;

import jackpal.androidterm.util.TermSettings;

public class ExtraKeysActivity extends AppCompatActivity {
    private ExtraKeysConfig currentConfig;
    private String homePath;

    private TableLayout previewContainer;
    private LinearLayout rowsContainer;
    private View visualEditorContainer;
    private View codeEditorContainer;
    private EditText editPropertiesCode;
    private RadioGroup modeRadioGroup;

    private boolean isUpdatingCode = false;

    private static final String[] PRESET_KEYS = {
            "ESC", "TAB", "CTRL", "ALT", "HOME", "END", "PGUP", "PGDN",
            "UP", "DOWN", "LEFT", "RIGHT", "KEYBOARD", "ENTER", "BKSP", "DEL",
            "INS", "PASTE", "COPY", "-", "_", "~", "/", "|", ":", ";", "=", "+",
            "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra_keys);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.extra_keys_title);
        }

        TermSettings settings = new TermSettings(this);
        homePath = settings.getHomePath();
        currentConfig = ExtraKeysHelper.loadConfig(this, homePath);

        previewContainer = findViewById(R.id.preview_container);
        rowsContainer = findViewById(R.id.rows_container);
        visualEditorContainer = findViewById(R.id.visual_editor_container);
        codeEditorContainer = findViewById(R.id.code_editor_container);
        editPropertiesCode = findViewById(R.id.edit_properties_code);
        modeRadioGroup = findViewById(R.id.mode_radio_group);

        findViewById(R.id.btn_add_row).setOnClickListener(v -> {
            currentConfig.getRows().add(new ArrayList<>());
            refreshUI();
        });

        findViewById(R.id.btn_reset_default).setOnClickListener(v -> {
            currentConfig = ExtraKeysConfig.getDefaultConfig();
            refreshUI();
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            if (codeEditorContainer.getVisibility() == View.VISIBLE) {
                syncFromCodeEditor();
            }
            ExtraKeysHelper.saveConfig(this, homePath, currentConfig);
            Toast.makeText(this, R.string.extra_keys_saved_toast, Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btn_format_json).setOnClickListener(v -> {
            syncFromCodeEditor();
            updateCodeEditor();
        });

        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_visual_editor) {
                syncFromCodeEditor();
                visualEditorContainer.setVisibility(View.VISIBLE);
                codeEditorContainer.setVisibility(View.GONE);
                refreshUI();
            } else if (checkedId == R.id.radio_code_editor) {
                visualEditorContainer.setVisibility(View.GONE);
                codeEditorContainer.setVisibility(View.VISIBLE);
                updateCodeEditor();
            }
        });

        editPropertiesCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdatingCode) return;
                try {
                    ExtraKeysConfig parsed = ExtraKeysConfig.parse(s.toString());
                    if (parsed != null && !parsed.getRows().isEmpty()) {
                        currentConfig = parsed;
                        updatePreview();
                    }
                } catch (Exception ignored) {}
            }
        });

        refreshUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshUI() {
        updatePreview();
        buildVisualEditor();
        updateCodeEditor();
    }

    private void updatePreview() {
        previewContainer.removeAllViews();
        List<List<ExtraKeyInfo>> rows = currentConfig.getRows();
        for (int r = 0; r < rows.size(); r++) {
            List<ExtraKeyInfo> row = rows.get(r);
            TableRow tableRow = new TableRow(this);
            TableLayout.LayoutParams rowParams = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.WRAP_CONTENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);
            if (r > 0) {
                rowParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
            }
            tableRow.setLayoutParams(rowParams);

            for (ExtraKeyInfo info : row) {
                Button button = (Button) getLayoutInflater().inflate(R.layout.extra_key_button, tableRow, false);
                button.setText(info.getDisplayLabel());
                tableRow.addView(button);
            }
            previewContainer.addView(tableRow);
        }
    }

    private void buildVisualEditor() {
        rowsContainer.removeAllViews();
        List<List<ExtraKeyInfo>> rows = currentConfig.getRows();

        for (int r = 0; r < rows.size(); r++) {
            final int rowIndex = r;
            List<ExtraKeyInfo> row = rows.get(r);

            LinearLayout cardView = new LinearLayout(this);
            cardView.setOrientation(LinearLayout.VERTICAL);
            cardView.setPadding(16, 16, 16, 16);
            cardView.setBackgroundColor(0xFF222222);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
            cardView.setLayoutParams(cardParams);

            // Row header
            LinearLayout headerLayout = new LinearLayout(this);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView titleView = new TextView(this);
            titleView.setText(getString(R.string.extra_keys_row_title, rowIndex + 1));
            titleView.setTextSize(14);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            titleView.setTextColor(0xFFFFFFFF);
            headerLayout.addView(titleView);

            View space = new View(this);
            LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(0, 0, 1.0f);
            space.setLayoutParams(spaceParams);
            headerLayout.addView(space);

            if (rowIndex > 0) {
                Button btnUp = new Button(this, null, android.R.attr.borderlessButtonStyle);
                btnUp.setText("▲");
                btnUp.setTextColor(0xFFFFFFFF);
                btnUp.setOnClickListener(v -> {
                    List<ExtraKeyInfo> tmp = rows.remove(rowIndex);
                    rows.add(rowIndex - 1, tmp);
                    refreshUI();
                });
                headerLayout.addView(btnUp);
            }

            if (rowIndex < rows.size() - 1) {
                Button btnDown = new Button(this, null, android.R.attr.borderlessButtonStyle);
                btnDown.setText("▼");
                btnDown.setTextColor(0xFFFFFFFF);
                btnDown.setOnClickListener(v -> {
                    List<ExtraKeyInfo> tmp = rows.remove(rowIndex);
                    rows.add(rowIndex + 1, tmp);
                    refreshUI();
                });
                headerLayout.addView(btnDown);
            }

            Button btnDelRow = new Button(this, null, android.R.attr.borderlessButtonStyle);
            btnDelRow.setText("✕");
            btnDelRow.setTextColor(0xFFFF5555);
            btnDelRow.setOnClickListener(v -> {
                rows.remove(rowIndex);
                refreshUI();
            });
            headerLayout.addView(btnDelRow);

            cardView.addView(headerLayout);

            // Keys horizontal scroll container
            HorizontalScrollView keyScroll = new HorizontalScrollView(this);
            LinearLayout keysLayout = new LinearLayout(this);
            keysLayout.setOrientation(LinearLayout.HORIZONTAL);

            for (int k = 0; k < row.size(); k++) {
                final int keyIndex = k;
                ExtraKeyInfo info = row.get(k);

                Button keyButton = (Button) getLayoutInflater().inflate(R.layout.extra_key_button, keysLayout, false);
                keyButton.setText(info.getDisplayLabel());
                keyButton.setOnClickListener(v -> showEditKeyDialog(rowIndex, keyIndex));

                keysLayout.addView(keyButton);
            }

            Button btnAddKey = new Button(this, null, android.R.attr.borderlessButtonStyle);
            btnAddKey.setText(R.string.extra_keys_add_key);
            btnAddKey.setTextColor(0xFF33B5E5);
            btnAddKey.setOnClickListener(v -> showEditKeyDialog(rowIndex, -1));
            keysLayout.addView(btnAddKey);

            keyScroll.addView(keysLayout);
            cardView.addView(keyScroll);

            rowsContainer.addView(cardView);
        }
    }

    private void updateCodeEditor() {
        isUpdatingCode = true;
        editPropertiesCode.setText(currentConfig.toPrettyJSONString());
        isUpdatingCode = false;
    }

    private void syncFromCodeEditor() {
        String text = editPropertiesCode.getText().toString();
        ExtraKeysConfig parsed = ExtraKeysConfig.parse(text);
        if (parsed != null && !parsed.getRows().isEmpty()) {
            currentConfig = parsed;
        }
    }

    private void showEditKeyDialog(int rowIndex, int keyIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(keyIndex >= 0 ? R.string.extra_keys_edit_key : R.string.extra_keys_new_key);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 24, 32, 24);

        List<List<ExtraKeyInfo>> rows = currentConfig.getRows();
        List<ExtraKeyInfo> row = rows.get(rowIndex);
        ExtraKeyInfo existing = keyIndex >= 0 ? row.get(keyIndex) : new ExtraKeyInfo("");

        // Preset selector label & flow
        TextView presetLabel = new TextView(this);
        presetLabel.setText(R.string.extra_keys_preset_keys);
        presetLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(presetLabel);

        HorizontalScrollView presetScroll = new HorizontalScrollView(this);
        LinearLayout presetLayout = new LinearLayout(this);
        presetLayout.setOrientation(LinearLayout.HORIZONTAL);

        final EditText editKey = new EditText(this);
        editKey.setHint(R.string.extra_keys_key_action);
        editKey.setText(existing.getKey());

        final EditText editDisplay = new EditText(this);
        editDisplay.setHint(R.string.extra_keys_display_label);
        editDisplay.setText(existing.getDisplay());

        final EditText editMacro = new EditText(this);
        editMacro.setHint(R.string.extra_keys_macro);
        editMacro.setText(existing.getMacro());

        final EditText editPopup = new EditText(this);
        editPopup.setHint(R.string.extra_keys_popup);
        editPopup.setText(existing.getPopup());

        for (String preset : PRESET_KEYS) {
            Button chip = new Button(this, null, android.R.attr.buttonStyleSmall);
            chip.setText(preset);
            chip.setOnClickListener(v -> editKey.setText(preset));
            presetLayout.addView(chip);
        }
        presetScroll.addView(presetLayout);
        layout.addView(presetScroll);

        TextView labelKey = new TextView(this);
        labelKey.setText(R.string.extra_keys_key_action);
        labelKey.setPadding(0, 16, 0, 0);
        layout.addView(labelKey);
        layout.addView(editKey);

        TextView labelDisplay = new TextView(this);
        labelDisplay.setText(R.string.extra_keys_display_label);
        labelDisplay.setPadding(0, 8, 0, 0);
        layout.addView(labelDisplay);
        layout.addView(editDisplay);

        TextView labelMacro = new TextView(this);
        labelMacro.setText(R.string.extra_keys_macro);
        labelMacro.setPadding(0, 8, 0, 0);
        layout.addView(labelMacro);
        layout.addView(editMacro);

        TextView labelPopup = new TextView(this);
        labelPopup.setText(R.string.extra_keys_popup);
        labelPopup.setPadding(0, 8, 0, 0);
        layout.addView(labelPopup);
        layout.addView(editPopup);

        builder.setView(layout);

        builder.setPositiveButton(R.string.extra_keys_save, (dialog, which) -> {
            String k = editKey.getText().toString().trim();
            String d = editDisplay.getText().toString().trim();
            String m = editMacro.getText().toString();
            String p = editPopup.getText().toString().trim();

            ExtraKeyInfo info = new ExtraKeyInfo(
                    TextUtils.isEmpty(k) ? null : k,
                    TextUtils.isEmpty(d) ? null : d,
                    TextUtils.isEmpty(m) ? null : m,
                    TextUtils.isEmpty(p) ? null : p
            );

            if (keyIndex >= 0) {
                row.set(keyIndex, info);
            } else {
                row.add(info);
            }
            refreshUI();
        });

        if (keyIndex >= 0) {
            builder.setNeutralButton(R.string.extra_keys_delete_key, (dialog, which) -> {
                row.remove(keyIndex);
                refreshUI();
            });
        }

        builder.setNegativeButton(R.string.extra_keys_cancel, null);
        builder.show();
    }
}
