/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.services.library;

import java.io.IOException;

public interface LibraryLoader {
  /**
   * Useless, but I prefer it for a better readability
   *
   * @return simply the OS name
   */
  static String getOS() {
    return System.getProperty("os.name");
  }
  
  /**
   * Loads the main library
   *
   * @throws IOException if it can't found the lib
   */
  void load() throws IOException;
  
  /**
   * Loads extra libraries, in the case of opencv it loads the extra features ( FREAK, and other non-free algorithms )
   *
   * @throws IOException if it can't found the lib
   */
  void loadExtra() throws IOException;
  
  /**
   * It's unused for the time being, but it could be helpful in the future
   *
   * @return bitness of the jvm
   */
  default int getOsBitness() {
    return Integer.parseInt(System.getProperty("sun.arch.data.model"));
  }
}
