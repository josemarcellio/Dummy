package com.josemarcellio.dummy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProxyHandler implements Runnable {
    private final Socket clientSocket;
    private final String targetServer;
    private final int targetPort;
    private static final Logger logger = Logger.getLogger(ProxyHandler.class.getName());

    public ProxyHandler(Socket clientSocket, String targetServer, int targetPort) {
        this.clientSocket = clientSocket;
        this.targetServer = targetServer;
        this.targetPort = targetPort;
    }

    @Override
    public void run() {
        try (Socket serverSocket = new Socket(targetServer, targetPort);
             InputStream clientIn = clientSocket.getInputStream();
             OutputStream clientOut = clientSocket.getOutputStream();
             InputStream serverIn = serverSocket.getInputStream();
             OutputStream serverOut = serverSocket.getOutputStream()) {

            logger.info("Proxying data between " + clientSocket.getRemoteSocketAddress() + " and " + targetServer + ":" + targetPort);

            Thread clientToServer = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = clientIn.read(buffer)) != -1) {
                        serverOut.write(buffer, 0, bytesRead);
                        serverOut.flush();
                    }
                } catch (SocketException e) {
                    logger.log(Level.INFO, "Client connection closed: " + e.getMessage());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error forwarding data from client to server", e);
                } finally {
                    closeSocket();
                }
            });

            Thread serverToClient = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = serverIn.read(buffer)) != -1) {
                        clientOut.write(buffer, 0, bytesRead);
                        clientOut.flush();
                    }
                } catch (SocketException e) {
                    logger.log(Level.INFO, "Server connection closed: " + e.getMessage());
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error forwarding data from server to client", e);
                } finally {
                    closeSocket();
                }
            });

            clientToServer.start();
            serverToClient.start();

            clientToServer.join();
            serverToClient.join();
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Error in proxy handler", e);
        } finally {
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (!clientSocket.isClosed()) {
                clientSocket.close();
                logger.info("Client socket closed.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing client socket", e);
        }
    }
}
