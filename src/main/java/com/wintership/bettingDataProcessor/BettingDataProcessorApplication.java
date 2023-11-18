package com.wintership.bettingDataProcessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class BettingDataProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(BettingDataProcessorApplication.class, args);

		String matchDataFilePath = "src/main/resources/match_data.txt";
		String playerDataFilePath = "src/main/resources/player_data.txt";

		DataFileReader matchDataReader = new DataFileReader(matchDataFilePath);
		DataFileReader playerDataReader = new DataFileReader(playerDataFilePath);


		matchDataReader.readAndProcessMatchData();

		playerDataReader.readAndProcessData();

		playerDataReader.identifyAndLogPlayerBalances();



		//UUID matchIdToFind = UUID.fromString("d6c8b5a4-31ce-4bf8-8511-206cfd693440");// specify the match ID you want to find
		//		System.out.println("Attempting to find match with ID: " + matchIdToFind);
		//Match foundMatch = matchDataReader.findMatchById(matchIdToFind);
	}
}

