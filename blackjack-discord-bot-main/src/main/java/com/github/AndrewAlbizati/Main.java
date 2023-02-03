package com.github.AndrewAlbizati;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Contains Main function that is run on start.
 */
public class Main {
    /**
     * Creates bjpoints.json and config.properties if not already created.
     * If token is provided in config.properties, the bot will start
     * @param args N/A
     */
    public static void main(String[] args) {
        // Check if bjpoints.json and config.properties are present, creates new files if absent
        try {
            File pointsJSONFile = new File("bjpoints.json");
            if (pointsJSONFile.createNewFile()) {
                FileWriter writer = new FileWriter("bjpoints.json");
                writer.write("{}"); // Empty JSON object
                writer.close();
                System.out.println("bjpoints.json has been created");
            }

            File config = new File("config.properties");
            if (config.createNewFile()) {
                FileWriter writer = new FileWriter("config.properties");
                writer.write("token=");
                writer.close();
                System.out.println("config.properties has been created");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Get token from config.properties
        String token;
        try {
            Properties prop = new Properties();
            FileInputStream ip = new FileInputStream("config.properties");
            prop.load(ip);
            ip.close();

            token = prop.getProperty("token");

            if (token.length() == 0)
                throw new NullPointerException("Please add a Discord bot token into config.properties");
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
            System.out.println("Token not found! " + e.getMessage());
            return;
        }

        Bot bot = new Bot(token);
        bot.run();
    }
}
