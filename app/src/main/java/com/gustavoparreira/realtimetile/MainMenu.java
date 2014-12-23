package com.gustavoparreira.realtimetile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class MainMenu extends ActionBarActivity {

    AlertDialog alert;
    public static String gameID;
    boolean isFirstLoad;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        sp = getSharedPreferences("values", Context.MODE_PRIVATE);
        isFirstLoad = sp.getString("gameID", "").equals("");

        if (isFirstLoad) {
            changeNickname(null);

            gameID = UUID.randomUUID().toString();
            gameID = gameID.replace("-", "");
            gameID = gameID.substring(0, 10);
            sp.edit().putString("gameID", gameID).apply();
        } else {
            gameID = sp.getString("gameID", "");
        }

        TextView textView = (TextView) findViewById(R.id.gameIDTextView);
        textView.append("\n" + gameID);
    }

    public void newGame(View view) {
        Intent intent = new Intent(this, NewGame.class);
        startActivity(intent);
    }

    public void joinGame(View view) {
        Intent intent = new Intent(this, JoinGame.class);
        startActivity(intent);
    }

    public void changeNickname(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.change_nickname);

        final EditText input = new EditText(this);
        input.setHeight(100);
        input.setWidth(340);
        input.setGravity(Gravity.CENTER);

        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sp.edit().putString("nickname", input.getText().toString()).apply();
                alert.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isFirstLoad) {
                    alert.dismiss();
                    changeNickname(null);
                    Toast.makeText(getApplicationContext(), "Please choose a nickname", Toast.LENGTH_LONG).show();
                } else {
                    alert.dismiss();
                }
            }
        });
        alert = builder.create();
        alert.show();
    }
}
