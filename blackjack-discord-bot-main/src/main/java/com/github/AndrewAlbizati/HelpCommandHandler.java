package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;

import java.awt.*;

/**
 * Handles when a user executes the /help command
 */
public class HelpCommandHandler implements SlashCommandCreateListener {
    private final Bot bot;

    public HelpCommandHandler(Bot bot) {
        this.bot = bot;
    }

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction interaction = event.getSlashCommandInteraction();

        // Ignore other slash commands
        if (!interaction.getCommandName().equalsIgnoreCase("help")) {
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Blackjack Help");
        eb.addField("How to Play", "You and the dealer are dealt 2 cards in the beginning of the game. You can see both of your cards and one of the dealer's cards. The goal of the game is to get your card total close to 21 without going over. Each card is valued normally, except that face cards are worth 10, and aces are worth 1 or 11 (depending on if it goes over the total).");
        eb.addField("Controls", "When a game is started, you can hit (draw a card) or stand (stop drawing cards). If you have enough points, you can also double down (doubles your bet but you are only given 1 more card). If both of the cards are the same number, they can also be split (starts 2 Blackjack hands in the same game with each of the cards).");
        eb.addField("Strategy", "A good strategy for Blackjack is to hit until your total is 17 or above, and then stand. Doubling down on totals of 10 and 11 are also generally favorable for the player.");
        eb.addField("About", "For more information about the bot, click [here](https://github.com/AndrewAlbizati/blackjack-discord-bot).");
        eb.setThumbnail(bot.getApi().getYourself().getAvatar());
        eb.setColor(new Color(184, 0, 9));

        interaction.createImmediateResponder()
                .addEmbed(eb)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond();
    }
}
