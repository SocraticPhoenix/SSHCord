package com.socraticphoenix.sshcord.util;

import com.socraticphoenix.sshcord.App;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.interaction.InteractionBase;

import java.util.concurrent.TimeUnit;

public class MessageUtil {

    public static void send(InteractionBase interaction, String text) {
        interaction.createImmediateResponder()
                .setContent(text)
                .setFlags(MessageFlag.EPHEMERAL)
                .respond()
                .thenAccept(responder -> interaction.getApi().getThreadPool().runAfter(responder::delete,
                        App.config().deleteWait(), TimeUnit.SECONDS));
    }


}
