package com.serli.dojo.arena.dice.poker;

public class PokerPlayerHolding extends PokerPlayer {

	public PokerPlayerHolding(String name) {
		super(name);
	}

	@Override
	public PokerAction play(PokerMatch match) {
		return PokerAction.hold();
	}

}
