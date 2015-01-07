package com.gustavoparreira.realtimetile;

import java.util.LinkedHashMap;

import co.realtime.storage.ItemAttribute;
import co.realtime.storage.ItemRef;
import co.realtime.storage.StorageRef;
import co.realtime.storage.TableRef;
import co.realtime.storage.ext.OnError;
import co.realtime.storage.ext.OnItemSnapshot;

public class Player {

    public String gameID;
    public String playerID;
    public String name;
    public int score;
    TableRef gameTableRef;

    public Player(StorageRef storage, String _gameID, String _playerID, String _name, int _score) {
        gameID= _gameID;
        playerID = _playerID;
        name = _name;
        score = _score;
        gameTableRef = storage.table("Game").equals("gameID", new ItemAttribute(gameID));
    }

    public void update(final OnItemSnapshot finish) {

        ItemAttribute primaryKey = new ItemAttribute(gameID);
        ItemAttribute secondaryKey = new ItemAttribute(playerID);

        LinkedHashMap<String, ItemAttribute> playerInfo = new LinkedHashMap<String, ItemAttribute>();
        playerInfo.put("gameID", primaryKey);
        playerInfo.put("playerID", secondaryKey);
        playerInfo.put("playerName", new ItemAttribute(name));
        playerInfo.put("score", new ItemAttribute(score));

        gameTableRef.push(playerInfo, finish, new OnError() {
            @Override
            public void run(Integer code, String errorMessage) {
                System.out.println(String.format("Player update:: error %d (%s)", code, errorMessage));
                if(finish != null) {
                    finish.run(null);
                }
            }
        });
    }

    public void leaveGame() {
        ItemAttribute primaryKey = new ItemAttribute(gameID);
        ItemAttribute secondaryKey = new ItemAttribute(playerID);
        ItemRef player = gameTableRef.item(primaryKey, secondaryKey);
        player.del(null, null);
    }
}