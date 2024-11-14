package com.socraticphoenix.sshcord.command;

import canaryprism.slavacord.Commands;
import canaryprism.slavacord.annotations.Command;
import canaryprism.slavacord.annotations.CommandGroup;
import canaryprism.slavacord.annotations.CreateGlobal;
import canaryprism.slavacord.annotations.Interaction;
import canaryprism.slavacord.annotations.Option;
import com.socraticphoenix.sshcord.App;
import com.socraticphoenix.sshcord.ssh.Session;
import com.socraticphoenix.sshcord.ssh.SessionManager;
import com.socraticphoenix.sshcord.util.MessageUtil;
import com.socraticphoenix.sshcord.util.StringUtil;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.HighLevelComponent;
import org.javacord.api.entity.message.component.LowLevelComponent;
import org.javacord.api.entity.message.component.TextInput;
import org.javacord.api.entity.message.component.TextInputStyle;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandInteraction;

import java.util.List;
import java.util.stream.Stream;

@CreateGlobal
public class SshCommand implements Commands {

    @CommandGroup(name = "ssh")
    public static class Ssh {

        @Command(name = "start", description = "Start an SSH session")
        public static void ssh(@Interaction SlashCommandInteraction interaction) {
            User user = interaction.getUser();
            interaction.respondWithModal(
                    "com.socraticphoenix.sshcord:ssh-" + user.getDiscriminatedName(),
                    "Connect to SSH",
                    wrap(
                            TextInput.create(TextInputStyle.SHORT, "host", "Hostname", "Enter the SSH URL", null, true),
                            TextInput.create(TextInputStyle.SHORT, "port", "Port", "Enter the SSH port number (default 22)", "22", false),
                            TextInput.create(TextInputStyle.SHORT, "user", "Username", "Enter the SSH username", null, true),
                            TextInput.create(TextInputStyle.SHORT, "pass", "Password", "Disclaimer: not very secure, use only for testing", null, true)
                    )).join();
        }

        @Command(name = "bytes", description = "Send custom bytes to an SSH session")
        public static void bytes(@Interaction SlashCommandInteraction interaction, @Option(name = "bytes", description = "A list of base-10 unsigned bytes to send") String bytes) {
            User user = interaction.getUser();
            SessionManager manager = App.sessionManager();
            Session session = manager.findSession(user);
            if (session != null) {
                String[] parts = bytes.split("[^0-9]+");
                byte[] result = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    try {
                        result[i] = (byte) Integer.parseInt(parts[i]);
                    } catch (NumberFormatException e) {
                        MessageUtil.send(interaction, "'" + parts[i] + "' is not a valid number");
                        return;
                    }
                }
                MessageUtil.send(interaction, "Sending raw bytes");
                session.sendInput(result);
            } else {
                MessageUtil.send(interaction, "You do not have an active session");
            }
        }

        @Command(name = "text", description = "Send custom text to an SSH session")
        public static void text(@Interaction SlashCommandInteraction interaction, @Option(name = "text", description = "The text to send") String text) {
            User user = interaction.getUser();
            SessionManager manager = App.sessionManager();
            Session session = manager.findSession(user);
            if (session != null) {
                MessageUtil.send(interaction, "Sending raw text");
                session.sendInput(StringUtil.deEscape(text));
            } else {
                MessageUtil.send(interaction, "You do not have an active session");
            }
        }

        @Command(name = "esc", description = "Send an escape sequence to an SSH session")
        public static void esc(@Interaction SlashCommandInteraction interaction, @Option(name = "sequence", description = "The escape sequence to send") String sequence) {
            User user = interaction.getUser();
            SessionManager manager = App.sessionManager();
            Session session = manager.findSession(user);
            if (session != null) {
                MessageUtil.send(interaction, "Sending escape sequence");
                session.sendInput("\u001b" + StringUtil.deEscape(sequence));
            } else {
                MessageUtil.send(interaction, "You do not have an active session");
            }
        }

        @Command(name = "close", description = "Close your existing SSH session")
        public static void close(@Interaction SlashCommandInteraction interaction) {
            User user = interaction.getUser();
            SessionManager manager = App.sessionManager();
            Session session = manager.findSession(user);

            if (session != null) {
                MessageUtil.send(interaction, "Stopping session");
                manager.stop(session);
            } else {
                MessageUtil.send(interaction, "You do not have an active session");
            }
        }
    }

    private static List<HighLevelComponent> wrap(LowLevelComponent... components) {
        return (List) Stream.of(components).map(ActionRow::of).toList();
    }


}
