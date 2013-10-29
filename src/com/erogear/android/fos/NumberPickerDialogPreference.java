package com.erogear.android.fos;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

public class NumberPickerDialogPreference extends DialogPreference {
	private static final int DEFAULT_MIN_VALUE = 1;
	private static final int DEFAULT_MAX_VALUE = 100;
	private static final int DEFAULT_VALUE = 30;
	
	private int mMinValue;
	private int mMaxValue;
	private int mValue;
	private NumberPicker mNumberPicker;
	
	public NumberPickerDialogPreference(Context context) {
		this(context, null);
	}
	
	public NumberPickerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.NumberPickerDialogPreference, 0, 0);
		
		try {
			setMinValue(array.getInteger(R.styleable.NumberPickerDialogPreference_min, DEFAULT_MIN_VALUE));
			setMaxValue(array.getInteger(R.styleable.NumberPickerDialogPreference_android_max, DEFAULT_MAX_VALUE));
		} finally {
			array.recycle();
		}
		
		// set layout   

		setDialogLayoutResource(R.layout.preference_number_picker_dialog);
		
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		TextView dialogMessageText = (TextView) view.findViewById(R.id.tvDialogMessage);
		dialogMessageText.setText(getDialogMessage());
		
		mNumberPicker = (NumberPicker) view.findViewById(R.id.prefNumberPicker);
		mNumberPicker.setMinValue(mMinValue);
		mNumberPicker.setMaxValue(mMaxValue);
		mNumberPicker.setValue(mValue);
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		// when the user selects OK, persist the new value
		if (positiveResult) {
			int numberPickerValue = mNumberPicker.getValue();
			if (callChangeListener(numberPickerValue)) {
				setValue(numberPickerValue);
			}
		}
	}
	
	public void setMinValue(int minValue) {
        mMinValue = minValue;
        setValue(Math.max(mValue, mMinValue));
    }
	
	public void setMaxValue(int maxValue) {
        mMaxValue = maxValue;
        setValue(Math.min(mValue, mMaxValue));
    }
	
	public void setValue(int value) {
        value = Math.max(Math.min(value, mMaxValue), mMinValue);
 
        if (value != mValue) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }
	
}