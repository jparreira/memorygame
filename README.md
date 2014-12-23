
## A multi-player memory game for Android using Realtime Cloud Storage atomic counters ##

A few weeks ago I wrote about [using atomic counters to implement mutual exclusive actions](https://medium.com/@jparreira/concurrent-mutual-exclusive-actions-using-atomic-counters-7dca3bbd0f1d) in a high concurrency scenario, like an online multi-player game.

With the help of my eldest son, [@TheGustash](https://twitter.com/TheGustash), we've decided to write a simple multi-player Android game that could illustrate in practical terms the strategy defined in my previous post. 

For the data backend we selected the Realtime Cloud Storage service. Powered by the AWS DynamoDB database, it has the atomic counters with the added goodies of real-time data-sync between players to make it more fun.

## The game concept ##

The game concept is simple: several players use their Android devices to concurrently find the hidden 18 alphanumeric pairs in a 6x6 grid. 

When players select a tile in the grid, the atomic counters are used to find if they won the selection. When they successfully select a tile, the tile content is revealed to all players and the player that selected it has 3 seconds to select a second tile that contains the same content. If they make a pair they win 10 points and the pair of tiles becomes blocked and permanently visible. A failed attempt to finding a pair will decrement 1 point to the current player score. 

Through the use of Realtime Cloud Storage data-sync feature the players actions are synchronized in real-time and if you are really good and have a great memory you’ll benefit from the other player’s tile selections and not just yours.

## Using the atomic counter to resolve conflicts  ##


Each tile of grid has a counter initialized to the value 1. Whenever players select a tile they subtract the value 1 (one) to the counter and wait for the data store reply. If the reply contains a counter value of 0 (zero) than this means the player was the first one to successfully decrement the counter granting him or her the tile ownership for the moment. Due to the atomic properties of the counter all other players that have selected the same tile will have replies with negative values in the counter, so they know they have not conquered the tile and should try another one. The tile conqueror will now be responsible for handling the game logic for that tile. 

From a development point a view it’s a binary semaphore lock, since in every other device the transactions over that particular tile are “locked” until the winning player resets the counter (thus releasing the semaphore). The cool part is that no one is really blocked in the process, they can continue playing selecting other non-locked tiles.

## Running the game  ##

To compile and run it in your device you‘ll need Android Studio 1.0 and a [Realtime Cloud Storage subscription](https://accounts.realtime.co/signup/).

Using the Realtime Cloud Storage console create two tables with the following key schema:

   	Game
    The table Game will store the players in each game 
	along with their individual score.
    
    Primary key: gameID, String
    Secondary key: playerID, String
    
    Tiles
    The table Tiles will store the game tiles (the grid) 
	where each tile is identified by it’s gameID
	and position in the grid (an x,y coordinate).
    
    Primary key: gameID, String
    Secondary key: coordinate, String

If you prefer you can download the .apk file and install it manually in your Android device.

You can play alone in single-mode or invite friends to play with you by sending them your gameID (a 10 digit string that uniquely identifies your game). You can play against a maximum of three friends at a time.

## Conclusion 

I just wanted to prove with this simple prototype that you can build concurrent multi-player games and resolve the concurrency conflicts using only client-side code (in this case Java code in an Android device).

Maybe you feel motivated to build a JavaScript or iOS version. If you keep the same table schema we’ve used in this example, Android players could play against web and iOS players. A true cross-platform memory map game.

Let me know if you’re up to it ... OH and if you find any improvement opportunity (i'm sure there's plenty) just add a pull request.

Have fun and don't eat too much cheese ;)