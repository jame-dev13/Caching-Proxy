package cache;

public interface Cache {
   void putResource(String key, byte[] data);
   byte[] getResource(String key);
   boolean containsKey(String key);
   void clearCache();
   void printCache();
}
