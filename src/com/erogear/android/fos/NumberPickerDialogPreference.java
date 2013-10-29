package com.erogear.android.fos;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

public class NumberPickerDialogPreference extends DialogPreference {
	private static final int DEFAULT_VALUE = 30;
	
	private int mMinValue;
	private int mMaxValue;
	private int mValue;
	private String[] mValues;
	private NumberPicker mNumberPicker;
	
	public NumberPickerDialogPreference(Context context) {
		this(context, null);
	}
	
	public NumberPickerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Set up values
		mValue = NumberPickerDialogPreference.DEFAULT_VALUE;
		mValues = context.getResources().getStringArray(R.array.valuesFrameRate);
		
		// Set layout
		setDialogLayoutResource(R.layout.preference_number_picker_dialog);
	}
	 
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		TextView dialogMessageText = (TextView) view.findViewById(R.id.tvDialogMessage);
		dialogMessageText.setText(getDialogMessage());
		
		mNumberPicker = (NumberPicker) view.findViewById(R.id.prefNumberPicker);
		mNumberPicker.setMaxValue(mValues.length - 1);
		mNumberPicker.setDisplayedValues(mValues);
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
	
	public void setValue(int value) {
        value = Math.max(Math.min(value, mMaxValue), mMinValue);
 
        if (value != mValue) {
            mValue = value;
            persistInt(value);
            notifyChanged();
        }
    }
	
}