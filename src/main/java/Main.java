import proxy.ProxyHandler;
import utils.ExecutorServiceUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

/**
 * @author Angel Maciel
 * @version 1.0
 */
public class Main {

   public static final int PORT = 8081;
   public static final Set<String> OPTIONS = Set.of("-p", "--port");

   public static void startCachingProxy(int port) {
      setUpProxy(port);
   }

   public static void startCachingProxy() {
      setUpProxy(PORT);
   }

   private static void setUpProxy(int port) {
      try (ServerSocket serverSocket = new ServerSocket(port)) {
         while (!serverSocket.isClosed()) {
            System.out.println("Proxy listening port: " + port);
            Socket client = serverSocket.accept();
            ExecutorServiceUtil.start(() -> new ProxyHandler(client));
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public static void main(String[] args) {
      int port;
      if (args.length == 0) {
         startCachingProxy();
         return;
      }
      try {
         for (int i = 0; i < args.length; i++) {
            if (args.length > 2) {
               System.err.println("Too much arguments. There just be 2: -p <number> or --port <number>.");
               System.exit(1);
               return;
            }
            if (!OPTIONS.contains(args[i].toLowerCase())) {
               System.err.println("Argument not found.");
               System.exit(1);
               return;
            }

            port = Integer.parseInt(args[1]);

            if (port < 1024 || port > 49151) {
               System.err.println("Permissions denied for that port value.");
               System.exit(1);
               return;
            }

            startCachingProxy(port);
         }
      } catch (NumberFormatException e) {
         System.err.println("The port must be a numeric value.");
         System.exit(1);
      }
   }
}
