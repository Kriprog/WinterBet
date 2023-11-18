package com.wintership.bettingDataProcessor;

public class Casino {
    private long balance;

    public Casino(long initialBalance) {
        this.balance = initialBalance;
    }

    public void adjustBalance(long amount) {
        // Logic to adjust balance based on bet action
        balance += amount;
    }

    public long getBalance() {
        return balance;
    }
}
