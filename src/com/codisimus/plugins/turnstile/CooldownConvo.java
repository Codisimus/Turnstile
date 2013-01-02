package com.codisimus.plugins.turnstile;

import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.FixedSetPrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;

/**
 *
 * @author Cody
 */
public class CooldownConvo implements ConversationAbandonedListener {
    private ConversationFactory cFactory;
    private Turnstile turnstile;

    public CooldownConvo(CommandSender commandSender, Turnstile turnstile) {
        this.cFactory = new ConversationFactory(TurnstileMain.plugin)
                .withPrefix(new CooldownConversationPrefix())
                .withFirstPrompt(turnstile.days == 0 && turnstile.hours == 0 &&
                                 turnstile.minutes == 0 && turnstile.seconds == 0
                                 ? new ForeverPrompt()
                                 : new DisablePrompt())
                .withEscapeSequence("/done")
                .addConversationAbandonedListener(this);
        if (commandSender instanceof Conversable) {
            cFactory.buildConversation((Conversable) commandSender).begin();
        }
        this.turnstile = turnstile;
    }

    @Override
    public void conversationAbandoned(ConversationAbandonedEvent abandonedEvent) {
        turnstile.save();
        if (abandonedEvent.gracefulExit()) {
            abandonedEvent.getContext().getForWhom().sendRawMessage("Conversation exited gracefully.");
        } else {
            abandonedEvent.getContext().getForWhom().sendRawMessage("Conversation abandoned by " + abandonedEvent.getCanceller().getClass().getName());
        }
    }

    private class DisablePrompt extends FixedSetPrompt {
        public DisablePrompt() {
            super("yes", "no");
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Do you want to disable cooldown for this Turnstile? (yes or no)";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String s) {
            if (s.equals("yes")) {
                turnstile.days = 0;
                turnstile.hours = 0;
                turnstile.minutes = 0;
                turnstile.seconds = 0;
                return Prompt.END_OF_CONVERSATION;
            } else {
                return new ForeverPrompt();
            }
        }
    }

    private class ForeverPrompt extends FixedSetPrompt {
        public ForeverPrompt() {
            super("yes", "no");
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Do you want each Player to only pay for this Turnstile once? (yes or no)";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String s) {
            if (s.equals("yes")) {
                turnstile.days = -1;
                turnstile.hours = -1;
                turnstile.minutes = -1;
                turnstile.seconds = -1;
                return Prompt.END_OF_CONVERSATION;
            } else {
                return new HowManyDaysPrompt();
            }
        }
    }

    private class HowManyDaysPrompt extends NumericPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return "How long should the cooldown last? How many days?";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() >= 0 ;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "§4Input must be a positive Integer";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            turnstile.days = number.intValue();
            return new HowManyHoursPrompt();
        }
    }

    private class HowManyHoursPrompt extends NumericPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return "How many hours?";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() >= 0 ;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "§4Input must be a positive Integer";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            turnstile.hours = number.intValue();
            return new HowManyMinutesPrompt();
        }
    }

    private class HowManyMinutesPrompt extends NumericPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return "How many minutes?";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() >= 0 ;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "§4Input must be a positive Integer";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            turnstile.minutes = number.intValue();
            return new HowManySecondsPrompt();
        }
    }

    private class HowManySecondsPrompt extends NumericPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return "How many seconds?";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() >= 0 ;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "§4Input must be a positive Integer";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            turnstile.seconds = number.intValue();
            return new RoundDownPrompt();
        }
    }

    private class RoundDownPrompt extends FixedSetPrompt {
        public RoundDownPrompt() {
            super("yes", "no");
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Would you like the times to be rounded down to make daily/hourly/etc cooldowns";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String s) {
            turnstile.roundDown = s.equals("yes");
            return new LimitPrompt();
        }
    }

    private class LimitPrompt extends FixedSetPrompt {
        public LimitPrompt() {
            super("yes", "no");
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return "Do you want to limit how many Players can use this Turnstile at a time? "
                    + "For example, a turnstile leading to a hotel room should only allow 1 Player at a time. "
                    + "But there should be no limit to how many Players can use a train station";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, String s) {
            if (s.equals("no")) {
                turnstile.privateWhileOnCooldown = false;
                return Prompt.END_OF_CONVERSATION;
            } else {
                return new HowManyUsersPrompt();
            }
        }
    }

    private class HowManyUsersPrompt extends NumericPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            return "How many Players are allowed to use this turnstile before it cools down?";
        }

        @Override
        protected boolean isNumberValid(ConversationContext context, Number input) {
            return input.intValue() > 0 ;
        }

        @Override
        protected String getFailedValidationText(ConversationContext context, Number invalidInput) {
            return "§4Input must be a positive Integer";
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            turnstile.amountPerCooldown = number.intValue();
            return Prompt.END_OF_CONVERSATION;
        }
    }

    private class CooldownConversationPrefix implements ConversationPrefix {
        @Override
        public String getPrefix(ConversationContext context) {
            return "§2[Turnstile] ";
        }
    }
}
