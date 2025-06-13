package uminho;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Objects;

public class ServerLogic {
    private DatagramSocket serverSocket;
    private int COUNTER;

    public ServerLogic(int F, int A, int PORT) {
        try {
            serverSocket = new DatagramSocket(PORT);
            System.out.println("\nServer started on port " + PORT);
            System.out.println("Waiting for connections...\n");

            while(true) {
                try {
                    byte[] receivedBuffer = new byte[256];

                    // REQUEST PACKET FROM THE CLIENT, THROUGH THE FORWARDER
                    DatagramPacket requestPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
                    serverSocket.receive(requestPacket);

                    String message = new String(requestPacket.getData(), 0, requestPacket.getLength());

                    //GET CLIENT INFO FROM SOCKET
                    InetAddress clientAddress = requestPacket.getAddress();
                    int clientPort = requestPacket.getPort();

                    double fa = Math.pow( 10, -F );
                    long sleep = Math.round(1000 / fa);

                    // CODE VERIFICATION
                    if( message.equals( "123456789" )) {
                        System.out.println("Verified Client Connected from: " + clientAddress + ":" + clientPort + "\n");

                        // TODO: ADD THE READING LOGIC
                        // TODO: * ADD THE SENDING OF SAID DATA

                        // Respond to forwarder
                        InetAddress forwarderAddress = requestPacket.getAddress();
                        int forwarderPort = requestPacket.getPort();

                        String[] contentsFile = readFile( "class_transcript.txt" );
                        COUNTER = 0;

                        while( COUNTER < contentsFile.length  ) {
                            PDU pdu = new PDU( F, A, contentsFile[COUNTER].trim() );
                            sendMessage(pdu, forwarderAddress, forwarderPort);
                            System.out.println( "MESSAGE -> " + pdu.getMessage() );
                            COUNTER++;
                            Thread.sleep( sleep );
                        }

                        sendMessage(null, forwarderAddress, forwarderPort);
                        System.out.println( "\nEND OF FILE MESSAGE SENT" );

                    }

                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SocketException e) {
            System.err.println("Socket error: " + e.getMessage());
        }
    }

    private void sendMessage(PDU pdu, InetAddress forwarderAddress, int forwarderPort) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(pdu);
        oos.flush();
        byte[] sendBuffer = baos.toByteArray();

        //byte[] sendBuffer;


        //sendBuffer = pdu.toString().getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, forwarderAddress, forwarderPort);
        serverSocket.send(sendPacket);
    }

    private static String[] readFile(String filePath) {
        String[] sentences = null;

        try {
            StringBuilder contentBuilder = new StringBuilder();

            InputStream inputStream = ServerLogic.class.getClassLoader().getResourceAsStream(filePath);

            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;

                while( ( line = reader.readLine()) != null ) {
                    contentBuilder.append(line).append(" ");
                }
            }

            String content = contentBuilder.toString();
            sentences = content.split( "(?<=\\.)\\s+" );

            /*long delay = 1000L / fa;  // Milliseconds between sentences
            int counter = 0;

            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (!sentence.isEmpty()) {
                    System.out.println(sentence);
                    counter++;
                    Thread.sleep(delay);
                }
            }*/

        } catch (IOException e) {
            System.err.println("Error reading main file: " + e.getMessage());
        }
        return sentences;
    }

}
