package com.socraticphoenix.sshcord.command;

import com.socraticphoenix.sshcord.ssh.Session;
import com.socraticphoenix.sshcord.ssh.SessionManager;
import com.socraticphoenix.sshcord.util.StringUtil;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

public class SshMessageListener implements MessageCreateListener {
    private SessionManager manager;

    public SshMessageListener(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        event.getMessageAuthor().asUser().ifPresent(user -> {
            Session session = this.manager.findSession(user);
            Session.InteractionPoint inter = new Session.InteractionPoint(event.getServer().orElse(null), event.getChannel());
            if (session != null && inter.equals(session.interactionPoint())) {
                event.getMessage().delete();
                String msg = event.getMessageContent();
                session.sendInput(StringUtil.deEscape(msg) + "\n");
            }
        });
    }

}
