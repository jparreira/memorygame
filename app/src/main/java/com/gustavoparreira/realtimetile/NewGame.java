package com.gustavoparreira.realtimetile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;


public class NewGame extends ActionBarActivity {

    Game game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);
        SharedPreferences sp = getSharedPreferences("values", Context.MODE_PRIVATE);
        String nickname = sp.getString("nickname", "");

        game = new Game(this, nickname);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        game.close();
    }
}
