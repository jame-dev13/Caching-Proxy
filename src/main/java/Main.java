import cache.LRUCache;
import proxy.ProxyHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
   private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

   public static void main(String[] args) {
      try(ServerSocket serverSocket = new ServerSocket(8081)){
         while(true){
            System.out.println("Proxy listening port 8081");
            Socket client = serverSocket.accept();
            EXECUTOR_SERVICE.submit(() -> new ProxyHandler(client));
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
