package com.gustavoparreira.realtimetile;

import java.util.LinkedHashMap;

import co.realtime.storage.ItemAttribute;
import co.realtime.storage.ItemSnapshot;
import co.realtime.storage.TableRef;
import co.realtime.storage.ext.OnError;
import co.realtime.storage.ext.OnItemSnapshot;

public class Tile {
    int state;
    String value, gameID, coordinate, playerID;
    int atomicCounter;

    public  Tile() {
        gameID = "";
        coordinate = "";
        state = 0;
        value = "";
        playerID = "";
        atomicCounter = 0;
    }

    public Tile(String _gameID, String _coordinate, int _state, String _value, String _playerID, int _atomicCounter) {
        gameID = _gameID;
        coordinate = _coordinate;
        state = _state;
        value = _value;
        playerID = _playerID;
        atomicCounter = _atomicCounter;
    }

    public void update(TableRef tableRef, final OnItemSnapshot onFinished) {

        System.out.println(String.format("Tile.update state=%d coordinate=%s", state, coordinate));

        ItemAttribute primaryKey = new ItemAttribute(gameID);
        ItemAttribute secondaryKey = new ItemAttribute(coordinate);

        OnError onError = new OnError() {
            @Override
            public void run(Integer integer, String s) {
                // we really should try to recover from this error, like retrying,
                // but we are too lazy atm ...
                if(onFinished != null)
                    onFinished.run(null);
            }
        };

        LinkedHashMap<String, ItemAttribute> tile = new LinkedHashMap<String, ItemAttribute>();
        tile.put("state", new ItemAttribute(state));
        tile.put("playerID", new ItemAttribute(playerID));

        if(atomicCounter == 1)
            tile.put("atomicCounter", new ItemAttribute(atomicCounter));

        tableRef.item(primaryKey, secondaryKey).set(tile, onFinished, onError);
    }

}
