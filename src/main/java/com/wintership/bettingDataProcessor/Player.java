package com.wintership.bettingDataProcessor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    private UUID playerId;
    private long balance;
    private List<PlayerAction> actions;
    private Set<PlayerAction> processedActions = new HashSet<>();


    public boolean hasProcessedAction(PlayerAction action) {
        return processedActions.contains(action);
    }

    public Player(UUID playerId) {
        this.playerId = playerId;
        this.balance = 0; // Assuming the balance starts at 0
        this.actions = new ArrayList<>();
    }

    public void addAction(PlayerAction action) {
        actions.add(action);
        updateBalance(action);
    }

    public void updateBalance(long amount) {
        System.out.println("Before update long: " + balance);
        balance += amount;
        System.out.println("After update long: " + balance);

    }

    public void updateBalance(PlayerAction action) {
        try {
            System.out.println("Before updateBalanceAction: " + balance);
            switch (action.getAction()) {
                case "DEPOSIT":
                    balance += action.getCoinsAmount();
                    break;
                case "BET":
                    balance -= action.getCoinsAmount();
                    break;
                case "WITHDRAW":
                    balance -= action.getCoinsAmount();
                    break;
            }
            System.out.println("After updateBalanceAction: " + balance);
        } catch (Exception e) {
            System.err.println("Error updating balance: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasFirstIllegalOperation() {
        // Implement this method to check if there's any illegal operation
        // For example, check if the balance goes negative during a withdrawal
        return false;
    }

    public String getFirstIllegalOperation() {
        // Implement this method to return details of the first illegal operation
        return null;
    }


}