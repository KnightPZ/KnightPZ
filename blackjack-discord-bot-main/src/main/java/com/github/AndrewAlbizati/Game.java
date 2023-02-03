package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Represents a game of Blackjack.
 * Each user can only be playing 1 game.
 */
public class Game {
    private Message message;

    private final Server server;
    private final User user;
    private long bet;

    private final Deck deck;
    private final Hand dealerHand;
    private final ArrayList<Hand> playerHand = new ArrayList<>();

    private int selectedHandIndex = 0;

    public Game(Bot bot, Server server, User user, long bet) {
        this.server = server;
        this.user = user;
        this.bet = bet;

        this.deck = bot.getDeck();

        playerHand.clear();

        playerHand.add(new Hand());
        dealerHand = new Hand();

        playerHand.get(0).add(deck.deal());
        dealerHand.add(deck.deal());

        playerHand.get(0).add(deck.deal());
        dealerHand.add(deck.deal());
    }

    /**
     * Returns the index of the hand that the user is currently playing with.
     * Only used if the user has split and has multiple active hands.
     * @return The index of the current hand.
     */
    public int getSelectedHandIndex() {
        return selectedHandIndex;
    }

    /**
     * Increments the index of the hand that the user is currently playing with.
     * Only used if the user has split and has multiple active hands.
     */
    public void incrementSelectedHandIndex() {
        selectedHandIndex++;
    }

    /**
     * Gets the message that contains the active game.
     * @return A message object.
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Sets the message that contains the active game.
     * @param message The message object.
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Refreshes the message with the active game
     * Creates new embed with updated information.
     * Removes certain buttons if applicable.
     */
    public void refreshMessage() {
        // Create new embed with all current game information
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Blackjack");
        eb.setDescription("You bet **" + bet + "** point" + (bet != 1 ? "s" : "") +"\n" +
                "You have **" + getPlayerPointAmount() + "** point" + (getPlayerPointAmount() != 1 ? "s" : "") + "\n\n" +
                "**Rules**\n" +
                "Dealer must stand on all 17s\n" +
                "Blackjack pays 3 to 2");
        eb.setColor(new Color(184, 0, 9));
        eb.setFooter("Game with " + user.getDiscriminatedName(), user.getAvatar());
        eb.setThumbnail("https://the-datascientist.com/wp-content/uploads/2020/05/counting-cards-black-jack.png");

        // Show the dealer's up card and the players hand
        eb.addField("Dealer's Hand", getDealerHand().get(0).toString());
        eb.addField("Your Hand (" + (getPlayerHands().get(selectedHandIndex).isSoft() ? "Soft " : "") + getPlayerHands().get(selectedHandIndex).getScore() + ")", getPlayerHands().get(selectedHandIndex).toString());

        // Display multiple hands if user has split
        if (getPlayerHands().size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < getPlayerHands().size(); i++) {
                Hand hand = getPlayerHands().get(i);
                if (i == selectedHandIndex) {
                    sb.append("**");
                }

                sb.append("Hand ");
                sb.append((i + 1));
                sb.append(": ");

                sb.append((hand.isSoft() ? "Soft " : ""));
                sb.append(hand.getScore());
                if (i == selectedHandIndex) {
                    sb.append("**");
                }

                sb.append("\n");
            }
            eb.addField("Other Hands", sb.toString());
        }

        // Remove all buttons if hand is completed
        if (getPlayerHands().get(getSelectedHandIndex()).isCompleted()) {
            message.createUpdater()
                    .setEmbed(eb)
                    .removeAllComponents()
                    .applyChanges();
        } else {
            // Determine which buttons should be displayed
            Hand playerHand = getPlayerHands().get(getSelectedHandIndex());

            ActionRow components;
            if (getBet() * 2 <= getPlayerPointAmount() && playerHand.size() == 2) {

                if (playerHand.get(0).compareTo(playerHand.get(1)) == 0) {
                    components = ActionRow.of(
                            Button.primary("hit", "Hit"),
                            Button.primary("stand", "Stand"),
                            Button.primary("dd", "Double Down"),
                            Button.primary("split", "Split")
                    );
                } else {
                    components = ActionRow.of(
                            Button.primary("hit", "Hit"),
                            Button.primary("stand", "Stand"),
                            Button.primary("dd", "Double Down")
                    );
                }
            } else {
                components = ActionRow.of(
                        Button.primary("hit", "Hit"),
                        Button.primary("stand", "Stand")
                );
            }
            message.createUpdater()
                    .setEmbed(eb)
                    .removeAllComponents()
                    .addComponents(components)
                    .applyChanges();
        }
    }

    /**
     * Get the user that is playing the game of Blackjack.
     * @return A user object.
     */
    public User getUser() {
        return user;
    }

    /**
     * Get the deck that is being used for this game of Blackjack.
     * @return A deck object.
     */
    public Deck getDeck() {
        return deck;
    }

    /**
     * Get the hand that the dealer is playing with.
     * @return A Hand object.
     */
    public Hand getDealerHand() {
        return dealerHand;
    }

    /**
     * Gets all of the player hands.
     * Player can have more than one hand if they split.
     * @return An ArrayList of Hands.
     */
    public ArrayList<Hand> getPlayerHands() {
        return playerHand;
    }

    /**
     * Get the value of the bet that the user placed.
     * Can be changed by splitting or doubling.
     * @return The long value of their bet.
     */
    public long getBet() {
        return bet;
    }

    /**
     * Set the user's bet.
     * Used when doubling and splitting.
     * @param bet The new value of bet.
     */
    public void setBet(long bet) {
        this.bet = bet;
    }

    /**
     * Gets the amount of points that a specific player has.
     * Data stored in bjpoints.json
     *
     * @return The long value of the amount of points the player has.
     */
    public long getPlayerPointAmount() {
        try {
            String fileName = "bjpoints.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            reader.close();

            JSONObject serverPoints = (JSONObject) json.get(server.getIdAsString());

            // Add the user to the JSON if they're not already on file
            if (!serverPoints.containsKey(user.getIdAsString())) {
                serverPoints.put(user.getIdAsString(), 100L); // Player receives 100 points to start with
            }

            return (long) serverPoints.get(user.getIdAsString());

        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * Adds or subtracts points from a user's score.
     * Data stored in bjpoints.json
     *
     * @param pointAmount The amount of points that will be added/removed.
     */
    public void givePoints(long pointAmount) {
        try {
            String fileName = "bjpoints.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            reader.close();

            JSONObject serverPoints = (JSONObject) json.get(server.getIdAsString());

            // Add the user to the JSON if they're not already on file
            if (!serverPoints.containsKey(user.getIdAsString())) {
                serverPoints.put(user.getIdAsString(), 100L); // Player receives 100 points to start with
            }

            long userPoints = (long) serverPoints.get(user.getIdAsString());

            serverPoints.put(user.getIdAsString(), userPoints + pointAmount);
            json.put(server.getIdAsString(), serverPoints);

            Files.write(Paths.get(fileName), json.toJSONString().getBytes());
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
