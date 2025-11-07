package proxy;

import cache.Cache;
import cache.LRUCache;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is a kind of controller done for simulates a caching proxy
 * that catch the request for the client side y check into the cache, if the
 * url is in, just return the data associated, support only method GET, and http protocol,
 * for https requests it only forward client - server, server - client. due to Socket can't read
 * an SSL communication, and using an SSL socket implies to generate own CA certifies, witch clients like POSTMAN
 * or the BROWSERS have to validate and it maybe or not be valid depends on the case.
 */
public final class ProxyHandler {
   private static final Cache CACHE = new LRUCache(5);
   private final Socket client;
   private final Logger logger = Logger.getLogger(ProxyHandler.class.getName());

   public ProxyHandler(final Socket client) {
      this.client = client; //client who's doing the request
      handleProxyRequest();
   }

   /**
    * Manages the behavior of a caching proxy, reading client request and perform actions in base to the protocol http/https
    * of the request.
    * See {@link proxy.ProxyHandler#extractHeader(BufferedReader)}.
    * See {@link proxy.ProxyHandler#handleHttpResponse(String, int, URL, List, OutputStream)}.
    * See {@link proxy.ProxyHandler#handleHttpResponse(String, int, URL, List, OutputStream)}.
    * See {@link proxy.ProxyHandler#forwardHttp(List, URL, BufferedWriter, InputStream)}.
    * See {@link proxy.ProxyHandler#forwardHttps(InputStream, OutputStream)}.
    */
   private void handleProxyRequest() {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.client.getInputStream())); //read requests from the client
           OutputStream clientWriter = this.client.getOutputStream() // write the response to the client
      ) {
         List<String> headers = extractHeader(reader);
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

         //HTTPS
         if ("CONNECT".equals(method)) {
            handleHttpsResponse(client, target);
            return;
         }


         URL url = URI.create(target).toURL();

         //Checks if the resource is in the cache.
         if (CACHE.containsKey(url.toString())) {
            System.out.println("Response returned from the CACHE.");
            clientWriter.write(CACHE.getResource(url.toString()));
            clientWriter.flush();
            return;
         }

         String host = url.getHost();
         int port = url.getPort() == -1 ? 80 : url.getPort();
         //HTTP
         handleHttpResponse(host, port, url, headers, clientWriter);

      } catch (IOException e) {
         logger.severe(e.getMessage());
      }
   }

   /**
    * Extracts the headers in the incoming client request.
    *
    * @param reader the BufferedReader to read clients requests.
    * @return a {@code List<String>}
    * @throws IOException on I/O error.
    */
   private List<String> extractHeader(final BufferedReader reader) throws IOException {
      List<String> headers = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null && !line.isEmpty()) {
         headers.add(line);
      }
      return headers;
   }

   /**
    * Handle the response for http protocol, creates a new Socket witch represents the remote server (response).
    * basically, this method going to connect with the server in case that the response not found in cache, and going to request it.
    * See {@linkplain proxy.ProxyHandler#forwardHttp(List, URL, BufferedWriter, InputStream)}
    *
    * @param host           the server host.
    * @param port           the port host.
    * @param url            the request url.
    * @param headers        the request client headers.
    * @param clientResponse the writer witch going to write the response in the client side.
    */
   private void handleHttpResponse(final String host,
                                   final int port,
                                   final URL url,
                                   final List<String> headers,
                                   final OutputStream clientResponse) {
      try (Socket remoteServer = new Socket(host, port);
           BufferedWriter remoteWriter =
                   new BufferedWriter(new OutputStreamWriter(remoteServer.getOutputStream())); //write the request on the server
           InputStream remoteInput = remoteServer.getInputStream()) { //Get the response from the server if there's any.

         ByteArrayOutputStream buffer = forwardHttp(headers, url, remoteWriter, remoteInput);
         byte[] response = buffer.toByteArray();

         clientResponse.write(response);
         clientResponse.flush();

         CACHE.putResource(url.toString(), response);
         System.out.println("response cached.");
      } catch (IOException e) {
         logger.severe(e.getMessage());
      }
   }

   /**
    * Handles the communication when the method on the request is CONNECT due to a https protocol.
    * See {@link proxy.ProxyHandler#forwardHttps(InputStream, OutputStream)}
    *
    * @param client the Client socket.
    * @param target the target URL composed by the host and the port in this case.
    */
   private void handleHttpsResponse(final Socket client,
                                    final String target) {
      String[] splitTarget = target.split(":");
      String host = splitTarget[0].trim();
      int port = Integer.parseInt(splitTarget[1].trim());
      try (Socket remote = new Socket(host, port);
           InputStream clientInput = client.getInputStream();
           OutputStream clientOutput = client.getOutputStream();
           InputStream remoteInput = remote.getInputStream();
           OutputStream remoteOutput = remote.getOutputStream()) {
         clientOutput.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
         clientOutput.flush();

         Thread.ofVirtual().start(() -> forwardHttps(clientInput, remoteOutput));
         forwardHttps(remoteInput, clientOutput);
      } catch (IOException e) {
         logger.severe(e.getMessage());
      }
   }

   /**
    * Writes the request on the remote server, like: {@code GET /some/url/1 HTTP/1.X} then writes the headers
    * extract from the client request intercepted by this proxy and re-writes the Connection header
    * to <b>close</b> to avoid that the method {@code transferTo(OutputStream out)} locks and hangs out
    * the socket due to the header {@code Connection} is keep-alive, once this done, just return the buffer.
    * <pre>{@code
    *    ByteArrayOutPutStream buffer = new ByteArrayOutputStream();
    *    remoteInput.transferTo(buffer);
    *    return buffer;
    * }<pre/>
    * @param headers the {@code List<String> headers} witch contains the request headers.
    * @param url the {@code URL url} to do the request.
    * @param remoteWriter the {@code BufferedWriter remoteWriter} to write the request and request headers from the client.
    * @param remoteIn the response input from the remote.
    * @return buffer - the {@code ByteArrayOutputStream} witch the response was transmitted.
    * @throws IOException on I/O error.
    */
   private ByteArrayOutputStream forwardHttp(final List<String> headers,
                                             final URL url,
                                             final BufferedWriter remoteWriter,
                                             final InputStream remoteIn) throws IOException {
      String[] urlSplit = headers.getFirst().split(" ");
      String file = url.getFile().isEmpty() ? "/" : url.getFile();
      remoteWriter.write(urlSplit[0] + " " + file + " " + urlSplit[2] + "\r\n");
      for (String header : headers) {
         remoteWriter.write(header + "\r\n");
      }
      remoteWriter.write("\r\n");
      remoteWriter.write("Connection: close\r\n");
      remoteWriter.flush();

      try(ByteArrayOutputStream buffer = new ByteArrayOutputStream()){
         remoteIn.transferTo(buffer);
         return buffer;
      }
   }

   /**
    * Due to HTTPS doesn't allow reading responses through a Socket, it's just retransmitted from client to remote - remote to client,
    * because it can be cached.
    * <pre> {@code
    *    Thread.ofVirtual().start(() -> forwardHttps(clientInput, remoteOutput));
    *    forwardHttps(() -> remoteInput, clientOutput);
    * }
    * </pre>
    * A Thread sends the client requests and the remote outstream is going to write it and interpret it, so in the main Thread we're
    * going to have the response, and calling again this method we're going to get the result response for the client, reading from the
    * remote and writing from the client.
    *
    * @param in  the InputStream (remote or client)
    * @param out the OutputStream (remote or client)
    */
   private void forwardHttps(final InputStream in, final OutputStream out) {
      try {
         byte[] buffer = new byte[8192];
         int bytesRead;
         while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
         }
      } catch (IOException ignored) {
      }
   }
}
