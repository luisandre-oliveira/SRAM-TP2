package uminho;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerMain {
    private static final String NAME_PROPERTIES_FILE = "server.properties";

    private static int F; // FREQUENCY RELATED
    private static int A; // DELAY
    private static int PORT;

    public static void main( String[] args ) {
        Properties properties = loadProperties();
        processProperties( properties );
        new ServerLogic(F,A,PORT);
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
                System.err.println( "\nError loading config file: " + file.getAbsolutePath() + e.getMessage() );
            }
        }

        if ( !loaded ) {
            try ( InputStream input = ServerMain.class.getResourceAsStream( "/" + NAME_PROPERTIES_FILE ) ) {
                if ( input == null ) {
                    System.err.println( "\nEmbedded config file not found: " + NAME_PROPERTIES_FILE + "\n");
                    System.exit( 1 );
                }
                properties.load( input );
            } catch ( IOException e ) {
                System.err.println( "\nError loading embedded config file: " + NAME_PROPERTIES_FILE + e.getMessage());
                System.exit( 1 );
            }
        }

        return properties;
    }

    private static void processProperties( Properties properties) {
        try {
            F = Integer.parseInt( properties.getProperty( "F" ) );
            A = Integer.parseInt( properties.getProperty( "A" ) );
            PORT = Integer.parseInt( properties.getProperty( "PORT") );

            String sb = "Loaded values:" + "\n"
                    + "\tF - " + F + "\n"
                    + "\tA - " + A + "\n"
                    + "\tPORT - " + PORT + "\n";

            System.out.println( sb );
        } catch ( Exception e ) {
            System.err.println( "\nError getting data from config file." + e.getMessage() );
            System.exit( 1 );
        }
    }
}
