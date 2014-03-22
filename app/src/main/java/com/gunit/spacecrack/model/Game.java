package com.gunit.spacecrack.model;

/**
 * Created by Dimitri on 27/02/14.
 */

/**
 * All models use public fields because this increases performance.
 */
public class Game {
    public int gameId;
    public String name;
    public int loserPlayerId;
    public Player player1;
    public Player player2;
    public int actionNumber;
}
