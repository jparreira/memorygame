package com.gustavoparreira.realtimetile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;


public class JoinGame extends ActionBarActivity {

    private Game game;
    Intent intent;
    SharedPreferences sp;
    boolean gameStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_game);
        intent = getIntent();

        sp = getSharedPreferences("values", Context.MODE_PRIVATE);
        EditText input = (EditText) findViewById(R.id.joinGameEditText);
        input.setText(sp.getString("lastPlayed", ""));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameStarted) {
            game.close();
            gameStarted = false;
        }
    }

    public void start(View view) {
        EditText editText = (EditText)findViewById(R.id.joinGameEditText);
        String gameID = editText.getText().toString();
        String nickame = sp.getString("nickname", "");
        sp.edit().putString("lastPlayed", gameID).apply();

        game = new Game(this, gameID, nickame);
        gameStarted = true;
    }
}
