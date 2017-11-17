package com.lu.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.databinding.DataBindingUtil;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lu.filemanager2.R;
import com.lu.filemanager2.databinding.PermissionSetMenuBinding;

/**
 * Created by bulefin on 2017/11/9.
 */

public class DialogManager {
    private static DialogManager mDialogManager;
    private AlertDialog mPropertyDialog;
    private AlertDialog mTipDialog;
    private TextView mPropertyTv[];
    private Object mPermissionSet[];
    private CheckBox mPerCheckBoxs[];
    private Object mNewFileOrDir[];
    private Object mMsgDialog[];
    private Object mMsgConfirmDialog[];

    private DialogManager(){}
    public static DialogManager get() {
        if (mDialogManager == null) {
            mDialogManager = new DialogManager();
        }
        return mDialogManager;
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

    public AlertDialog createTiPDialog(Activity ay, View.OnClickListener listener) {
        if (mTipDialog != null) {
            return mTipDialog;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(ay);
        LinearLayout view = (LinearLayout) ay.getLayoutInflater().inflate(R.layout.tip_dialog, null);
        view.findViewById(R.id.tip_cancel).setOnClickListener(listener);
        view.findViewById(R.id.tip_confirm).setOnClickListener(listener);
        ((TextView)view.findViewById(R.id.tip_content)).setText(ay.getString(R.string.tip_content));
        builder.setView(view);
        mTipDialog = builder.create();
        return mTipDialog;
    }

    public Object[] createNewFileOrDirDialog(Activity ay, View.OnClickListener listener) {
        if (mNewFileOrDir != null) {
            return mNewFileOrDir;
        }
        mNewFileOrDir = new Object[3];
        AlertDialog.Builder builder = new AlertDialog.Builder(ay);
        LinearLayout view = (LinearLayout) ay.getLayoutInflater().inflate(R.layout.new_filedir_dialog, null);
        view.findViewById(R.id.newfiledir_cancel).setOnClickListener(listener);
        view.findViewById(R.id.newfiledir_confirm).setOnClickListener(listener);
        builder.setView(view);
        mNewFileOrDir[0] = builder.create();
        mNewFileOrDir[1] = view.getChildAt(0);
        mNewFileOrDir[2] = view.getChildAt(3);
        return mNewFileOrDir;
    }

    public Object[] getMsgDialog(Activity ay, View.OnClickListener listener) {
        if (mMsgDialog != null) {
            return mMsgDialog;
        }
        mMsgDialog = new Object[2];
        AlertDialog.Builder builder = new AlertDialog.Builder(ay);
        LinearLayout view = (LinearLayout) ay.getLayoutInflater().inflate(R.layout.msg_dialog, null);
        view.getChildAt(view.getChildCount() -1).setOnClickListener(listener);
        builder.setView(view);
        mMsgDialog[0] = builder.create();
        mMsgDialog[1] = view.getChildAt(2);
        return mMsgDialog;
    }

    public Object[] getMsgConfirmDialog(Activity ay, View.OnClickListener listener) {
        if (mMsgConfirmDialog != null) {
            return mMsgConfirmDialog;
        }
        mMsgConfirmDialog = new Object[3];
        AlertDialog.Builder builder = new AlertDialog.Builder(ay);
        LinearLayout view = (LinearLayout) ay.getLayoutInflater().inflate(R.layout.msg_confirm_dialog, null);
        LinearLayout ll = (LinearLayout) view.getChildAt(view.getChildCount() -1);
        ll.getChildAt(0).setOnClickListener(listener);
        ll.getChildAt(2).setOnClickListener(listener);
        builder.setView(view);
        mMsgConfirmDialog[0] = builder.create();
        mMsgConfirmDialog[1] = view.getChildAt(0);
        mMsgConfirmDialog[2] = view.getChildAt(2);
        return mMsgConfirmDialog;
    }

    public static void onDestroy() {
        mDialogManager = null;
    }
}