package com.josemarcellio.dummy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Dummy {
    private static final int LOCAL_PORT = 25565;
    private static final String TARGET_SERVER = "dummy.server";
    private static final int TARGET_PORT = 25565;
    private static final Logger logger = Logger.getLogger(Dummy.class.getName());

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(LOCAL_PORT)) {
            logger.info("Proxy server started on port " + LOCAL_PORT);

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Accepted connection from " + clientSocket.getRemoteSocketAddress());
                    executor.execute(new ProxyHandler(clientSocket, TARGET_SERVER, TARGET_PORT));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error accepting connection", e);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error starting server", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
