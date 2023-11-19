package com.wintership.bettingDataProcessor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.wintership.bettingDataProcessor.DataFileReader.logger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    private UUID playerId;
    private long balance;
    private List<PlayerAction> actions;
    private Set<PlayerAction> processedActions = new HashSet<>();
    private long netAmount;
    private int wonMatches = 0;
    private int totalMatches = 0;
    private boolean firstIllegalOperationDetected = false;
    private PlayerAction firstIllegalOperation;


    public boolean hasProcessedAction(PlayerAction action) {
        return processedActions.contains(action);
    }

    public Player(UUID playerId) {
        this.playerId = playerId;
        this.balance = 0;
        this.actions = new ArrayList<>();
        this.netAmount = 0;
    }

    public void addAction(PlayerAction action) {
        actions.add(action);
        updateBalance(action);
    }
    public void updateNetAmount(long amount) {
        this.netAmount += amount;
    }
    public void updateBalance(long amount) {
        balance += amount;
    }

    public void updateBalance(PlayerAction action) {
        try {
            switch (action.getAction()) {
                case "DEPOSIT":
                    balance += action.getCoinsAmount();
                    break;
                case "BET", "WITHDRAW":
                    balance -= action.getCoinsAmount();
                    break;
            }
        } catch (Exception e) {
            logger.error("Error updating balance: " + e.getMessage());
        }
    }

    public void setFirstIllegalOperation(PlayerAction operationDetails) {
        if (!firstIllegalOperationDetected) {
            this.firstIllegalOperation = operationDetails;
            this.firstIllegalOperationDetected = true;
        }
    }

    public boolean hasFirstIllegalOperation() {
        return firstIllegalOperationDetected;
    }

    public void incrementWonMatches() {
        wonMatches++;
    }

    public void incrementTotalMatches() {
        totalMatches++;
    }

    public BigDecimal getWinRate() {
        if (totalMatches == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal winRate = new BigDecimal(wonMatches).divide(new BigDecimal(totalMatches), 4, RoundingMode.HALF_UP);

        return winRate.setScale(2, RoundingMode.HALF_UP);
    }

}