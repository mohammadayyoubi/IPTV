package com.example.iptv.ui;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.iptv.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Material dialog with one or two text fields (dialog_input.xml). */
public final class InputDialog {

    public interface OnSubmit {
        void onSubmit(String first, String second);
    }

    private final Context context;
    private CharSequence title;
    private @DrawableRes int icon;
    private String hint1, value1, helper1;
    private int inputType1 = InputType.TYPE_CLASS_TEXT;
    private String hint2, value2;
    private String positive;
    private OnSubmit onSubmit;

    public InputDialog(Context context) {
        this.context = context;
        this.positive = context.getString(R.string.save);
    }

    public InputDialog title(CharSequence title) { this.title = title; return this; }
    public InputDialog icon(@DrawableRes int icon) { this.icon = icon; return this; }
    public InputDialog positive(String positive) { this.positive = positive; return this; }

    public InputDialog field(String hint, @Nullable String value) {
        this.hint1 = hint;
        this.value1 = value;
        return this;
    }

    public InputDialog fieldHelper(String helper) { this.helper1 = helper; return this; }
    public InputDialog fieldInputType(int inputType) { this.inputType1 = inputType; return this; }

    public InputDialog secondField(String hint, @Nullable String value) {
        this.hint2 = hint;
        this.value2 = value;
        return this;
    }

    public InputDialog onSubmit(OnSubmit onSubmit) { this.onSubmit = onSubmit; return this; }

    public AlertDialog show() {
        View body = LayoutInflater.from(context).inflate(R.layout.dialog_input, null, false);
        TextInputLayout layout1 = body.findViewById(R.id.inputLayout1);
        TextInputEditText input1 = body.findViewById(R.id.input1);
        TextInputLayout layout2 = body.findViewById(R.id.inputLayout2);
        TextInputEditText input2 = body.findViewById(R.id.input2);

        layout1.setHint(hint1);
        if (helper1 != null) layout1.setHelperText(helper1);
        input1.setInputType(inputType1);
        if (value1 != null) input1.setText(value1);

        if (hint2 != null) {
            layout2.setVisibility(View.VISIBLE);
            layout2.setHint(hint2);
            if (value2 != null) input2.setText(value2);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setView(body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(positive, (d, w) -> {
                    if (onSubmit != null) {
                        onSubmit.onSubmit(text(input1), text(input2));
                    }
                });
        if (icon != 0) builder.setIcon(icon);

        AlertDialog dialog = builder.show();
        input1.requestFocus();
        return dialog;
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /** Convenience for confirmation prompts, with a red action for destructive ones. */
    public static void confirm(Context context, CharSequence title, CharSequence message,
                               String action, boolean destructive, Runnable onConfirm) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(destructive ? R.drawable.ic_delete : 0)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(action, (d, w) -> onConfirm.run())
                .show();
        if (destructive) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(context.getColor(R.color.danger));
        }
    }
}
