package com.wintership.bettingDataProcessor;

import lombok.Getter;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class DataFileReader {

    private final String filePath;
    private final List<Player> players;
    private static final List<Match> matches = new ArrayList<>();
    @Getter
    private int casinoBalance = 0;

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
                if (currentPlayer.hasFirstIllegalOperation()) {
                    continue;
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
        if (currentPlayer.hasFirstIllegalOperation()) {
            return;
        }
        String[] data = line.split(",");

        if (data.length < 5 && !("DEPOSIT".equals(data[1]) || "WITHDRAW".equals(data[1]))) {
            System.err.println("Invalid line: " + line);
            return;
        }

        try {
            String actionType = data[1];
            UUID matchId = data.length > 2 && !data[2].isEmpty() ? UUID.fromString(data[2]) : null;
            int coinsAmount = Integer.parseInt(data[3]);
            String sideBetOn = null;


            if ("WITHDRAW".equals(actionType) && currentPlayer.getBalance() < coinsAmount) {
                PlayerAction illegalWithdrawal = new PlayerAction("ILLEGAL_WITHDRAW", null, coinsAmount, null, null, currentPlayer.getPlayerId());
                currentPlayer.setFirstIllegalOperation(illegalWithdrawal);
                System.out.println("First illegal operation for Player " + currentPlayer.getPlayerId() + ": " + line);
                return;  // Stop further processing for this player
            }

// If it is a bet, check if the balance is sufficient
            if ("BET".equals(actionType) && currentPlayer.getBalance() < coinsAmount) {
                PlayerAction illegalBet = new PlayerAction("ILLEGAL_BET", matchId, coinsAmount, null, null, currentPlayer.getPlayerId());
                currentPlayer.setFirstIllegalOperation(illegalBet);
                System.out.println("First illegal operation for Player " + currentPlayer.getPlayerId() + ": " + line);
                return;  // Stop further processing for this player
            }

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
            System.err.println("Error processing line: " + line);
            e.printStackTrace();
        }
    }


    private void identifyFirstIllegalOperation(Player player) {
        if (player.hasFirstIllegalOperation()) {
            System.out.println("First illegal operation for Player " + player.getPlayerId() + ": " + player.getFirstIllegalOperation());
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

    private void calculateWinnings(Player currentPlayer, Match match, PlayerAction bet) {
            if (match.getMatchId().equals(bet.getMatchId())) {
                if (!currentPlayer.hasFirstIllegalOperation()) {
                    if ("DRAW".equalsIgnoreCase(match.getWinningSide())) {
                        int coinsBet = bet.getCoinsAmount();
                        currentPlayer.updateBalance(coinsBet);
                        currentPlayer.incrementTotalMatches();
                        return;
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
                            rate = new BigDecimal(1);
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid side bet: " + bet.getSideBetOn());
                    }

                    if (match.getWinningSide().equalsIgnoreCase(bet.getSideBetOn())) {
                        int winnings = (int) (bet.getCoinsAmount() * rate.doubleValue());
                        int totalWinnings = bet.getCoinsAmount() + winnings;
                        currentPlayer.updateBalance(totalWinnings);
                        casinoBalance -= winnings;
                        currentPlayer.incrementWonMatches();

                    } else {
                        casinoBalance += bet.getCoinsAmount();
                    }
                } else {
                    System.out.println("Illegitimate player detected: " + currentPlayer.getPlayerId());
                    return;

                }
                currentPlayer.incrementTotalMatches();
            }
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
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("results.txt"))) {
            identifyAndLogLegitimatePlayers(writer);
            writer.newLine();
            identifyAndLogIllegitimatePlayers(writer);
            writer.newLine();
            logCasinoBalanceChanges(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Results written to results.txt.");
    }

    private void identifyAndLogLegitimatePlayers(BufferedWriter writer) throws IOException {
        System.out.println("Identifying and logging legitimate player balances...");

        players.stream()
                .filter(player -> !player.hasFirstIllegalOperation())
                .sorted(Comparator.comparing(Player::getPlayerId))
                .forEach(player -> {
                    try {
                        BigDecimal winRate = player.getWinRate();
                        writer.write(player.getPlayerId() +
                                " " + player.getBalance() +
                                " " + winRate.setScale(2, BigDecimal.ROUND_HALF_UP));
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void identifyAndLogIllegitimatePlayers(BufferedWriter writer) throws IOException {
        System.out.println("Identifying and logging illegitimate players...");

        players.stream()
                .filter(Player::hasFirstIllegalOperation)
                .sorted(Comparator.comparing(Player::getPlayerId))
                .forEach(player -> {
                    try {
                        PlayerAction firstIllegalOperation = player.getFirstIllegalOperation();
                        writer.write(player.getPlayerId() +
                                " " + firstIllegalOperation.getAction() +
                                " " + firstIllegalOperation.getMatchId() +
                                " " + firstIllegalOperation.getCoinsAmount() +
                                " " + firstIllegalOperation.getSideBetOn());
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void logCasinoBalanceChanges(BufferedWriter writer) throws IOException {
        System.out.println("Logging casino balance changes...");

        int casinoBalanceChange = getCasinoBalance();

        writer.write("Casino Balance Change: " + casinoBalanceChange);
    }

}
