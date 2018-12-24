package ru.zont.mvc;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

class Client {
    static final int TIMEOUT = 30000;
    private static Socket socket;

    private static String ip;
    private static int port;

    private static ArrayList<Long> queue = new ArrayList<>();

    static void setup(String ip, int port) {
        Client.ip = ip;
        Client.port = port;
    }

    static Throwable tryConnection(String ip, int port) {
        Throwable result = null;
        try {
            connect(ip, port);
        } catch (IOException e) {
            result = e;
        } finally {
            disconnect();
        }
        return result;
    }

    private static void connect(String ip, int port) throws IOException {
        long sessionId = new Random().nextLong();
        queue.add(sessionId);
        while (!(queue.size() <= 0 || queue.get(0).equals(sessionId)))
            try { Thread.sleep(200); } catch (InterruptedException ignored) { }

        socket = new Socket(ip, port);
    }

    static String sendJsonForResult(String json) throws IOException {
        return sendJsonForResult(json, /*null,*/ TIMEOUT);
    }

//    static String sendJsonForResult(String json, String expectedResponse) throws IOException {
//        return sendJsonForResult(json, expectedResponse, TIMEOUT);
//    }

    static String sendJsonForResult(String json, /*String expectedResponse,*/  int timeout) throws IOException {
        try {
            connect(ip, port);
            if (socket == null) throw new IOException("Socket is null");
            socket.setSoTimeout(timeout);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(json);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            if (response == null || response.equals(""))
                throw new IOException(String.format("Incorrect response (%s)", response));
            Log.d("CLIENT", "Answer:\n\t" + response);
            return response;
        } catch (IOException e) {
            throw e;
        } finally {
            disconnect();
        }
    }

    private static void disconnect() {
        try {
            socket.close();
        } catch (Exception ignored) { }
        if (queue.size() > 0) queue.remove(0);
    }

}
