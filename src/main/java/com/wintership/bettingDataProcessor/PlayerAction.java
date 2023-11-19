package com.wintership.bettingDataProcessor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerAction {
    private UUID playerId;
    private String action;
    private UUID matchId;
    private int coinsAmount;
    private String sideBetOn;
    private List<Match> matches;
    private String line;


    public PlayerAction(String action, UUID matchId, int coinsAmount, String sideBetOn, List<Match> matches, UUID playerId) {
        this.action = action;
        this.matchId = matchId;
        this.coinsAmount = coinsAmount;
        this.sideBetOn = sideBetOn;
        this.matches = matches;
        this.playerId = playerId;
    }

}
