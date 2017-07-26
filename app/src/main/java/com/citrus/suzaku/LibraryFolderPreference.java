package com.citrus.suzaku;
//
// Created by safu9 on 2017/06/12

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

public class LibraryFolderPreference extends DialogPreference
{
    private List<String> paths = new ArrayList<>();

    public LibraryFolderPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index)
    {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue)
    {
        String value;
        if (restoreValue){
            if (defaultValue == null){
                value = getPersistedString("");
            }else{
                value = getPersistedString(defaultValue.toString());
            }
        }else{
            value = defaultValue.toString();
        }

        paths = PreferenceUtils.toStringList(value);
    }

    public void persistStringListValue(List<String> paths)
    {
        persistString(PreferenceUtils.toJsonString(paths));
    }

    public List<String> getStringListValue()
    {
        List<String> clone = new ArrayList<>();
        clone.addAll(paths);
        return clone;
    }

}
