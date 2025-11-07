package cache;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * This class implements {@link Cache} witch has method to put, get, clear and check if contains
 * any key that are given to it. Uses a synchronizedMap of {@link @LinkedHashMap} to perform the
 * behavior of the LRU (Last Recently Use) algorithm to manage the data store in cache.
 */
public final class LRUCache implements Cache {

   private final Map<String, byte[]> cache;

   public LRUCache(final int capacity) {
      this.cache = Collections.synchronizedMap(new LinkedHashMap<>(){
         @Override
         protected boolean removeEldestEntry(final Map.Entry<String, byte[]> eldest) {
            return size() > capacity;
         }
      });
   }

   /**
    * Adds a new resource o replace if the key is in it.
    * @param url the key.
    * @param resource the resource associated with the url key.
    */
   public void putResource(final String url, final byte[] resource) {
      this.cache.put(url, resource);
   }

   /**
    * Gets the value associated with the given key.
    * @param url the key.
    * @return the value of the key.
    */
   public byte[] getResource(final String url) {
      return this.cache.get(url);
   }

   /**
    * Checks if the Map contains the given key.
    * @param key the key.
    * @return true if the key is in the Map, false otherwise.
    */
   @Override
   public boolean containsKey(final String key) {
      return this.cache.containsKey(key);
   }

   /**
    * Clear all the data store in the Map (key, values).
    */
   @Override
   public void clearCache() {
      this.cache.clear();
   }

   /**
    * Prints a representation of the Map data stored.
    */
   @Override
   public void printCache() {
      this.cache.forEach((k, v) -> System.out.println(k + ": " + Arrays.toString(v)));
   }
}
