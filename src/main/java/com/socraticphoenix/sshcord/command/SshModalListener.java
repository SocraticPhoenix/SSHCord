package com.socraticphoenix.sshcord.command;

import com.socraticphoenix.sshcord.ssh.Session;
import com.socraticphoenix.sshcord.ssh.SessionManager;
import com.socraticphoenix.sshcord.util.MessageUtil;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.ModalSubmitEvent;
import org.javacord.api.interaction.ModalInteraction;
import org.javacord.api.listener.interaction.ModalSubmitListener;

import java.util.Optional;

public class SshModalListener implements ModalSubmitListener {
    private SessionManager manager;

    public SshModalListener(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void onModalSubmit(ModalSubmitEvent event) {
        ModalInteraction inter = event.getModalInteraction();

        User user = inter.getUser();
        String targetId = "com.socraticphoenix.sshcord:ssh-" + user.getDiscriminatedName();

        if (targetId.equals(event.getModalInteraction().getCustomId())) {
            if (this.manager.findSession(user) != null) {
                MessageUtil.send(inter, "You already have a session open, close it first");
            } else {

                Optional<String> host = inter.getTextInputValueByCustomId("host");
                Optional<String> port = inter.getTextInputValueByCustomId("port");
                Optional<String> username = inter.getTextInputValueByCustomId("user");
                Optional<String> pass = inter.getTextInputValueByCustomId("pass");

                if (host.isEmpty()) {
                    MessageUtil.send(inter, "Hostname is required");
                } else if (username.isEmpty()) {
                    MessageUtil.send(inter, "Username is required");
                } else if (pass.isEmpty()) {
                    MessageUtil.send(inter, "Password is required");
                } else {
                    try {
                        int portNum = port.map(Integer::parseInt).orElse(22);
                        Session.Creds creds = new Session.Creds(host.get(), portNum, username.get(), pass.get());
                        Session.InteractionPoint interPoint = new Session.InteractionPoint(inter.getServer().orElse(null),
                                inter.getChannel().orElse(null));

                        MessageUtil.send(inter, "Creating session");
                        this.manager.create(creds, interPoint, user);
                    } catch (NumberFormatException e) {
                        MessageUtil.send(inter, port.orElse("null") + " is not a number");
                    }
                }
            }
        }
    }

}
