package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Handles when a user executes the /blackjack command
 */
public class GameCommandHandler implements SlashCommandCreateListener {
    private final Bot bot;
    public static final HashMap<Long, Game> blackjackGames = new HashMap<>();

    public GameCommandHandler(Bot bot) {
        this.bot = bot;
    }

    /**
     * Creates and registers a Blackjack game.
     * @param event The event.
     */
    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();
        User user = interaction.getUser();

        // Ignore other slash commands
        if (!interaction.getCommandName().equalsIgnoreCase("blackjack")) {
            return;
        }

        if (blackjackGames.containsKey(user.getId())) {
            interaction.createImmediateResponder()
                    .setContent("Please finish your previous Blackjack game before starting a new one.")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
            return;
        }

        if (interaction.getOptionLongValueByIndex(0).isEmpty()) {
            interaction.createImmediateResponder()
                    .setContent("Please provide a valid bet.")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
            return;
        }

        try {
            String fileName = "bjpoints.json";
            FileReader reader = new FileReader(fileName);
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(reader);
            reader.close();

            // Add the server to the JSON if they're not already on file
            if (!json.containsKey(interaction.getServer().get().getIdAsString())) {
                json.put(interaction.getServer().get().getIdAsString(), new JSONObject());
                Files.write(Paths.get(fileName), json.toJSONString().getBytes());
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        Game game = new Game(bot, interaction.getServer().get(), user, interaction.getOptionLongValueByIndex(0).get());

        // Player tried to bet less than one point
        if (game.getBet() < 1) {
            interaction.createImmediateResponder()
                    .setContent("You must bet at least one point.")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
            return;
        }

        // Player's bet is too high
        if (game.getPlayerPointAmount() < game.getBet()) {
            interaction.createImmediateResponder()
                    .setContent("Sorry, you need " + (game.getBet() - game.getPlayerPointAmount()) + " more points.")
                    .setFlags(MessageFlag.EPHEMERAL)
                    .respond();
            return;
        }


        // Create embed with all game information
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Blackjack");
        eb.setDescription("You bet **" + game.getBet() + "** point" + (game.getBet() != 1 ? "s" : "") +"\n" +
                "You have **" + game.getPlayerPointAmount() + "** point" + (game.getPlayerPointAmount() != 1 ? "s" : "") + "\n\n" +
                "**Rules**\n" +
                "Dealer must stand on all 17s\n" +
                "Blackjack pays 3 to 2");
        eb.setColor(new Color(184, 0, 9));
        eb.setFooter("Game with " + user.getDiscriminatedName(), user.getAvatar());
        eb.setThumbnail("https://the-datascientist.com/wp-content/uploads/2020/05/counting-cards-black-jack.png");

        Hand playerHand = game.getPlayerHands().get(0);

        // Player is dealt a Blackjack
        if (playerHand.getScore() == 21) {
            eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
            eb.addField("Your Hand (" + (playerHand.isSoft() ? "Soft " : "") + playerHand.getScore() + ")", playerHand.toString());
            eb.setDescription("**You have a blackjack! You win " + (long) Math.ceil(game.getBet() * 1.5) + " points!**");
            eb.setFooter(user.getDiscriminatedName() + " won!", user.getAvatar());

            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .respond();
            game.givePoints((long) Math.ceil(game.getBet() * 1.5));
            return;
        }

        // Dealer is dealt a Blackjack
        if (game.getDealerHand().get(0).getValue() == 1 && game.getDealerHand().getScore() == 21) {
            eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
            eb.addField("Your Hand (" + (playerHand.isSoft() ? "Soft " : "") + playerHand.getScore() + ")", playerHand.toString());
            eb.setDescription("**Dealer has a blackjack! You lose " + game.getBet() + " point" + (game.getBet() == 1 ? "" : "s") + "**");
            eb.setFooter(user.getDiscriminatedName() + " lost!", user.getAvatar());

            interaction.createImmediateResponder()
                    .addEmbed(eb)
                    .respond();
            game.givePoints(-game.getBet());
            return;
        }

        // Show the dealer's up card and the players hand
        eb.addField("Dealer's Hand", game.getDealerHand().get(0).toString());
        eb.addField("Your Hand (" + (playerHand.isSoft() ? "Soft " : "") + playerHand.getScore() + ")", playerHand.toString());

        Message message;
        // Add double down option if the player has enough points

        message = interaction.createImmediateResponder().setContent("Setting up game...")
                .respond().join().update().join();

        game.setMessage(message);
        game.refreshMessage();

        blackjackGames.put(user.getId(), game);
    }
}
