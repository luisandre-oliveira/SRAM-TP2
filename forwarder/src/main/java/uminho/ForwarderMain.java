package uminho;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ForwarderMain {
    private static final String NAME_PROPERTIES_FILE = "forwarder.properties";

    private static int SERVER_PORT;
    private static int FORWARDER_PORT;
    private static int CLIENT_PORT;
    private static String HOSTNAME;

    private static int N; // Number of frames before pausing
    private static int P;  // Pause duration in seconds
    private static int M; // Interval in seconds to drop a frame

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ForwarderMain() {
        forwarderLogic();
    }

    private void forwarderLogic() {

        int COUNTER = 0;

        try ( DatagramSocket forwarderSocket = new DatagramSocket(FORWARDER_PORT) ) {
            byte[] receivedBuffer = new byte[1024];

            System.out.println("Forwarder is running...");

            LocalTime startTime = LocalTime.now();

            while (true) {
                // Receive from client
                DatagramPacket receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
                forwarderSocket.receive(receivedPacket);

                String msg = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                InetAddress senderAddress = receivedPacket.getAddress();
                int senderPort = receivedPacket.getPort();

                if (senderPort == 12345) {
                    // Message from client → forward to server
                    System.out.println( LocalTime.now().format(formatter) + " : CLIENT -> SERVER = " + msg.length() + " bytes");

                    DatagramPacket forwardPacket = new DatagramPacket( receivedPacket.getData(), receivedPacket.getLength(), senderAddress, 9876 );
                    forwarderSocket.send(forwardPacket);

                } else if (senderPort == 9876) {

                    LocalTime currentTime = LocalTime.now();
                    Duration timeDiff = Duration.between(startTime, currentTime);

                    if( (int) timeDiff.getSeconds() > M ) {
                        startTime = currentTime;
                        System.out.println(LocalTime.now().format(formatter) + " : PACKET DROPPED");
                        continue;
                    }

                    // Message from server → forward to client
                    System.out.println(LocalTime.now().format(formatter) + " : SERVER -> CLIENT = " + msg.length() + " bytes");

                    if( COUNTER == N ) {
                        System.out.println(LocalTime.now().format(formatter) + " : FORWARDER ON PAUSE");
                        COUNTER = 0;
                        Thread.sleep(1000L * P);
                    } else {
                        COUNTER++;
                    }

                    DatagramPacket returnPacket = new DatagramPacket( receivedPacket.getData(), receivedPacket.getLength(), senderAddress, 12345 );
                    forwarderSocket.send(returnPacket);

                }
            }
        } catch (Exception e) {
            System.err.println( "\n " + e.getMessage() );
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
                System.err.println( "Error loading config file: " + file.getAbsolutePath()  + e.getMessage() );
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
                System.err.println( "Error loading embedded config file: " + NAME_PROPERTIES_FILE + e.getMessage());
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
            N = Integer.parseInt( properties.getProperty( "N") );
            P = Integer.parseInt( properties.getProperty( "P") );
            M = Integer.parseInt( properties.getProperty( "M") );

            String sb = "Loaded values:" + "\n"
                    + "\tSERVER_PORT - " + SERVER_PORT + "\n"
                    + "\tFORWARDER_PORT - " + FORWARDER_PORT + "\n"
                    + "\tCLIENT_PORT - " + CLIENT_PORT + "\n"
                    + "\tHOSTNAME - " + HOSTNAME + "\n"
                    + "\tN - " + N + "\n"
                    + "\tP - " + P + "\n"
                    + "\tM - " + M + "\n";

            System.out.println( sb );
        } catch ( Exception e ) {
            System.err.println( "\nError getting data from config file.\n"  + e.getMessage() );
            System.exit( 1 );
        }
    }
}