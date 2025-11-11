package app;

import proxy.CachingProxy;

import java.util.Set;

public final class CLIApp {

   private static final Set<String> OPTIONS = Set.of("-p", "--PORT");

   /**
    * Perform the operations based on the options that going to be given by the console
    * and perform some verification to ensure the correct behavior of the program.
    *
    * The set {@link #OPTIONS} defines the arguments that must have to be present for that this
    * application works, there has to be the options or arguments '-p' or '--PORT' followed by
    * the number of port if any option is set, just accept one of them, the port range that accepts
    * is between 1024 and 49151, if the given port doesn't match with any number on that range the
    * program will shut down. Otherwise, the program will start on the given port, if there is no
    * option set the program will start with the default app port, see {@link proxy.CachingProxy#PORT}.
    * @param args the args option to be given.
    */
   public static void init(final String[] args){
      if(args.length > 2){
         System.err.println("Too much arguments. There just be 2: -p <number> or --port <number>.");
         System.exit(1);
         return;
      }
      if(args.length == 0){
         CachingProxy.startProxy();
         return;
      }
      try{
         for (int i = 0; i < args.length; i++) {
            if(!OPTIONS.contains(args[i])){
               System.err.println("Options unknown.");
               System.exit(1);
               return;
            }
            final int port = Integer.parseInt(args[1]);
            if(port < 1024 || port > 49151){
               System.err.println("Port value not accepted: " + port);
               System.out.println("Consider a value between (1024 - 49151).");
               System.exit(1);
               return;
            }
            CachingProxy.startProxy(port);
         }
      }catch (NumberFormatException e){
         System.err.println("Wrong type of params, expected number. " + e.getMessage());
         System.exit(1);
      }
   }
}
