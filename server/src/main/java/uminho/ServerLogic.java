package uminho;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ServerLogic {
    private DatagramSocket serverSocket;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServerLogic(double F, int A, int PORT) {
        int COUNTER;

        try {
            serverSocket = new DatagramSocket(PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for connections...\n");

            while(!serverSocket.isClosed()) {
                try {
                    byte[] receivedBuffer = new byte[256]; // buffer for the socket

                    // REQUEST PACKET FROM THE CLIENT, THROUGH THE FORWARDER
                    DatagramPacket requestPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
                    serverSocket.receive(requestPacket);

                    String message = new String(requestPacket.getData(), 0, requestPacket.getLength());

                    //GET CLIENT INFO FROM SOCKET
                    InetAddress clientAddress = requestPacket.getAddress();
                    int clientPort = requestPacket.getPort();

                    double fa = Math.pow( 10, -F /10 );
                    long sleep = Math.round(1000 / fa); // number of milliseconds for the thread.sleep method later

                    // CODE VERIFICATION
                    if( message.equals( "123456789" )) { // arbitrary check for a code, as explained in the report
                        System.out.println("Verified Client Connected from: " + clientAddress + ":" + clientPort);

                        // Respond to forwarder
                        InetAddress forwarderAddress = requestPacket.getAddress();
                        int forwarderPort = requestPacket.getPort();

                        PDU ack_pdu = new PDU( F, A, null); // creates the ACK PDU, also explained in the report
                        sendMessage( ack_pdu, forwarderAddress, forwarderPort );
                        System.out.println("\n" + LocalTime.now().format(formatter) + " : ACK message sent. \n");

                        String[] contentsFile = readFile(); // method that read the whole file
                        COUNTER = 0; // counter for the section of the file that it is currently on

                        while( COUNTER < contentsFile.length  ) {
                            PDU pdu = new PDU( F, A, contentsFile[COUNTER].trim() ); // sends the sentence with the F and A parameters in a PDU
                            sendMessage(pdu, forwarderAddress, forwarderPort); // packages the PDU into a UDP packet
                            System.out.println(LocalTime.now().format(formatter) + " : MESSAGE -> " + pdu.getMessage() );
                            COUNTER++;
                            Thread.sleep( sleep ); // stops temporarily the thread to insure the sampling rate (fa)
                        }

                        sendMessage(null, forwarderAddress, forwarderPort); // sends an empty message to the client, also explained in the report
                        System.out.println( "\n" + LocalTime.now().format(formatter) + " : END OF FILE MESSAGE SENT" );

                        serverSocket.close();
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
        oos.writeObject(pdu); // converts the bytes into a PDU instance
        oos.flush();
        byte[] sendBuffer = baos.toByteArray();

        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, forwarderAddress, forwarderPort);
        serverSocket.send(sendPacket);
    }

    private static String[] readFile() {
        String[] sentences = null;

        try {
            StringBuilder contentBuilder = new StringBuilder();

            InputStream inputStream = ServerLogic.class.getClassLoader().getResourceAsStream("class_transcript.txt"); // finds the file

            assert inputStream != null;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;

                while( ( line = reader.readLine()) != null ) {
                    contentBuilder.append(line).append(" ");
                }
            }

            String content = contentBuilder.toString();
            sentences = content.split( "(?<=\\.)\\s+" ); // Split by sentence-ending dot followed by space or end of string

        } catch (IOException e) {
            System.err.println("Error reading main file: " + e.getMessage());
        }
        return sentences;
    }

}
