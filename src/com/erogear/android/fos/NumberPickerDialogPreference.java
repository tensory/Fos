package com.erogear.android.fos;

import java.util.Arrays;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

public class NumberPickerDialogPreference extends DialogPreference {
	private static final int DEFAULT_VALUE = 30;
	private int mValue;
	private int mDefaultValueIndex;
	private String[] mValues;
	private NumberPicker mNumberPicker;
	
	public NumberPickerDialogPreference(Context context) {
		this(context, null);
	}
	
	public NumberPickerDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// Set up values
		mValues = context.getResources().getStringArray(R.array.valuesFrameRate);	
		for (int i = 0; i < mValues.length; i++) {
			if (NumberPickerDialogPreference.DEFAULT_VALUE == Integer.valueOf(mValues[i])) {
				mDefaultValueIndex = i;
			}
		}
		
		// Set layout
		setDialogLayoutResource(R.layout.preference_number_picker_dialog);
	}
	 
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		
		TextView dialogMessageText = (TextView) view.findViewById(R.id.tvDialogMessage);
		dialogMessageText.setText(getDialogMessage());
		
		mNumberPicker = (NumberPicker) view.findViewById(R.id.prefNumberPicker);
		mNumberPicker.setMinValue(0);
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
			setValue(numberPickerValue);
		}
		
		Log.e("PREFERENCE_STORED", "index stored : " + getPersistedInt(mDefaultValueIndex));
	}
	
	@Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
		if (restore) {
			setValue(getPersistedInt(mDefaultValueIndex));
		} else {
			setValue(mDefaultValueIndex);
		}
    }
	
	
	
	@Override
	protected Object onGetDefaultValue(TypedArray array, int index) {
		Log.d("PREFERENCE", "onGetDefaultValue " + index);
		return array.getInt(index, NumberPickerDialogPreference.DEFAULT_VALUE);
	}
	
	@Override
	  public void onDismiss(DialogInterface dialog) {
	    mValue = mNumberPicker.getValue();
	    super.onDismiss(dialog);
	  }
	
	/**
	 * @param value The integer array index of the value to store.
	 */
	public void setValue(int value) {
		mValue = value;	
		
		persistInt(mValue);
        notifyChanged();
    }

}