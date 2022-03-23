package ds4h.services;

import java.io.File;

/**
 * https://stackoverflow.com/questions/482560/can-you-tell-on-runtime-if-youre-running-java-from-within-a-jar
 */
public class JarService {
  private JarService() {}
  
  public static String getJarName()
  {
    return new File(JarService.class
          .getProtectionDomain()
          .getCodeSource()
          .getLocation()
          .getPath())
          .getName();
  }
  
  public static boolean runningFromJar()
  {
    return getJarName().contains(".jar");
  }
}
