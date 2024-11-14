package com.socraticphoenix.sshcord.ssh;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SessionExecutor {
    private ExecutorService service = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("virtual-ssh").factory());
    private boolean active = true;

    public void start(Session session) {
        service.submit(session);
    }

    public void shutdown() {
        this.active = false;
        service.shutdown();
    }

    public boolean active() {
        return this.active;
    }
}
