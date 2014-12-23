package com.gustavoparreira.realtimetile;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;

import co.realtime.storage.ItemAttribute;
import co.realtime.storage.ItemRef;
import co.realtime.storage.ItemSnapshot;
import co.realtime.storage.StorageRef;
import co.realtime.storage.TableRef;
import co.realtime.storage.ext.OnError;
import co.realtime.storage.ext.OnItemSnapshot;
import co.realtime.storage.ext.OnReconnected;
import co.realtime.storage.ext.OnReconnecting;
import co.realtime.storage.ext.StorageException;

public class Game extends ActionBarActivity {

    public String gameID, playerID, _nickname;
    LinkedHashMap<String, Player> players;
    public static final int GRIDSIZE = 36;
    public static final int HIDDEN = 0;
    public static final int VISIBLE = 1;
    public static final int LOCKED = 2;
    StorageRef storage;
    TableRef tilesTableRef, gameTableRef;
    StringTokenizer tokens;
    Random random = new Random();
    public Tile[] grid = new Tile[GRIDSIZE];
    Activity _activity;
    CountDownTimer mainTimer = null;
    int firstTile = -1, secondTile = -1;
    boolean readyToPlay = false;
    int tilesSaved = 0;
    OnItemSnapshot onTileUpdateCallback, onPlayerUpdateCallback, onPlayerDeleteCallback;
    ProgressDialog progress;

    private void storageInit() {
        try {
            storage = new StorageRef("[ENTER YOUR REALTIME STORAGE APPKEY]", "token");
        } catch (StorageException e) {
            e.printStackTrace();
        }

        storage.onReconnecting(new OnReconnecting() {
            @Override
            public void run(StorageRef storageRef) {

                _activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        readyToPlay = false;
                        System.out.println("**** RECONNECTING TO STORAGE SERVER ...");
                        Toast.makeText(_activity, "Oops, lost connection. Trying to reconnect", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        storage.onReconnected(new OnReconnected() {
            @Override
            public void run(StorageRef storageRef) {

                _activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        unsubscribeGameUpdates();
                        clearPlay();
                        System.out.println("**** RECONNECTED TO STORAGE SERVER. Reloading the game ...");
                        startProgress("Reconnected. Please wait for game to reload ...");
                        loadPlayers();
                    }
                });
            }
        });

        tilesTableRef = storage.table("Tiles").equals("gameID", new ItemAttribute(gameID));
    }

    private void gridInit() {
        for(int i=0; i < GRIDSIZE; i++) {
            grid[i] = new Tile(gameID, fromLinearPositionToCoordinate(i), HIDDEN, "", playerID, 1);
        }
    }

    public Game(Activity activity, String nickname) {
        _activity = activity;
        _nickname = nickname;

        gameID = MainMenu.gameID;
        playerID = UUID.randomUUID().toString();

        startProgress("Please wait for game to load...");
        storageInit();

        // remove all players

        gameTableRef = storage.table("Game").equals("gameID", new ItemAttribute(gameID));

        gameTableRef.getItems(new OnItemSnapshot() {
                                  @Override
                                  public void run(final ItemSnapshot itemSnapshot) {
                      if (itemSnapshot != null) {
                          String _playerID = itemSnapshot.val().get("playerID").toString();
                          Player player = new Player(storage, gameID, _playerID, "", 0);
                          player.leaveGame();
                      } else {
                          // finished removing players
                          gameInit();
                      }
                  }

                  ;
              },
            new OnError() {
                @Override
                public void run(Integer code, String errorMessage) {
                    System.out.println(String.format("delete all players:: error: %s", errorMessage));
                }
            }
        );

        System.out.println("----- Waiting for game to start");
    }

    private void gameInit() {

        players = new LinkedHashMap<>();

        Player currentPlayer = new Player(storage, gameID, playerID, _nickname, 0);
        currentPlayer.update(null);
        players.put(playerID, currentPlayer);

        gridInit();

        String halfGrid = UUID.randomUUID().toString().toUpperCase();
        halfGrid = halfGrid.replace("-", "");

        generateGridValues(halfGrid.substring(0,GRIDSIZE/2));

        // save generated grid to storage
        for (int i = 0; i < GRIDSIZE; i++) {

            ItemAttribute primaryKey = new ItemAttribute(gameID);
            ItemAttribute secondaryKey = new ItemAttribute(fromLinearPositionToCoordinate(i));

            LinkedHashMap<String, ItemAttribute> tile = new LinkedHashMap<String, ItemAttribute>();
            tile.put("gameID", primaryKey);
            tile.put("coordinate", secondaryKey);
            tile.put("state", new ItemAttribute(grid[i].state));
            tile.put("value", new ItemAttribute(String.valueOf(grid[i].value)));
            tile.put("playerID", new ItemAttribute(playerID));
            tile.put("atomicCounter", new ItemAttribute(1));

            tilesTableRef.push(tile, new OnItemSnapshot() {
                @Override
                public void run(final ItemSnapshot itemSnapshot) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(String.format("New Game constructor:: tile saved %s", itemSnapshot.val().get("coordinate").toString()));
                            tilesSaved++;
                            if(tilesSaved == GRIDSIZE) {
                                System.out.println("----- Game saved. Starting ...");
                                progress.dismiss();
                                generateUI();
                            }
                        }

                    });

                }
            }, new OnError() {
                @Override
                public void run(Integer code, String errorMessage) {
                    System.out.println(String.format("New Game constructor:: error saving tile: %d (%s)", code, errorMessage));
                }
            });
        }
    }

    public Game(Activity activity, String ID, String nickname) {
        _activity = activity;
        _nickname = nickname;

        playerID = UUID.randomUUID().toString();
        gameID = ID;

        hideJoinGameInput();
        startProgress("Please wait for game to load...");
        storageInit();
        gridInit();

        loadPlayers();
    }

    private void loadPlayers() {

        players = new LinkedHashMap<>();
        gameTableRef = storage.table("Game").equals("gameID", new ItemAttribute(gameID));

        gameTableRef.getItems(new OnItemSnapshot() {
                                  @Override
                                  public void run(final ItemSnapshot itemSnapshot) {
                                      if (itemSnapshot != null) {
                                          String _playerID = itemSnapshot.val().get("playerID").toString();
                                          String _playerName = itemSnapshot.val().get("playerName").toString();
                                          int _score = Integer.parseInt(itemSnapshot.val().get("score").toString());

                                          Player player = new Player(storage, gameID, _playerID, _playerName, _score);
                                          players.put(_playerID, player);
                                      } else {
                                          // finished loading players

                                          // add current player to the game if it's not there (it will in case of reconnect)

                                          if(players.get(playerID) == null) {
                                              final Player currentPlayer = new Player(storage, gameID, playerID, _nickname, 0);
                                              players.put(playerID, currentPlayer);

                                              currentPlayer.update(new OnItemSnapshot() {
                                                  @Override
                                                  public void run(ItemSnapshot itemSnapshot) {
                                                      //load the game
                                                      loadGame();
                                                  }
                                              });
                                          } else {
                                              loadGame();
                                          }
                                      }
                                  }

                                  ;
                              },
                new OnError() {
                    @Override
                    public void run(Integer code, String errorMessage) {
                        System.out.println(String.format("Load players:: error: %s", errorMessage));
                    }
                }
        );
    }

    private void loadGame() {

        tilesTableRef = storage.table("Tiles").equals("gameID", new ItemAttribute(gameID));

        // load grid from storage
        tilesTableRef.getItems(new OnItemSnapshot() {
                                   @Override
                                   public void run(final ItemSnapshot itemSnapshot) {

                           runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   if (itemSnapshot != null) {
                                       int position = fromCoordinateToLinearPosition(itemSnapshot.val().get("coordinate").toString());
                                       grid[position].value = itemSnapshot.val().get("value").toString();
                                       grid[position].state = Integer.parseInt(itemSnapshot.val().get("state").toString());
                                       if(itemSnapshot.val().get("playerID") != null)
                                            grid[position].playerID = itemSnapshot.val().get("playerID").toString();
                                   } else {
                                       // grid finished loading, populate UI buttons
                                       progress.dismiss();
                                       generateUI();
                                   }
                               }
                           });
                       }
                   },
                new OnError() {
                    @Override
                    public void run(Integer code, String errorMessage) {
                        System.out.println(String.format("Join Game constructor:: error: %s", errorMessage));
                    }
                });
    }

    private void startProgress(String reason) {
        progress = new ProgressDialog(_activity);
        progress.setTitle("Loading game");
        progress.setMessage(reason);
        progress.setCancelable(false);
        progress.show();
    }

    private void hideJoinGameInput() {
        EditText editText = (EditText)_activity.findViewById(R.id.joinGameEditText);
        Button button = (Button)_activity.findViewById(R.id.joinGameButton);
        ((ViewGroup)editText.getParent()).removeView(editText);
        ((ViewGroup)button.getParent()).removeView(button);
    }

    private void generateGridValues(String gridChars) {

        for (int i = 0; i < GRIDSIZE / 2; i++) {
            grid[i].value = String.valueOf(gridChars.charAt(i));
        }

        for (int i = 0; i < GRIDSIZE / 2 - 1; i++) {
            int position = random.nextInt(gridChars.length() - 1);
            StringBuilder sb = new StringBuilder(gridChars);

            grid[i + GRIDSIZE / 2].value = String.valueOf(gridChars.charAt(position));

            sb.deleteCharAt(position);
            gridChars = sb.toString();
        }
        grid[GRIDSIZE - 1].value = gridChars;
    }

    private void generateUI() {
        RelativeLayout layout = (RelativeLayout) _activity.findViewById(R.id.layout1);

        //Populate player UI
        populatePlayers();

        //Populate buttons
        Button[][] buttonArray = new Button[(int) Math.sqrt(GRIDSIZE)][(int) Math.sqrt(GRIDSIZE)];
        TableLayout table = new TableLayout(_activity);
        RelativeLayout.LayoutParams tableParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        tableParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        WindowManager wm = (WindowManager) _activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = (int) ((size.x / Math.sqrt(GRIDSIZE)) - 30);
        int height = (int) (((size.y * 0.7) / Math.sqrt(GRIDSIZE)) - 30);

        int i = 0;
        for (int row = 0; row < Math.sqrt(GRIDSIZE); row++) {
            TableRow currentRow = new TableRow(_activity);
            for (int button = 0; button < Math.sqrt(GRIDSIZE); button++) {
                final Button currentButton = new Button(_activity);
                TableRow.LayoutParams trParams = new TableRow.LayoutParams(width, height);
                trParams.leftMargin = 0;
                // you could initialize them here
                currentButton.setText(String.valueOf(grid[i].value));
                currentButton.setTextColor(Color.TRANSPARENT);
                currentButton.setId(i);
                currentButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                                if(readyToPlay) {
                                    checkPlayValidity(v);
                                }
                            }
                });
                // you can store them
                buttonArray[row][button] = currentButton;
                // and you have to add them to the TableRow
                currentRow.addView(currentButton, trParams);
                i++;
            }
            // a new row has been constructed -> add to table
            table.addView(currentRow);
        }
        // and finally takes that new table and add it to your layout.
        layout.addView(table, tableParams);

        // update tile buttons state
        for(i=0; i < GRIDSIZE; i++) {
            updateTileButtonUI(i);
        }

        subscribeGameUpdates();

        Toast.makeText(_activity, "You can play now. Good luck!", Toast.LENGTH_LONG).show();
        readyToPlay = true;
    }

    private void populatePlayers() {
        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView[] playerUI = new TextView[4];

                playerUI[0] = (TextView)_activity.findViewById(R.id.player1);
                playerUI[1] = (TextView)_activity.findViewById(R.id.player2);
                playerUI[2] = (TextView)_activity.findViewById(R.id.player3);
                playerUI[3] = (TextView)_activity.findViewById(R.id.player4);

                // clear player UI from previous populates
                for(int i=0; i<4; i++) {
                    playerUI[i].setText("");
                    playerUI[i].setVisibility(View.INVISIBLE);
                }

                int playerNumber = 0;
                for (LinkedHashMap.Entry<String, Player> p: players.entrySet()) {
                    if (playerNumber < 4) {
                        Player player = p.getValue();
                        playerUI[playerNumber].setText(player.name + ": " + player.score);
                        playerUI[playerNumber].setVisibility(View.VISIBLE);
                        playerNumber++;
                    }
                }
            }
        });
    }

    private void removePlayer(final String _playerID) {
        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                TextView[] playerUI = new TextView[4];

                playerUI[0] = (TextView)_activity.findViewById(R.id.player1);
                playerUI[1] = (TextView)_activity.findViewById(R.id.player2);
                playerUI[2] = (TextView)_activity.findViewById(R.id.player3);
                playerUI[3] = (TextView)_activity.findViewById(R.id.player4);

                int playerNumber = 0;
                for (LinkedHashMap.Entry<String, Player> p: players.entrySet()) {
                    if (playerNumber < 4) {
                        Player player = p.getValue();
                        if(player.playerID.equals(_playerID)) {
                            playerUI[playerNumber].setText("");
                            playerUI[playerNumber].setVisibility(View.INVISIBLE);
                        }
                        playerNumber++;
                    }
                }

                players.remove(_playerID);
                populatePlayers();
            }
        });
    }

    private String fromLinearPositionToCoordinate(int position) {
        int x = (int) (position / Math.sqrt((double) GRIDSIZE));
        int y = (int) (position % Math.sqrt((double) GRIDSIZE));
        return x + "," + y;
    }

    private int fromCoordinateToLinearPosition(String coordinate) {
        int x, y;
        tokens = new StringTokenizer(coordinate, ",");
        x = Integer.parseInt(tokens.nextToken());
        y = Integer.parseInt(tokens.nextToken());
        return (int) (x * Math.sqrt((double) GRIDSIZE) + y);
    }

    private void checkPlayValidity(View v) {

        final int position = v.getId();

        System.out.println(String.format("checkPlayValidity:: %d %d %d", firstTile, secondTile, position));

        // we don't care if the tile is already visible or locked, if it's already selected and if secondTile is already set

        if(grid[position].state == HIDDEN && firstTile != position & secondTile == -1) {

            readyToPlay = false;

            showWaitingTileUI(position);

            ItemAttribute primaryKey = new ItemAttribute(gameID);
            final ItemAttribute secondaryKey = new ItemAttribute(fromLinearPositionToCoordinate(position));

            // atomic decrement to check if player was the first to select tile

            final ItemRef tile = tilesTableRef.item(primaryKey, secondaryKey);

            tile.decr("atomicCounter", 1, new OnItemSnapshot() {
                @Override
                public void run(final ItemSnapshot itemSnapshot) {

                    int atomicCounter =  Integer.parseInt(itemSnapshot.val().get("atomicCounter").toString());
                    int state = Integer.parseInt(itemSnapshot.val().get("state").toString());

                    if (atomicCounter == 0 && state == HIDDEN)  {

                        // user won the selection
                        // broadcast the tile turn to other players

                        System.out.println(String.format("checkPlayValidity:: make visible position = %d", position));

                        LinkedHashMap<String, ItemAttribute> tileProperties = new LinkedHashMap<String, ItemAttribute>();
                        //tileProperties.put("atomicCounter", new ItemAttribute(1));
                        tileProperties.put("state", new ItemAttribute(VISIBLE));
                        tileProperties.put("playerID", new ItemAttribute(playerID));
                        tile.set(tileProperties,
                                new OnItemSnapshot() {
                                    @Override
                                    public void run(final ItemSnapshot itemSnapshot) {

                                        if(firstTile == -1){
                                            firstTile = position;
                                        } else {
                                            secondTile = position;
                                        }

                                        // show the tile
                                        grid[position].state = VISIBLE;
                                        grid[position].playerID = playerID;
                                        updateTileButtonUI(position);

                                        // proceed to play scoring (e.g. matching, scores, ...)
                                        processPlay(fromCoordinateToLinearPosition(itemSnapshot.val().get("coordinate").toString()));
                                    }
                                },
                                new OnError() {
                                    @Override
                                    public void run(Integer integer, String s) {
                                        System.out.println(String.format("checkPlayValidity:: error reseting counter: %s", s));

                                        // oops, clear play otherwise the game is blocked :/
                                        clearPlay();
                                    }
                                });
                    } else {
                        // player missed the play
                        // play buzzer
                        System.out.println(String.format("checkPlayValidity:: player missed selection"));
                        readyToPlay = true;
                    }
                }
            }, new OnError() {
                @Override
                public void run(Integer integer, String s) {
                    System.out.println(String.format("checkPlayValidity:: error decrementing counter: %s", s));
                    clearPlay();
                }
            });
        }
    }


    // this function is only called when the player won the tile selection

    private void processPlay(final int position) {

        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                System.out.println(String.format("processPlay:: position=%d coordinate=%s", position, fromLinearPositionToCoordinate(position)));

                if(tilesMatch()) {

                    // user wins
                    increaseScore(10);

                    // Lock both tiles
                    lockTiles();

                    if(allTilesTurned()) {
                        gameOver();
                    }

                } else {

                    // no match

                    if(secondTile == -1) {
                        // first tile was turned
                        // give time to the player to find the matching tile
                        hideTiles(3);
                        readyToPlay = true;
                    } else {
                        // miss

                        // hide both tiles after 300ms
                        decreaseScore(1);
                        hideTiles(0.3);
                    }
                }

            }
        });

    }

    private void increaseScore(int points) {
        Player player = players.get(playerID);
        player.score += points;
        player.update(null);
        System.out.println("Score: " + player.score);
    }

    private void decreaseScore(int points) {
        Player player = players.get(playerID);
        if (player.score > 0) {
            player.score -= points;
            player.update(null);
        }
        System.out.println("Score: " + player.score);
    }

    private boolean tilesMatch() {
        if(firstTile != 1 && secondTile != -1) {
            return (grid[firstTile].value.equals(grid[secondTile].value));
        } else  {
            return false;
        }
    }

    private boolean allTilesTurned() {
        for (int i = 0; i < GRIDSIZE; i++) {
            if(grid[i].state == HIDDEN) {
                return false;
            }
        }

        return true;
    }

    private void hideTiles(double timeInSeconds) {

        if (mainTimer != null) {
            mainTimer.cancel();
        }

        mainTimer = new CountDownTimer((int)(timeInSeconds * 1000), 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                System.out.println(String.format("mainTimer.onFinish %d %d", firstTile, secondTile));
                clearPlay();
            }
        };

        mainTimer.start();
    }

    private void clearPlay() {

        System.out.println(String.format("clearPlay %d %d", firstTile, secondTile));

        if(mainTimer != null) {
            mainTimer.cancel();
            mainTimer = null;
        }

        // hide the tile if it isn't locked

        if (firstTile != -1 && grid[firstTile].state != LOCKED) {

            grid[firstTile].state = HIDDEN;
            grid[firstTile].playerID = playerID;
            grid[firstTile].atomicCounter = 1;

            grid[firstTile].update(tilesTableRef,
                    new OnItemSnapshot() {
                        @Override
                        public void run(ItemSnapshot itemSnapshot) {

                            // first tile finished updating
                            // hide the second tile if it isn't locked and the player is owner

                            if (secondTile != -1 && grid[secondTile].state != LOCKED) {

                                grid[secondTile].state = HIDDEN;
                                grid[secondTile].playerID = playerID;
                                grid[secondTile].atomicCounter = 1;

                                grid[secondTile].update(tilesTableRef,
                                        new OnItemSnapshot() {
                                            @Override
                                            public void run(ItemSnapshot itemSnapshot) {

                                                // hide both tiles at the same time for better UX

                                                if(firstTile != -1)
                                                    updateTileButtonUI(firstTile);

                                                if(secondTile != -1)
                                                    updateTileButtonUI(secondTile);

                                                // second tile finished updating, clear the play
                                                firstTile = -1;
                                                secondTile = -1;
                                                readyToPlay = true;

                                            }
                                        });
                            } else {
                                // just hide the first tile
                                if(firstTile != -1) {
                                    grid[firstTile].state = HIDDEN;
                                    grid[firstTile].playerID = playerID;
                                    updateTileButtonUI(firstTile);
                                }

                                firstTile = -1;
                                secondTile = -1;
                                readyToPlay = true;
                            }
                        }
                    });
        } else {
            // there's no first tile, just clear the play
            firstTile = -1;
            secondTile = -1;
            readyToPlay = true;
        }
    }

    private void lockTiles() {

        System.out.println(String.format("locking tiles %d %d", firstTile, secondTile));

        // cancel main local timer
        if(mainTimer != null) {
            mainTimer.cancel();
            mainTimer = null;
        }

        //lock the matching tiles

        grid[firstTile].state = LOCKED;
        grid[firstTile].playerID = playerID;

        grid[firstTile].update(tilesTableRef, new OnItemSnapshot() {

            @Override
            public void run(ItemSnapshot itemSnapshot) {

                grid[secondTile].state = LOCKED;
                grid[secondTile].playerID = playerID;

                grid[secondTile].update(tilesTableRef, new OnItemSnapshot() {

                    @Override
                    public void run(ItemSnapshot itemSnapshot) {

                        updateTileButtonUI(firstTile);
                        updateTileButtonUI(secondTile);

                        // reset play
                        firstTile = -1;
                        secondTile = -1;
                        readyToPlay = true;
                    }
                });
            }
        });
    }

    public void updateTileButtonUI(final int position) {

        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(position != -1) {

                    Boolean isCurrentPlayer = grid[position].playerID.equals(playerID);

                    Button button = (Button) _activity.findViewById(position);
                    button.setText(grid[position].value);

                    switch(grid[position].state) {

                        case HIDDEN:

                            button.setTextColor(Color.TRANSPARENT);
                            break;

                        case VISIBLE:

                            if(isCurrentPlayer) {
                                button.setTextColor(Color.BLACK);
                            } else {
                                button.setTextColor(Color.RED);
                            }
                            break;

                        case LOCKED:

                            button.setTextColor(Color.BLACK);

                            if(isCurrentPlayer) {
                                button.setBackgroundColor(Color.GREEN);
                            } else {
                                button.setBackgroundColor(Color.RED);
                            }

                            break;
                    }
                }
            }
        });
    }

    public void showWaitingTileUI(int position) {
        Button button = (Button) _activity.findViewById(position);
        button.setTextColor(Color.BLACK);
        button.setText("\u231B");
    }

    // Receive the update changes from other players

    private void subscribeGameUpdates() {

        // handle game updates

        onTileUpdateCallback = new OnItemSnapshot() {

            @Override
            public void run(final ItemSnapshot itemSnapshot) {

                System.out.println(String.format("Game updated: %s", itemSnapshot.val()));

                if (itemSnapshot.val().get("playerID") != null) {
                    String updatePlayer = itemSnapshot.val().get("playerID").toString();

                    // update is from another player
                    if (!updatePlayer.equals(playerID)) {
                        int atomicCounter = Integer.parseInt(itemSnapshot.val().get("atomicCounter").toString());
                        final int state = Integer.parseInt(itemSnapshot.val().get("state").toString());

                        if(atomicCounter >= 0) {
                            // we don't care for atomic decrements updates that didn't win the play, negative values

                            String coordinate = itemSnapshot.val().get("coordinate").toString();

                            final int position = fromCoordinateToLinearPosition(coordinate);
                            grid[position].state = state;
                            grid[position].playerID = updatePlayer;
                            updateTileButtonUI(position);

                            if(allTilesTurned()) {
                                gameOver();
                            }
                        }
                    }
                }
            }
        };

        tilesTableRef.on(StorageRef.StorageEvent.UPDATE, new ItemAttribute(gameID), onTileUpdateCallback);


        // handle score updates

        onPlayerUpdateCallback = new OnItemSnapshot() {

            @Override
            public void run(final ItemSnapshot itemSnapshot) {

                System.out.println(String.format("Player updated: %s", itemSnapshot.val()));
                String _playerID = itemSnapshot.val().get("playerID").toString();
                String _name = itemSnapshot.val().get("playerName").toString();
                int _score = Integer.parseInt(itemSnapshot.val().get("score").toString());

                Player player = players.get(_playerID);

                if(player != null) {
                    player.score = _score;
                } else {
                    Player newPlayer = new Player(storage, gameID, _playerID, _name, _score);
                    players.put(_playerID, newPlayer);
                }

                populatePlayers();
            }
        };

        gameTableRef.on(StorageRef.StorageEvent.UPDATE, new ItemAttribute(gameID), onPlayerUpdateCallback);

        // handle player leaving

        onPlayerDeleteCallback = new OnItemSnapshot() {
            @Override
            public void run(ItemSnapshot itemSnapshot) {
                System.out.println(String.format("Player deleted: %s", itemSnapshot.val()));
                String _playerID = itemSnapshot.val().get("playerID").toString();
                removePlayer(_playerID);
            }
        };

        gameTableRef.on(StorageRef.StorageEvent.DELETE, new ItemAttribute(gameID), onPlayerDeleteCallback);
    }
    private void unsubscribeGameUpdates() {
        tilesTableRef.off(StorageRef.StorageEvent.UPDATE, new ItemAttribute(gameID), onTileUpdateCallback);
        gameTableRef.off(StorageRef.StorageEvent.UPDATE, new ItemAttribute(gameID), onPlayerUpdateCallback);
        gameTableRef.off(StorageRef.StorageEvent.DELETE, new ItemAttribute(gameID), onPlayerDeleteCallback);
    }

    public void close() {
        unsubscribeGameUpdates();
        players.get(playerID).leaveGame();
    }

    private void gameOver() {

        _activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                int highScore = 0;
                String winnerPlayerID = "";

                for (LinkedHashMap.Entry<String, Player> p: players.entrySet()) {
                    Player player = p.getValue();
                    if(player.score > highScore) {
                        winnerPlayerID = player.playerID;
                        highScore = player.score;
                    }
                }

                if(playerID.equals(winnerPlayerID)) {
                    Toast.makeText(_activity, "You beat game, gud gud", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(_activity, "I guess you've taken a beating", Toast.LENGTH_LONG).show();
                }

                readyToPlay = false;

            }
        });
    }

}
