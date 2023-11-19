package com.winternship.bettingDataProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Stream;

public class DataFileReader {
    static final Logger logger = LoggerFactory.getLogger(DataFileReader.class);
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
                if (currentPlayer.hasFirstIllegalOperation()) {
                    continue;
                }

                processLine(currentPlayer, line);
            }
        } catch (IOException e) {
            logger.error("An error occurred:", e);
        }

        for (Player player : players) {
            identifyFirstIllegalOperation(player);
        }
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
            String sideBetOn = data.length > 4 ? data[4] : null;

            if ("WITHDRAW".equals(actionType) && currentPlayer.getBalance() < coinsAmount) {
                PlayerAction illegalWithdrawal = new PlayerAction("WITHDRAW", null, coinsAmount, null, null, currentPlayer.getPlayerId());
                currentPlayer.setFirstIllegalOperation(illegalWithdrawal);
                return;
            }

            if ("BET".equals(actionType)) {
                if (currentPlayer.getBalance() < coinsAmount) {
                    PlayerAction illegalBet = new PlayerAction("BET", matchId, coinsAmount, sideBetOn, null, currentPlayer.getPlayerId());
                    currentPlayer.setFirstIllegalOperation(illegalBet);
                    return;
                }
            }

            if ("DEPOSIT".equals(actionType) || "WITHDRAW".equals(actionType)) {
                matchId = null;
            } else if (data[4] != null && !data[4].trim().isEmpty()) {

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
            logger.error("An error occurred:", e);
        }
    }


    private void identifyFirstIllegalOperation(Player player) {
        player.hasFirstIllegalOperation();
    }

    public void readAndProcessMatchData() {
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.forEach(this::processMatchData);
        } catch (IOException e) {
            logger.error("An error occurred:", e);
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
            logger.error("Error creating Match object from line: " + line);
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

                BigDecimal rate = switch (bet.getSideBetOn()) {
                    case "A" -> match.getRateValueA();
                    case "B" -> match.getRateValueB();
                    case "DRAW" -> new BigDecimal(1);
                    default -> throw new IllegalArgumentException("Invalid side bet: " + bet.getSideBetOn());
                };

                if (match.getWinningSide().equalsIgnoreCase(bet.getSideBetOn())) {
                    int winnings = (int) (bet.getCoinsAmount() * rate.doubleValue());
                    int totalWinnings = bet.getCoinsAmount() + winnings;
                    currentPlayer.updateBalance(totalWinnings);
                    currentPlayer.updateNetAmount(winnings);
                    currentPlayer.incrementWonMatches();

                } else {
                    currentPlayer.updateNetAmount(-bet.getCoinsAmount());
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

    public void writeResultToFile(String resultsFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFilePath))) {
            legitimatePlayersToFile(writer);
            writer.newLine();
            illegitimatePlayersToFile(writer);
            writer.newLine();
            casinoBalanceToFile(writer);
        } catch (IOException e) {
            logger.error("An error occurred:", e);

        }
    }

    private void legitimatePlayersToFile(BufferedWriter writer) throws IOException {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.GERMAN));

        players.stream()
                .filter(player -> !player.hasFirstIllegalOperation())
                .sorted(Comparator.comparing(Player::getPlayerId))
                .forEach(player -> {
            try {
                BigDecimal winRate = player.getWinRate();
                writer.write(player.getPlayerId() + " " + player.getBalance() + " " + decimalFormat.format(winRate.setScale(2, RoundingMode.HALF_UP)));
                writer.newLine();
            } catch (IOException e) {
                logger.error("An error occurred:", e);
            }
        });
    }

    private void illegitimatePlayersToFile(BufferedWriter writer) throws IOException {

        players.stream()
                .filter(Player::hasFirstIllegalOperation)
                .sorted(Comparator.comparing(Player::getPlayerId))
                .forEach(player -> {
                    try {
                        PlayerAction firstIllegalOperation = player.getFirstIllegalOperation();
                        writer.write(player.getPlayerId() + " " + firstIllegalOperation.getAction() + " " + firstIllegalOperation.getMatchId() + " " + firstIllegalOperation.getCoinsAmount() + " " + firstIllegalOperation.getSideBetOn());
                        writer.newLine();
                    } catch (IOException e) {
                        logger.error("An error occurred:", e);
                    }
                });
    }

    private void casinoBalanceToFile(BufferedWriter writer) throws IOException {
        long sumNetAmounts = players.stream()
                .filter(player -> !player.hasFirstIllegalOperation())
                .mapToLong(Player::getNetAmount)
                .sum();

        writer.write("" + sumNetAmounts * -1);
    }
}
