package com.socraticphoenix.sshcord;

import canaryprism.slavacord.CommandHandler;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.socraticphoenix.sshcord.command.SshCommand;
import com.socraticphoenix.sshcord.command.SshMessageListener;
import com.socraticphoenix.sshcord.command.SshModalListener;
import com.socraticphoenix.sshcord.ssh.SessionManager;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;

public class App {
    public static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static SessionManager manager;
    private static Config config;

    public static void main(String[] args) {
        Gson gson = new Gson();
        LOGGER.info("Loading config");
        try {
            Config config = gson.fromJson(Files.newBufferedReader(Paths.get("config.json")), Config.class);
            App.config = config;
            LOGGER.info("Connecting to discord");
            try {
                DiscordApi api = new DiscordApiBuilder()
                        .setToken(config.token())
                        .setAllIntents()
                        .login().join();

                LOGGER.info("Bot link: " + api.createBotInvite(new PermissionsBuilder()
                        .setAllAllowed()
                        .build()));

                LOGGER.info("Starting session manager");
                SessionManager manager = new SessionManager(config, api);
                App.manager = manager;
                manager.start();

                LOGGER.info("Registering listeners");
                api.addModalSubmitListener(new SshModalListener(manager));
                api.addMessageCreateListener(new SshMessageListener(manager));

                LOGGER.info("Registering commands");
                CommandHandler handler = new CommandHandler(api);
                handler.register(SshCommand.class, true);

                LOGGER.info("Setup complete");
                Scanner scanner = new Scanner(System.in);
                boolean running = true;
                while (running) {
                    String next = scanner.next();
                    switch (next) {
                        case "exit" -> running = false;
                        default -> LOGGER.info("Unknown command: {}", next);
                    }
                }
                LOGGER.info("Shutting down");
                manager.stop();
                api.disconnect().join();
                LOGGER.info("Shutdown");
            } catch (CompletionException | CancellationException e) {
                LOGGER.error("Failed to connect to discord", e);
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    public static SessionManager sessionManager() {
        return manager;
    }

    public static Config config() {
        return config;
    }

}
