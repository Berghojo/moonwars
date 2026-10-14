package com.limelight.preferences;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.binding.input.kbm.KbmAction;
import com.limelight.binding.input.kbm.KbmBindings;
import com.limelight.binding.input.kbm.KbmController;
import com.limelight.utils.UiHelper;

import java.util.List;

/**
 * Lets the user choose which key or mouse button each gamepad input sends
 * in gamepad-to-keyboard/mouse mode.
 */
public class KbmBindingsActivity extends Activity {
    private KbmBindings bindings;
    private LinearLayout rows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UiHelper.setLocale(this);
        setTitle(R.string.title_kbm_bindings_activity);

        bindings = KbmController.loadBindings(this);

        int padding = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);

        TextView header = new TextView(this);
        header.setText(R.string.kbm_bindings_header);
        header.setPadding(0, 0, 0, dp(8));
        root.addView(header);

        LinearLayout presets = newRow();
        presets.addView(newPresetButton(R.string.kbm_preset_bladesworn, true));
        presets.addView(newPresetButton(R.string.kbm_preset_gw2, false));
        root.addView(presets);

        LinearLayout columnHeader = newRow();
        columnHeader.addView(newLabel(getString(R.string.kbm_bindings_column_input), true), labelParams());
        columnHeader.addView(newLabel(getString(R.string.kbm_bindings_column_base), true), spinnerParams());
        columnHeader.addView(newLabel(getString(R.string.kbm_bindings_column_shift), true), spinnerParams());
        root.addView(columnHeader);

        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        root.addView(rows);
        populateRows();

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(root);
        setContentView(scrollView);

        UiHelper.notifyNewRootView(this);
    }

    private void populateRows() {
        rows.removeAllViews();
        List<KbmAction> actions = KbmAction.all();

        for (final KbmBindings.Input input : KbmBindings.Input.values()) {
            LinearLayout row = newRow();
            row.addView(newLabel(input.label, false), labelParams());

            row.addView(newSpinner(actions, bindings.getBase(input), new ActionSetter() {
                @Override
                public void set(KbmAction action) {
                    bindings.setBase(input, action);
                }
            }), spinnerParams());

            if (input.isStickDirection()) {
                // Movement doesn't change while the layer shift is held
                row.addView(new View(this), spinnerParams());
            }
            else {
                row.addView(newSpinner(actions, bindings.getShifted(input), new ActionSetter() {
                    @Override
                    public void set(KbmAction action) {
                        bindings.setShifted(input, action);
                    }
                }), spinnerParams());
            }

            rows.addView(row);
        }
    }

    private Button newPresetButton(final int nameRes, final boolean bladesworn) {
        Button button = new Button(this);
        button.setText(nameRes);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Build a fresh copy each time, since the spinners edit it in place
                bindings = bladesworn ? KbmBindings.bladeswornDefaults() : KbmBindings.guildWars2Defaults();
                KbmController.saveBindings(KbmBindingsActivity.this, bindings);
                populateRows();
                Toast.makeText(KbmBindingsActivity.this,
                        getString(R.string.kbm_bindings_preset_loaded, getString(nameRes)),
                        Toast.LENGTH_SHORT).show();
            }
        });
        return button;
    }

    private interface ActionSetter {
        void set(KbmAction action);
    }

    private Spinner newSpinner(final List<KbmAction> actions, KbmAction current, final ActionSetter setter) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<KbmAction> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, actions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int index = actions.indexOf(current);
        spinner.setSelection(Math.max(index, 0), false);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setter.set(actions.get(position));
                KbmController.saveBindings(KbmBindingsActivity.this, bindings);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return spinner;
    }

    private LinearLayout newRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));
        return row;
    }

    private TextView newLabel(String text, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        if (bold) {
            label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return label;
    }

    private static LinearLayout.LayoutParams labelParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.gravity = android.view.Gravity.CENTER_VERTICAL;
        return params;
    }

    private static LinearLayout.LayoutParams spinnerParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f);
        params.gravity = android.view.Gravity.CENTER_VERTICAL;
        return params;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }
}
