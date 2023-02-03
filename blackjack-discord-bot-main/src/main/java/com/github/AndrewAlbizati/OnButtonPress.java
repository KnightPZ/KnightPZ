package com.github.AndrewAlbizati;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.MessageComponentCreateEvent;
import org.javacord.api.interaction.MessageComponentInteraction;
import org.javacord.api.listener.interaction.MessageComponentCreateListener;

/**
 * Handles whenever a button is pressed in the game.
 */
public class OnButtonPress implements MessageComponentCreateListener {
    /**
     * Main function that handles button presses.
     * @param messageComponentCreateEvent The event.
     */
    @Override
    public void onComponentCreate(MessageComponentCreateEvent messageComponentCreateEvent) {
        MessageComponentInteraction messageComponentInteraction = messageComponentCreateEvent.getMessageComponentInteraction();
        String customId = messageComponentInteraction.getCustomId();

        Message message = messageComponentInteraction.getMessage();
        Game game = null;

        // Find game that is being played
        for (Long userId : GameCommandHandler.blackjackGames.keySet()) {
            if (GameCommandHandler.blackjackGames.get(userId).getMessage().getId() == message.getId()) {
                game = GameCommandHandler.blackjackGames.get(userId);
            }
        }

        // Game couldn't be found
        if (game == null || game.getUser().getId() != messageComponentInteraction.getUser().getId()) {
            return;
        }

        EmbedBuilder eb = message.getEmbeds().get(0).toBuilder();

        boolean endGame = false;

        Hand playerHand = game.getPlayerHands().get(game.getSelectedHandIndex());

        // Handle each of the decisions (dd, hit, stand, split)
        switch (customId) {
            case "split":
                Card card1 = game.getPlayerHands().get(game.getSelectedHandIndex()).get(0);
                Card card2 = game.getPlayerHands().get(game.getSelectedHandIndex()).get(1);

                game.getPlayerHands().get(game.getSelectedHandIndex()).clear();
                game.getPlayerHands().get(game.getSelectedHandIndex()).add(card1);

                Hand hand2 = new Hand();
                hand2.add(card2);
                game.getPlayerHands().add(hand2);

                game.getPlayerHands().get(game.getSelectedHandIndex()).add(game.getDeck().deal());
                game.getPlayerHands().get(game.getSelectedHandIndex() + 1).add(game.getDeck().deal());

                game.refreshMessage();
                break;

            case "dd":
                game.setBet(game.getBet() * 2);
                endGame = true;

            case "hit":
                game.getPlayerHands().get(game.getSelectedHandIndex()).add(game.getDeck().deal());

                eb.removeAllFields();
                eb.setDescription("You bet **" + game.getBet() + "** point" + (game.getBet() != 1 ? "s" : "") + "\n" +
                        "You have **" + game.getPlayerPointAmount() + "** point" + (game.getPlayerPointAmount() != 1 ? "s" : "") + "\n\n" +
                        "**Rules**\n" +
                        "Dealer must stand on all 17s\n" +
                        "Blackjack pays 3 to 2");

                eb.addField("Dealer", game.getDealerHand().get(0).toString());
                eb.addField("Your Hand (" + (playerHand.isSoft() ? "Soft " : "") + playerHand.getScore() + ")", playerHand.toString());

                // End game if player busts
                if (playerHand.getScore() >= 21) {
                    endGame = true;
                }

                // Resend the embed if the game is still in play
                if (!endGame) {
                    game.refreshMessage();
                }

                break;

            case "stand":
                endGame = true;
                break;
        }

        messageComponentInteraction.acknowledge();

        // Update the Game object status for completed
        game.getPlayerHands().get(game.getSelectedHandIndex()).setCompleted(endGame);

        // Stop code if the game hasn't ended
        if (!endGame) {
            return;
        }

        // Move to next hand if multiple and current hand is finished
        if (game.getPlayerHands().size() > game.getSelectedHandIndex() + 1) {
            game.incrementSelectedHandIndex();
            game.refreshMessage();
            return;
        }

        // Player can no longer make any moves
        game.refreshMessage();

        // Player only has 1 hand (no splits)
        if (game.getPlayerHands().size() == 1) {
            endOneHandGame(game);
        } else {
            endMultiHandGame(game);
        }

        GameCommandHandler.blackjackGames.remove(game.getUser().getId());
    }

    /**
     * End the game if the player only has 1 hand.
     * Dealer hits until 17 or bust, and points are awarded.
     * @param game A completed game.
     */
    private void endOneHandGame(Game game) {
        Hand hand = game.getPlayerHands().get(0);
        EmbedBuilder eb = game.getMessage().getEmbeds().get(0).toBuilder();

        // Player busts
        if (hand.getScore() > 21) {
            eb.setDescription("**You busted! You lose " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");

            eb.removeAllFields();
            eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
            eb.addField("Your Hand (" + (hand.isSoft() ? "Soft " : "") + hand.getScore() + ")", hand.toString());

            eb.setFooter(game.getUser().getDiscriminatedName() + " lost!", game.getUser().getAvatar());
            game.getMessage().createUpdater()
                    .setEmbed(eb)
                    .removeAllComponents()
                    .applyChanges();
            game.givePoints(-game.getBet());
            GameCommandHandler.blackjackGames.remove(game.getUser().getId());
            return;
        }

        // Dealer hits until they get 17+
        while (game.getDealerHand().getScore() < 17) {
            game.getDealerHand().add(game.getDeck().deal());
        }

        // Add final score
        eb.removeAllFields();
        eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());
        eb.addField("Your Hand (" + (hand.isSoft() ? "Soft " : "") + hand.getScore() + ")", hand.toString());


        // Dealer busts
        if (game.getDealerHand().getScore() > 21) {
            eb.setDescription("**Dealer busted! You win " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " won!", game.getUser().getAvatar());

            game.getMessage().createUpdater()
                    .setEmbed(eb)
                    .removeAllComponents()
                    .applyChanges();
            game.givePoints(game.getBet());
            GameCommandHandler.blackjackGames.remove(game.getUser().getId());
            return;
        }

        // Dealer wins
        if (game.getDealerHand().getScore() > hand.getScore()) {
            eb.setDescription("**The dealer beat you! You lose " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " lost!", game.getUser().getAvatar());
            game.givePoints(-game.getBet());
            // Player wins
        } else if (hand.getScore() > game.getDealerHand().getScore()) {
            eb.setDescription("**You beat the dealer! You win " + game.getBet() + " point" + (game.getBet() != 1 ? "s" : "") + "**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " won!", game.getUser().getAvatar());
            game.givePoints(game.getBet());
            // Tie
        } else {
            eb.setDescription("**You and the dealer tied! You don't win or lose any points**");
            eb.setFooter(game.getUser().getDiscriminatedName() + " tied!", game.getUser().getAvatar());
        }
        game.getMessage().createUpdater()
                .setEmbed(eb)
                .removeAllComponents()
                .applyChanges();
    }

    /**
     * End the game if the player has multiple hands.
     * Dealer hits until 17 or bust, and points are awarded
     * @param game A completed game.
     */
    private void endMultiHandGame(Game game) {
        EmbedBuilder eb = game.getMessage().getEmbeds().get(0).toBuilder();
        // Dealer hits until they get 17+
        while (game.getDealerHand().getScore() < 17) {
            game.getDealerHand().add(game.getDeck().deal());
        }

        // Add final score
        eb.removeAllFields();
        eb.addField("Dealer's Hand (" + (game.getDealerHand().isSoft() ? "Soft " : "") + game.getDealerHand().getScore() + ")", game.getDealerHand().toString());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < game.getPlayerHands().size(); i++) {
            int score = game.getPlayerHands().get(i).getScore();
            int dealerScore = game.getDealerHand().getScore();
            sb.append("Hand ");
            sb.append(i + 1);
            sb.append(": ");
            sb.append(score);
            sb.append(" **");

            if (score > 21) {
                sb.append("BUST");
                game.givePoints(-game.getBet());
            } else if (score > dealerScore || dealerScore > 21) {
                sb.append("WIN");
                game.givePoints(game.getBet());
            } else if (score == dealerScore) {
                sb.append("PUSH");
            } else {
                sb.append("LOSE");
                game.givePoints(-game.getBet());
            }

            sb.append("**\n");
        }
        eb.addField("Your Hands", sb.toString());

        game.getMessage().createUpdater()
                .setEmbed(eb)
                .removeAllComponents()
                .applyChanges();
        GameCommandHandler.blackjackGames.remove(game.getUser().getId());
    }
}
