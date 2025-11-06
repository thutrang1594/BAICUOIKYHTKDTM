package vn.tlu.cse.ht2.nhom16.moneymanagementapp.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

public class CustomVietnameseEditText extends TextInputEditText {

    public CustomVietnameseEditText(@NonNull Context context) {
        super(context);
    }

    public CustomVietnameseEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVietnameseEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * This is the magic part. By overriding this method, we prevent the EditText
     * from having a buggy behavior with some Vietnamese keyboards.
     * The key is to remove the 'IME_FLAG_NO_FULLSCREEN' flag.
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection conn = super.onCreateInputConnection(outAttrs);
        if (conn != null) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_FULLSCREEN;
        }
        return conn;
    }
}
