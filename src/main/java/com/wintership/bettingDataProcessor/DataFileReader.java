package com.wintership.bettingDataProcessor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class DataFileReader {

    private final String filePath;
    private final List<Player> players;
    private static final List<Match> matches = new ArrayList<>();

    private Player currentPlayer;

    public DataFileReader(String filePath) {
        this.filePath = filePath;
        this.players = new ArrayList<>();
    }

    public void readAndProcessData() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length < 4) {
                    continue;
                }

                UUID playerId = UUID.fromString(data[0]);
                if (currentPlayer == null || !playerId.equals(currentPlayer.getPlayerId())) {
                    currentPlayer = new Player(playerId);
                    players.add(currentPlayer);
                }

                processLine(currentPlayer, line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // After reading and processing the data, iterate over the list
        // and identify the first illegal operation for each player.
        for (Player player : players) {
            identifyFirstIllegalOperation(player);
        }
        System.out.println("Number of players: " + players.size());
    }

    private void processLine(Player currentPlayer, String line) {
        String[] data = line.split(",");

        if (data.length < 5 && !("DEPOSIT".equals(data[1]) || "WITHDRAW".equals(data[1]))) {
            System.err.println("Invalid line: " + line);
            return;
        }

        try {
            System.out.println("Processing line: " + line);

            String actionType = data[1];
            UUID matchId = data.length > 2 && !data[2].isEmpty() ? UUID.fromString(data[2]) : null;
            int coinsAmount = Integer.parseInt(data[3]);
            String sideBetOn = null;

            if ("DEPOSIT".equals(actionType) || "WITHDRAW".equals(actionType)) {
                matchId = null;
            } else if (data.length > 4 && !data[4].isEmpty()) {
                // If there is a non-empty value after the fourth comma, set it as sideBetOn
                sideBetOn = data[4].trim();
            }

            PlayerAction action = new PlayerAction(actionType, matchId, coinsAmount, sideBetOn, matches, currentPlayer.getPlayerId());
            if (!currentPlayer.hasProcessedAction(action)) {
                currentPlayer.addAction(action);
            }

            if ("BET".equals(action.getAction())) {
                if (matchId != null) {
                    Match currentMatch = findMatchById(matchId);
                    if (currentMatch != null) {
                        calculateWinnings(currentPlayer, currentMatch, action);
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            // Handle the exception if UUID or numeric parsing fails
            System.err.println("Error processing line: " + line);
            e.printStackTrace();
        }
    }


    private void identifyFirstIllegalOperation(Player player) {
        // Logic to identify and print the first illegal operation for each player
        // Example: Check if the action is WITHDRAW and if the balance is insufficient
        if (player.hasFirstIllegalOperation()) {
            System.out.println("First illegal operation for Player " + player.getPlayerId() + ": " + player.getFirstIllegalOperation());
            // Add further processing or error handling as needed
        }
    }

    public void readAndProcessMatchData() {
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.forEach(line -> {
                processMatchData(line);
            });
            System.out.println("Final matches list: " + matches);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finished reading and processing match data.");
    }

    private void processMatchData(String line) {
        Match match = createMatchObject(line);

        if (match != null) {
            matches.add(match);

        } else {
            System.err.println("Failed to create Match object from line: " + line);
        }
    }

    private Match createMatchObject(String line) {
        try {
            String[] data = line.split(",");

            if (data.length < 4) {
                System.err.println("Invalid match data. Expected 4 fields, but found " + data.length + ": " + line);
                return null;
            }

            Match match = new Match();
            match.setMatchId(UUID.fromString(data[0]));
            match.setRateValueA(new BigDecimal(data[1]));
            match.setRateValueB(new BigDecimal(data[2]));
            match.setWinningSide(data[3]);

            return match;
        } catch (Exception e) {
            System.err.println("Error creating Match object from line: " + line);
            e.printStackTrace();
            return null;
        }
    }

    private int calculateWinnings(Player currentPlayer, Match match, PlayerAction bet) {
        if (match.getMatchId().equals(bet.getMatchId())) {
            System.out.println("Before winnings calculation: " + currentPlayer.getBalance());

            if ("DRAW".equalsIgnoreCase(match.getWinningSide())) {
                int winnings = bet.getCoinsAmount();
                currentPlayer.updateBalance(winnings);
                return winnings;
            }

            BigDecimal rate;
            switch (bet.getSideBetOn()) {
                case "A":
                    rate = match.getRateValueA();
                    break;
                case "B":
                    rate = match.getRateValueB();
                    break;
                case "DRAW":
                    rate = new BigDecimal(1); // For simplicity, assuming draw has a rate of 1
                    break;
                default:
                    throw new IllegalArgumentException("Invalid side bet: " + bet.getSideBetOn());
            }

            // Check if the bet is on the winning side
            if (match.getWinningSide().equalsIgnoreCase(bet.getSideBetOn())) {
                int winnings = (int) (bet.getCoinsAmount() * rate.doubleValue() + bet.getCoinsAmount());
                currentPlayer.updateBalance(winnings);
                System.out.println("After winnings calculation: " + currentPlayer.getBalance());

                return winnings;
            } else {
                return 0; // Player loses the bet
            }
        } else {
            System.out.println("Match not found!");
        }

        return 0; // Player loses the bet or error occurred
    }

    private Match findMatchById(UUID matchId) {
        for (Match match : matches) {
            if (match.getMatchId().equals(matchId)) {
                return match;
            }
        }

        System.out.println("Match not found!");
        return null;
    }

    public void identifyAndLogPlayerBalances() {
        System.out.println("Identifying and logging player balances...");

        for (Player player : players) {
            System.out.println(player.getPlayerId() +
                    " " + player.getBalance());
        }
        System.out.println("Player balances identified and logged.");
    }


}
