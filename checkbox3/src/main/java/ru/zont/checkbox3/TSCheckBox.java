package ru.zont.mvc;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.CompoundButton;

public class TSCheckBox extends android.support.v7.widget.AppCompatCheckBox {
    public static final int UNCHECKED = 0;
    public static final int CHECKED= 1;
    public static final int REVCHECKED= 2;

    private int state;
    private OnCheckedChangeListener checkedChangeListener;
    private OnCheckedStateChangeListener checkedStateChangeListener;

    public TSCheckBox(Context context) {
        super(context);
        init();
    }

    public TSCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TSCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        state = UNCHECKED;
        update();
    }

    private void update() {
        switch (state) {
            case CHECKED:
                setButtonDrawable(R.drawable.ic_checkbox_checked);
                break;
            case REVCHECKED:
                setButtonDrawable(R.drawable.ic_checkbox_x);
                break;
            default:
                setButtonDrawable(R.drawable.ic_checkbox_unchecked);
                break;
        }
    }

    @Override
    public boolean isChecked() {
        return state > UNCHECKED;
    }

    @Override
    public void setChecked(boolean checked) {
        setCheckedState(checked ? CHECKED : UNCHECKED);
    }

    @Override
    public void toggle() {
        setCheckedState((state + 1) % 3);
    }

    public int getState() {
        return state;
    }

    public void setCheckedState(int state) {
        if (state < UNCHECKED || state > REVCHECKED) return;

        if (checkedStateChangeListener != null
                && checkedStateChangeListener.onCheckedStateChanged(this, state)) {
            this.state = state;
            update();
        }

        if (checkedChangeListener != null)
            checkedChangeListener.onCheckedChanged(this, isChecked());
    }

    @Override
    public void setOnCheckedChangeListener(@Nullable CompoundButton.OnCheckedChangeListener listener) {
        checkedChangeListener = listener;
    }

    public void setOnCheckedStateChangeListener(@Nullable OnCheckedStateChangeListener listener) {
        checkedStateChangeListener = listener;
    }

    public interface OnCheckedStateChangeListener {
        boolean onCheckedStateChanged(TSCheckBox view, int state);
    }
}
