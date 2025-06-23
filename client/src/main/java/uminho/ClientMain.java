package uminho;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ClientMain {
    private static final String NAME_PROPERTIES_FILE = "client.properties";

    private static int PORT;
    private static int FORWARDER_PORT;
    private static String HOSTNAME;

    private static List<DatagramPacket> BUFFER;
    private static int MAX_SIZE_BUFFER;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClientMain() {
        clientLogic();
    }

    public static void main( String[] args ) {
        Properties properties = loadProperties();
        processProperties( properties );

        try {
            new ClientMain();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void clientLogic() {
        Runnable runnable = () -> {
            try {
                DatagramSocket clientSocket = new DatagramSocket(PORT);

                String message = "123456789";
                byte[] sendData = message.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(HOSTNAME), FORWARDER_PORT);
                clientSocket.send(sendPacket);

                System.out.println(LocalTime.now().format(formatter) + " : Client sent the code: " + message + "\n");

                byte[] receivedData = new byte[1024];
                DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);

                boolean exit_loop = false;
                ObjectInputStream in;

                // ACK to get the F and A parameters from the server
                DatagramPacket ackPacket = new DatagramPacket(receivedData, receivedData.length);
                clientSocket.receive(ackPacket);
                in = new ObjectInputStream(new ByteArrayInputStream(ackPacket.getData()));
                PDU ack_pdu = (PDU) in.readObject();
                in.close();

                MAX_SIZE_BUFFER = (int) ( (1 / ( Math.pow( 10,-ack_pdu.getF() ) ) ) * ack_pdu.getA());
                long sleep = Math.round( 1000 / ( Math.pow( 10,-ack_pdu.getF() ) ) );
                // System.out.println(MAX_SIZE_BUFFER);

                BUFFER = new ArrayList<>();

                // start of the main logic of the client
                do {
                    clientSocket.receive(receivedPacket);

                    if( BUFFER.size() < MAX_SIZE_BUFFER ) {
                        BUFFER.add(receivedPacket);
                    } else {
                        BUFFER.remove(0);
                        BUFFER.add(receivedPacket);
                    }

                    in = new ObjectInputStream(new ByteArrayInputStream(receivedPacket.getData()));
                    PDU pdu = (PDU) in.readObject();

                    in.close();

                    if( pdu != null ) {
                        System.out.println(LocalTime.now().format(formatter) + " : MESSAGE -> " + pdu.getMessage());
                        // System.out.println("BUFFER SIZE USED = " + BUFFER.size());
                        Thread.sleep( sleep ); // sleep to guarantee fa frames/second
                    } else {
                        System.out.println("\n" + LocalTime.now().format(formatter) + " : END OF TRANSMISSION");
                        exit_loop = true;
                    }

                    BUFFER.remove(receivedPacket);

                } while ( !exit_loop );

                clientSocket.close();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        new Thread(runnable).start();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        boolean loaded = false;

        File file = new File( System.getProperty( "user.dir" ), NAME_PROPERTIES_FILE );

        if ( file.exists() ) {
            try ( FileInputStream input = new FileInputStream( file ) ) {
                properties.load( input );
                loaded = true;
            } catch ( IOException e ) {
                System.err.println( "Error loading config file: " + file.getAbsolutePath() + e.getMessage() );
            }
        }

        if ( !loaded ) {
            try ( InputStream input = ClientMain.class.getResourceAsStream( "/" + NAME_PROPERTIES_FILE ) ) {
                if ( input == null ) {
                    System.err.println( "Embedded config file not found: " + NAME_PROPERTIES_FILE );
                    System.exit( 1 );
                }
                properties.load( input );
            } catch ( IOException e ) {
                System.err.println( "Error loading embedded config file: " + NAME_PROPERTIES_FILE + e.getMessage() );
                System.exit( 1 );
            }
        }


        return properties;
    }

    private static void processProperties( Properties properties) {
        try {
            PORT = Integer.parseInt( properties.getProperty( "PORT") );
            FORWARDER_PORT = Integer.parseInt( properties.getProperty( "FORWARDERPORT" ) );
            HOSTNAME = String.copyValueOf(properties.getProperty( "HOSTNAME" ).toCharArray());

            String sb = "Loaded values:" + "\n"
                    + "\tPORT - " + PORT + "\n"
                    + "\tFORWARDER_PORT - " + FORWARDER_PORT + "\n"
                    + "\tHOSTNAME - " + HOSTNAME + "\n";

            System.out.println( sb );
        } catch ( Exception e ) {
            System.err.println( "\nError getting data from config file.\n"  + e.getMessage());
            System.exit( 1 );
        }
    }
}
