package com.lu.shell;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.lu.App;
import com.lu.filemanager2.R;

import java.io.IOException;

/**
 * Created by bulefin on 2018/2/24.
 */

public class MainActivity extends Activity {

    private TermSession termSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout ll = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.main_test, null);
        setContentView(ll);
        final EditText editText= (EditText) ll.getChildAt(0);
        ll.getChildAt(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //termSession.write(App.tools+"\n");
                termSession.write((App.tools + " " + editText.getText().toString()).trim() + "\n");
            }
        });
        final EditText editText2= (EditText) ll.getChildAt(2);
        ll.getChildAt(3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //termSession.write(App.tools+"\n");
                termSession.write((editText2.getText().toString()).trim() + "\n");
            }
        });

        App.initTools();
        try {
            termSession = createTermSession();
            termSession.initialize();
            termSession.write("su\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static TermSession createTermSession() throws IOException {
        GenericTermSession session = new ShellTermSession();
        return session;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        termSession.finish();
    }
}
