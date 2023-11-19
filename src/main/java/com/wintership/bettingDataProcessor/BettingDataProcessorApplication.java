package com.wintership.bettingDataProcessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BettingDataProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BettingDataProcessorApplication.class, args);

        String matchDataFilePath = "src/main/resources/match_data.txt";
        String playerDataFilePath = "src/main/resources/player_data.txt";
        String resultFilePath = "src/main/java/com/wintership/bettingDataProcessor/result.txt";


        DataFileReader matchDataReader = new DataFileReader(matchDataFilePath);
        DataFileReader playerDataReader = new DataFileReader(playerDataFilePath);


        matchDataReader.readAndProcessMatchData();

        playerDataReader.readAndProcessData();

        playerDataReader.writeResultToFile(resultFilePath);

    }
}

