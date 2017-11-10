package com.lu.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.lu.filemanager2.R;
import com.lu.filemanager2.databinding.PermissionSetMenuBinding;

/**
 * Created by bulefin on 2017/11/9.
 */

public class ViewManager {
    private static ViewManager mViewManager;
    private AlertDialog mPropertyDialog;
    private TextView mPropertyTv[];
    private Object mPermissionSet[];
    private CheckBox mPerCheckBoxs[];

    private ViewManager(){}
    public static ViewManager getInstance() {
        if (mViewManager == null) {
            mViewManager = new ViewManager();
        }
        return mViewManager;
    }

    public TextView[] getPropertyTvArray() {
        if (mPropertyTv != null) {
            return mPropertyTv;
        }
        mPropertyTv = new TextView[8];
        return mPropertyTv;
    }
    public AlertDialog createPropertyDialog(Activity ay, View.OnClickListener listener) {
        if (mPropertyDialog != null) {
            return mPropertyDialog;
        }
        if (mPropertyTv == null) {
            getPropertyTvArray();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ay);
        LinearLayout view = (LinearLayout) ay.getLayoutInflater().inflate(R.layout.fileinfo_property_menu, null);
        view.findViewById(R.id.property_confirm).setOnClickListener(listener);
        int m = 0;
        for (int i = 1; i < view.getChildCount() -1; i++) {
            if (view.getChildAt(i) instanceof TextView) {
                mPropertyTv[m] = (TextView) view.getChildAt(i);
                m++;
            }
        }
        builder.setView(view);
        mPropertyDialog = builder.create();
        return mPropertyDialog;
    }

    public Object[] createPermissionSetDialog(Activity ay, CompoundButton.OnCheckedChangeListener changeListener, View.OnClickListener clickListener) {
        if (mPermissionSet != null) {
            return mPermissionSet;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ay);
        PermissionSetMenuBinding viewBind = DataBindingUtil.inflate(ay.getLayoutInflater(), R.layout.permission_set_menu, null, false);
        View view = viewBind.getRoot();
        TextView perSetFileName = viewBind.permissionSetName;
        TextView permissionOctalValueTextView = viewBind.permissionOctalValue;
        if (mPerCheckBoxs == null) {
            getCheckBoxArray();
        }
        mPerCheckBoxs[0] = viewBind.permissionSetCheckownerRead;
        mPerCheckBoxs[1] = viewBind.permissionSetCheckownerWrite;
        mPerCheckBoxs[2] = viewBind.permissionSetCheckownerExecute;
        mPerCheckBoxs[3] = viewBind.permissionSetCheckusergroupRead;
        mPerCheckBoxs[4] = viewBind.permissionSetCheckusergroupWrite;
        mPerCheckBoxs[5] = viewBind.permissionSetCheckusergroupExecute;
        mPerCheckBoxs[6] = viewBind.permissionSetCheckotherRead;
        mPerCheckBoxs[7] = viewBind.permissionSetCheckotherWrite;
        mPerCheckBoxs[8] = viewBind.permissionSetCheckotherExecute;
        mPerCheckBoxs[9] = viewBind.permissionSetCheckSetuid;
        mPerCheckBoxs[10] = viewBind.permissionSetCheckSetgid;
        mPerCheckBoxs[11] = viewBind.permissionSetCheckSetsticky;
        for (int i = 0; i < mPerCheckBoxs.length; i++) {
            mPerCheckBoxs[i].setOnCheckedChangeListener(changeListener);
        }
        viewBind.permissionCheckApplyChilddirandfile.setOnCheckedChangeListener(changeListener);
        viewBind.permissionCheckNotapplyChildfile.setOnCheckedChangeListener(changeListener);
        viewBind.permissionCancel.setOnClickListener(clickListener);
        viewBind.permissionConfirm.setOnClickListener(clickListener);
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        builder.setView(view);
        Object objects[] = {builder.create(), view.getMeasuredWidth(), perSetFileName, permissionOctalValueTextView};
        mPermissionSet = objects;
        return mPermissionSet;
    }

    public CheckBox[] getCheckBoxArray() {
        if (mPerCheckBoxs != null) {
            return mPerCheckBoxs;
        }
        mPerCheckBoxs = new CheckBox[12];
        return mPerCheckBoxs;
    }
}
