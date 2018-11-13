package ru.zont.mvc;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Client {
    private static final int TIMEOUT = 60000;
    private static Socket socket;
    private static boolean connected = false;

    private static String ip;
    private static int port;

    static void setup(String ip, int port) {
        Client.ip = ip;
        Client.port = port;
    }

    static void establish() throws IOException {
        connect(ip, port);
        disconnect();
    }

    private static void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        socket.setSoTimeout(TIMEOUT);
        connected = true;

        Client.ip = ip;
        Client.port = port;
    }

    static String sendJsonForResult(String json) throws IOException {
        connect(ip, port);
        if (socket==null) throw new IOException("Socket is null");
        if (!connected) throw new IOException("Client is not connected!");

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(json);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String answer = in.readLine();
        if (answer==null || answer.equals("")) throw new IOException("Incorrect response" + (answer == null ? ": null" : ": blank line"));
        Log.d("CLIENT", "Answer:\n\t"+answer);
        disconnect();
        return answer;
    }

    private static void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {e.printStackTrace();}
        connected = false;
    }

    static boolean isConnected() { return connected; }
}
