package com.socraticphoenix.sshcord.ssh;

import com.socraticphoenix.sshcord.Config;
import com.socraticphoenix.sshcord.util.SubstringOutputStream;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.Channel;
import org.javacord.api.entity.Deletable;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Session implements Runnable {
    private Logger logger;

    private SshClient client;
    private Creds creds;
    private InteractionPoint inter;

    private Config config;
    private SessionManager manager;
    private User user;

    private AtomicBoolean running = new AtomicBoolean(true);

    private Deque<byte[]> commands = new ConcurrentLinkedDeque<>();

    public Session(SshClient client, Creds creds, InteractionPoint inter, User user, Config config, SessionManager manager) {
        this.client = client;
        this.creds = creds;
        this.inter = inter;

        this.config = config;
        this.manager = manager;
        this.user = user;

        this.logger = LoggerFactory.getLogger("sesh-" + user.getDiscriminatedName() + "-" + creds.host + ":" + creds.port + "@" + creds.user);
    }

    @Override
    public void run() {
        Duration timeout = Duration.of(this.config.sshTimeout(), ChronoUnit.SECONDS);
        long seshTimeout = TimeUnit.SECONDS.toMillis(this.config.seshTimeout());

        String pass = this.creds.password;
        String user = this.creds.user;
        String host = this.creds.host;
        int port = this.creds.port;
        this.creds = null;

        try {
            logger.info("Connecting to SSH");
            try (ClientSession session = this.client.connect(user, host, port)
                    .verify(timeout).getSession()) {
                try {
                    this.logger.info("Authenticating");
                    session.addPasswordIdentity(pass);
                    pass = null;
                    session.auth().verify(timeout);


                    this.logger.info("Connecting to shell");
                    try (ClientChannel channel = session.createChannel(Channel.CHANNEL_SHELL)) {
                        SubstringOutputStream responseStream = new SubstringOutputStream(this.config.bufferSize());
                        channel.setOut(responseStream);
                        channel.setRedirectErrorStream(true);

                        try {
                            channel.open().verify(timeout);
                            try (OutputStream input = channel.getInvertedIn()) {
                                this.logger.info("Starting loop");
                                long prevCmd = System.currentTimeMillis();
                                long prevOutput = System.currentTimeMillis();
                                int prevSize = 0;

                                while (this.running.get()) {
                                    long curr = System.currentTimeMillis();

                                    //Output
                                    if (prevSize != responseStream.size() && (curr - prevOutput) > this.config.outputWait()) {
                                        int cutoff = prevSize;
                                        prevOutput = curr;

                                        SubstringOutputStream target = responseStream;
                                        if (responseStream.size() >= this.config.bufferLimit()) {
                                            SubstringOutputStream replacement = new SubstringOutputStream(this.config.bufferSize());
                                            channel.setOut(replacement);
                                            responseStream = replacement;
                                            prevSize = 0;
                                        } else {
                                            prevSize = responseStream.size();
                                        }
                                        String response = target.toString(cutoff, target.size(), StandardCharsets.UTF_8);
                                        updateOutput(response, host, port, user);
                                    }

                                    if (channel.isClosed() || channel.isClosing() ||
                                            channel.getChannelState().contains(ClientChannelEvent.CLOSED)) {
                                        sendError("Session closed by host", host, port, user);
                                        this.running.set(false);
                                    } else {
                                        //Input
                                        if (!this.commands.isEmpty()) {
                                            prevCmd = System.currentTimeMillis();
                                            byte[] next = this.commands.poll();
                                            input.write(next);
                                            input.flush();
                                        } else {
                                            if (curr - prevCmd > seshTimeout) {
                                                this.logger.info("Timing out, no commands received");
                                                sendError("Session timed out! (no recent commands received)", host, port, user);
                                                this.running.set(false);
                                            }
                                        }
                                    }
                                }

                                String excess = responseStream.toString(prevSize, responseStream.size(), StandardCharsets.UTF_8);
                                if (!excess.isEmpty()) {
                                    updateOutput(excess, host, port, user);
                                }
                            }
                        } catch (IOException e) {
                            this.logger.info("Failed to send command", e);
                            sendError("Failed to send command", host, port, user);
                        } finally {
                            channel.close(false);
                        }
                    } catch (IOException e) {
                        this.logger.info("Failed to connect to shell", e);
                        sendError("Failed to connect to shell", host, port, user);
                    }
                    this.logger.info("Stopping");
                } catch (IOException e) {
                    this.logger.info("Authentication failed", e);
                    sendError("Authentication failed", host, port, user);
                }

            } catch (IOException e) {
                this.logger.info("Failed to connect to host", e);
                sendError("Failed to connect to host", host, port, user);
            }
            this.logger.info("Exiting worker");
        } catch (Throwable ex) {
            this.logger.info("Uncaught exception: ", ex);
            sendError("Session closed unexpectedly", host, port, user);
        } finally {
            this.messages.forEach(m -> {
                try {
                    m.thenAccept(Deletable::delete).join();
                } catch (CompletionException | CancellationException e) {
                    this.logger.info("Failed to delete message", e);
                }
            });
            this.manager.stop(this);
        }
    }

    public void sendInput(String input) {
        this.commands.add(input.getBytes(StandardCharsets.UTF_8));
    }

    public void sendInput(byte[] input) {
        this.commands.add(input);
    }

    public void stop() {
        this.running.set(false);
    }

    public long userId() {
        return this.user.getId();
    }

    private CompletableFuture<Message> currentMsg;
    private StringBuilder currOutput = new StringBuilder();
    private Set<CompletableFuture<Message>> messages = new LinkedHashSet<>();

    public synchronized void updateOutput(String output, String host, int port, String username) {
        String potential = sanitize(currOutput + output, host, port, username);
        if (potential.length() <= this.config.msgLimit()) {
            currOutput.append(output);
            this.editOrSend(potential);
        } else {
            String[] lines = output.split("\n");
            List<String> messages = new ArrayList<>();
            StringBuilder builder = new StringBuilder(this.currOutput);

            for (String line : lines) {
                line = line + "\n";

                String test = sanitize(builder + line, host, port, username);
                if (test.length() <= this.config.msgLimit()) {
                    builder.append(line);
                } else {
                    messages.add(builder.toString());
                    builder = new StringBuilder();

                    while (sanitize(line, host, port, username).length() > this.config.msgLimit()) {
                        int breakpoint = 1;
                        for (; breakpoint < this.config.msgLimit() && breakpoint < line.length(); breakpoint++) {
                            if (sanitize(line.substring(0, breakpoint), host, port, username).length() > this.config.msgLimit()) {
                                breakpoint--;
                                break;
                            }
                        }
                        messages.add(line.substring(0, breakpoint));
                        line = line.substring(breakpoint);
                    }

                    builder.append(line);
                }
            }

            if (!builder.isEmpty()) {
                messages.add(builder.toString());
            }

            if (!messages.isEmpty()) {
                editOrSend(sanitize(messages.get(0), host, port, username));
                for (int i = 1; i < messages.size(); i++) {
                    CompletableFuture<Message> msg = sendMessage(sanitize(messages.get(i), host, port, username));
                    this.messages.add(msg);
                    if (i == messages.size() - 1) {
                        this.currentMsg = msg;
                        this.currOutput = new StringBuilder(messages.get(i));
                    }
                }
            }
        }
    }

    private void editOrSend(String content) {
        if (this.currentMsg != null) {
            this.currentMsg.thenAccept(msg -> msg.edit(content));
        } else {
            this.currentMsg = this.sendMessage(content);
            this.messages.add(this.currentMsg);
        }
    }

    private CompletableFuture<Message> sendMessage(String content) {
        if (this.inter.channel != null) {
            return this.inter.channel.sendMessage(content);
        } else {
            return this.user.sendMessage(content);
        }
    }

    private String sanitize(String output, String host, int port, String username) {
        return new MessageBuilder()
                .appendCode("text", "Discord User: " + this.user.getName() + ", Host: " + host + ":" + port + ", User: " + username + "\n\n" +
                        output.replace("`", "`\u200B"))
                .getStringBuilder().toString();
    }

    public void sendError(String error, String host, int port, String username) {
        MessageBuilder builder = new MessageBuilder();
        builder.append(this.user)
                .addEmbed(new EmbedBuilder()
                        .setColor(Color.RED)
                        .setTitle("An Error Occurred")
                        .setDescription(error)
                        .addField("Discord User", this.user.getDiscriminatedName())
                        .addInlineField("Host", host + ":" + port)
                        .addInlineField("User", username)
                );
        if (this.inter.channel != null) {
            builder.send(this.inter.channel)
                    .thenAccept(m -> m.deleteAfter(Duration.of(this.config.deleteWait(), ChronoUnit.SECONDS)));
        } else {
            builder.send(this.user)
                    .thenAccept(m -> m.deleteAfter(Duration.of(this.config.deleteWait(), ChronoUnit.SECONDS)));
        }
    }

    public InteractionPoint interactionPoint() {
        return this.inter;
    }

    public record Creds(String host, int port, String user, String password) {
    }

    public record InteractionPoint(Server server, TextChannel channel) {
    }

}
