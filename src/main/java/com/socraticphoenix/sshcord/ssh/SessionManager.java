package com.socraticphoenix.sshcord.ssh;

import com.socraticphoenix.sshcord.Config;
import org.apache.sshd.client.SshClient;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private SessionExecutor executor = new SessionExecutor();
    private Map<Long, Session> sessions = new ConcurrentHashMap<>();

    private Config config;

    private DiscordApi api;
    private SshClient sshClient;

    public SessionManager(Config config, DiscordApi api) {
        this.config = config;
        this.api = api;
    }

    public void stop(Session session) {
        this.sessions.computeIfPresent(session.userId(), (key, curr) -> curr == session ? null : curr);
        session.stop();
    }

    public void start() {
        this.sshClient = SshClient.setUpDefaultClient();
        this.sshClient.start();
    }

    public void stop() {
        this.sessions.values().forEach(Session::stop);
        this.executor.shutdown();
        this.sshClient.stop();
    }

    public Session findSession(User user) {
        return this.sessions.get(user.getId());
    }

    public void create(Session.Creds creds, Session.InteractionPoint inter, User user) {
        Session session = new Session(this.sshClient, creds, inter, user, this.config, this);
        this.sessions.put(user.getId(), session);
        this.executor.start(session);
    }

    public DiscordApi api() {
        return this.api;
    }
}
