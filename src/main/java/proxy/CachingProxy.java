package proxy;

import utils.ExecutorServiceUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;


/**
 * Class that starts the service Caching proxy, it uses a {@link ServerSocket}
 * that is listening some port to perform his labor.
 */
public final class CachingProxy {
   private static final int PORT = 8081;
   private static final Logger log = Logger.getLogger(CachingProxy.class.getName());

   /**
    * Sets up the main logic for starting a server, in this case, a proxy that
    * waits for http request.
    * @param port the port to be listened.
    */
   private static void setUp(final int port){
      try(final ServerSocket server = new ServerSocket(port)){
         System.out.println("*** Caching proxy listening on port: " + port + " ***");
         while (!server.isClosed()){
            System.out.println("Waiting....");
            final Socket client = server.accept();
            ExecutorServiceUtil.start(() -> new ProxyHandler(client));
         }
      }catch (final IOException e){
         log.severe("Cannot start caching proxy server." + e.getMessage());
      }
   }

   /**
    * Start the caching proxy on the port given by default {@link #PORT}
    */
   public static void startProxy(){
      setUp(PORT);
   }

   /**
    * Start the caching proxy on the given port.
    * @param port the port to be listened.
    */
   public static void startProxy(final int port){
      setUp(port);
   }
}
