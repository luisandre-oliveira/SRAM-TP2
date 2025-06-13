package uminho;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Properties;

public class ClientMain {
    private static final String NAME_PROPERTIES_FILE = "client.properties";

    private static int PORT;
    private static int FORWARDER_PORT;
    private static String HOSTNAME;

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

                System.out.println("Client sent the code: " + message);

                byte[] receivedData = new byte[1024];
                DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);

                do {
                    clientSocket.receive(receivedPacket);

                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(receivedPacket.getData()));
                    PDU pdu = (PDU) in.readObject();
                    in.close();

                    if( pdu != null ) {
                        System.out.println("MESSAGE -> " + pdu.getMessage());
                    } else {
                        System.out.println("\nEND OF TRANSMISSION");
                    }

                } while ( receivedPacket.getData() != null );

                // TODO: maybe do an app that prints the transcript as it's being read

                clientSocket.close();

                /*while(true) {
                    byte[] bufferResponse = new byte[2048 * 15];
                    DatagramPacket response = new DatagramPacket(bufferResponse, bufferResponse.length);
                    clientSocket.receive(response);
                    System.out.println("Resposta recebida");
                }*/

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
                System.err.println( "Error loading config file: " + file.getAbsolutePath() );
                e.printStackTrace();
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
                System.err.println( "Error loading embedded config file: " + NAME_PROPERTIES_FILE );
                e.printStackTrace();
                System.exit( 1 );
            }
        }


        return properties;
    }

    private static void processProperties( Properties properties) {
        try {
            PORT = Integer.parseInt( properties.getProperty( "PORT") );
            FORWARDER_PORT = Integer.parseInt( properties.getProperty( "FORWARDERPORT") );
            HOSTNAME = String.copyValueOf(properties.getProperty( "HOSTNAME" ).toCharArray());

            String sb = "Loaded values:" + "\n"
                    + "\tPORT - " + PORT + "\n"
                    + "\tFORWARDER_PORT - " + FORWARDER_PORT + "\n"
                    + "\tHOSTNAME - " + HOSTNAME + "\n";

            System.out.println( sb );
        } catch ( Exception e ) {
            System.err.println( "\nError getting data from config file.\n" );
            e.printStackTrace();
            System.exit( 1 );
        }
    }
}
