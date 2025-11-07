package utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class that uses an ExecutorService instance to submit various tasks and performs it.
 */
public final class ExecutorServiceUtil {
   private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

   /**
    * Just like {@code executors.submit(Runnable r);}
    * @param r the Runnable function.
    */
   public static void start(Runnable r){
      executor.submit(r);
   }
}
