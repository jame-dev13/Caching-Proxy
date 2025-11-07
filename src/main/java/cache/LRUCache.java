package cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache extends LinkedHashMap<String, byte[]> {
   private final Map<String, byte[]> cache;
   private final int capacity;
   public LRUCache(int capacity){
      this.capacity = capacity;
      this.cache = new LinkedHashMap<>(this.capacity, 0.75f, true);
   }

   @Override
   protected boolean removeEldestEntry(final Map.Entry<String, byte[]> eldest) {
      return size() > this.capacity;
   }

   public byte[] getResource(String url){
      return this.cache.get(url);
   }

   public void putResource(String url, byte[] resource){
      this.cache.put(url, resource);
   }
}
