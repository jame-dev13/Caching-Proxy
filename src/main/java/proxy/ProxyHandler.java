package proxy;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class ProxyHandler {
   private static final Map<String, byte[]> CACHE = Collections.synchronizedMap(new LinkedHashMap<>(5, 0.75f, true));

   private final Socket client;
   private final Logger logger = Logger.getLogger(ProxyHandler.class.getName());

   public ProxyHandler(Socket client) {
      this.client = client; //client who's doing the request
      handleProxyRequest();
   }

   private void handleProxyRequest() {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.client.getInputStream())); //read requests from the client
           OutputStream clientWriter = this.client.getOutputStream(); // write the response to the client
      ) {
         List<String> headers = new ArrayList<>();
         String line;
         while ((line = reader.readLine()) != null && !line.isEmpty()) {
            headers.add(line);
         }

         if (headers.isEmpty()) return;

         //first header is the request.
         String urlLine = headers.getFirst();
         String[] urlSplit = urlLine.split(" ");
         if (urlSplit.length < 3) {
            System.out.println("Url incomplete.");
            return;
         }
         String method = urlSplit[0];
         String target = urlSplit[1];

         if ("CONNECT".equals(method)) {
            var splitPor = target.split(":");
            String host = splitPor[0].trim();
            int port = Integer.parseInt(splitPor[1].trim());
            try (Socket remote = new Socket(host, port)) {
               OutputStream clientOut = client.getOutputStream();
               InputStream clientIn = client.getInputStream();
               OutputStream remoteOut = remote.getOutputStream();
               InputStream remoteIn = remote.getInputStream();

               clientOut.write("HTTP/1.1 200 Connection established\r\n\r\n".getBytes());
               clientOut.flush();

               Thread.ofVirtual().start(() -> forward(clientIn, remoteOut));
               forward(remoteIn, clientOut);
               return;
            }
         }


         URL url = URI.create(target).toURL();

         //check if the url is in CACHE
         System.out.println("Cache before.");
         CACHE.forEach((k, v) -> System.out.println(k + ": " + Arrays.toString(v)));
         if (CACHE.containsKey(url.toString())) {//if url is in CACHE then we return it.
            System.out.println("Response returned from the CACHE.");
            clientWriter.write(CACHE.get(url.toString()));
            clientWriter.flush();
            return;
         }

         String host = url.getHost();
         int port = url.getPort() == -1 ? 80 : url.getPort();
         forwardResponse(host, port, url, headers);

      } catch (IOException e) {
         logger.severe(e.getMessage());
      }
   }

   private void forwardResponse(String host, int port, URL url, List<String> headers) {
      try (Socket remoteServer = new Socket(host, port)) {
         System.out.println("remote Server");
         BufferedWriter remoteWriter =
                 new BufferedWriter(new OutputStreamWriter(remoteServer.getOutputStream())); //write the request on the server
         InputStream remoteInput = remoteServer.getInputStream(); //Get the response from the server if there's any.
         String[] urlSplit = headers.getFirst().split(" ");

         remoteWriter.write(urlSplit[0] + " " + url.getFile() + " " + urlSplit[2] + "\r\n");
         for (String header : headers) {
            remoteWriter.write(header + "\r\n");
         }
         remoteWriter.write("\r\n");
         remoteWriter.write("Connection: close\r\n");
         remoteWriter.flush();

         ByteArrayOutputStream buffer = new ByteArrayOutputStream();
         remoteInput.transferTo(buffer);
         byte[] response = buffer.toByteArray();

         client.getOutputStream().write(response);
         client.getOutputStream().flush();

         CACHE.put(url.toString(), response);
         System.out.println("response cached.");
         System.out.println("cache after:");
         CACHE.forEach((k, v) -> System.out.println(k + ": " + Arrays.toString(v)));
      } catch (IOException e) {
         logger.severe(e.getMessage());
      }
   }

   private void forward(InputStream in, OutputStream out) {
      try {
         byte[] buffer = new byte[8192];
         int bytesRead;
         int length = 0;

         while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            length += 1;
            out.flush();
         }
         byte[] response = Arrays.copyOfRange(buffer, 0, length);
         CACHE.put("https://dummyjson.com/products", response);
      } catch (IOException ignored) {
      }
   }
}
