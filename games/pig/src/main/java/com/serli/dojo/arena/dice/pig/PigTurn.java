package com.serli.dojo.arena.dice.pig;

import com.serli.dojo.arena.dice.Die;

public class PigTurn {

	public final PigPlayer player;
	public final Integer turnScore;
	public final Integer turnCount;
	public final Integer dieScore;

	public static PigTurn playing(PigPlayer player) {
		return new PigTurn(player, 0, 1, Die.roll());
	}
	
	public PigTurn scoring() {
		return new PigTurn(player, turnScore + dieScore, turnCount + 1, Die.roll());
	}

	private PigTurn(PigPlayer player, Integer turnScore, Integer turnCount, Integer dieScore) {
		this.player = player;
		this.turnScore = turnScore;
		this.turnCount = turnCount;
		this.dieScore = dieScore;
	}

	@Override
	public String toString() {
		return String.format("%s currently holds %d points at turn %d and rolled %d", player.name, turnScore, turnCount, dieScore);
	}
}