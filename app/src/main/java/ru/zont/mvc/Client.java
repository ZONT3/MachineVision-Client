package ru.zont.mvc;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Client {
    static final int TIMEOUT = 30000;
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
        connected = true;

        Client.ip = ip;
        Client.port = port;
    }

    static String sendJsonForResult(String json) throws IOException {
        return sendJsonForResult(json, /*null,*/ TIMEOUT);
    }

//    static String sendJsonForResult(String json, String expectedResponse) throws IOException {
//        return sendJsonForResult(json, expectedResponse, TIMEOUT);
//    }

    static String sendJsonForResult(String json, /*String expectedResponse,*/  int timeout) throws IOException {
        connect(ip, port);
        if (socket==null) throw new IOException("Socket is null");
        if (!connected) throw new IOException("Client is not connected!");
        socket.setSoTimeout(timeout);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(json);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String response = in.readLine();
        if (response==null || response.equals(""))
            throw new IOException(String.format("Incorrect response (%s)", response));
        Log.d("CLIENT", "Answer:\n\t"+response);
        disconnect();
        return response;
    }

    private static void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {e.printStackTrace();}
        connected = false;
    }

}
