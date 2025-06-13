package uminho;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Properties;

public class ForwarderMain {
    private static final String NAME_PROPERTIES_FILE = "forwarder.properties";

    private static int SERVER_PORT;
    private static int FORWARDER_PORT;
    private static int CLIENT_PORT;
    private static String HOSTNAME;

    private static final int N = 10; // Number of frames before pausing
    private static final int P = 2;  // Pause duration in seconds
    private static final int M = 30; // Interval in seconds to drop a frame

    public ForwarderMain() {
        forwarderLogic();
    }

    private void forwarderLogic() {
        try {
            DatagramSocket forwarderSocket = new DatagramSocket(FORWARDER_PORT);
            byte[] receivedBuffer = new byte[1024];

            System.out.println("Forwarder is running...");

            while (true) {
                // Receive from client
                DatagramPacket receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
                forwarderSocket.receive(receivedPacket);

                //System.out.println("BRUH" + Arrays.toString(receivedPacket.getData()));

                String msg = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                InetAddress senderAddress = receivedPacket.getAddress();
                int senderPort = receivedPacket.getPort();

                if (senderPort == 12345) {
                    // Message from client → forward to server
                    System.out.println("Forwarder got message from client: " + msg);

                    DatagramPacket forwardPacket = new DatagramPacket(
                            receivedPacket.getData(), receivedPacket.getLength(), senderAddress, 9876);
                    forwarderSocket.send(forwardPacket);

                    System.out.println("Forwarder sent to server.");
                } else if (senderPort == 9876) {
                    // Message from server → forward to client
                    System.out.println("Forwarder got response from server: " + msg);

                    DatagramPacket returnPacket = new DatagramPacket(
                            receivedPacket.getData(), receivedPacket.getLength(), senderAddress, 12345);
                    forwarderSocket.send(returnPacket);

                    System.out.println("Forwarder sent response to client.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Properties properties = loadProperties();
        processProperties( properties );

        try {
            new ForwarderMain();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                System.err.println( "Error loading config file: " + file.getAbsolutePath() );
                e.printStackTrace();
            }
        }

        if ( !loaded ) {
            try ( InputStream input = ForwarderMain.class.getResourceAsStream( "/" + NAME_PROPERTIES_FILE ) ) {
                if ( input == null ) {
                    System.err.println( "Embedded config file not found: " + NAME_PROPERTIES_FILE );
                    System.exit( 1 );
                }
                properties.load( input );
            } catch ( IOException e ) {
                System.err.println( "Error loading embedded config file: " + NAME_PROPERTIES_FILE );
                e.printStackTrace();
                System.exit( 1 );
            }
        }


        return properties;
    }

    private static void processProperties( Properties properties) {
        try {
            SERVER_PORT = Integer.parseInt( properties.getProperty( "SERVERPORT") );
            FORWARDER_PORT = Integer.parseInt( properties.getProperty( "FORWARDERPORT") );
            CLIENT_PORT = Integer.parseInt( properties.getProperty( "CLIENTPORT") );
            HOSTNAME = String.copyValueOf(properties.getProperty( "HOSTNAME").toCharArray());

            String sb = "Loaded values:" + "\n"
                    + "\tSERVER_PORT - " + SERVER_PORT + "\n"
                    + "\tFORWARDER_PORT - " + FORWARDER_PORT + "\n"
                    + "\tCLIENT_PORT - " + CLIENT_PORT + "\n"
                    + "\tHOSTNAME - " + HOSTNAME + "\n";

            System.out.println( sb );
        } catch ( Exception e ) {
            System.err.println( "\nError getting data from config file.\n" );
            e.printStackTrace();
            System.exit( 1 );
        }
    }
}