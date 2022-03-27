/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h;

import ds4h.image.registration.ImageAlignment;
import ij.plugin.PlugIn;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class)
public class DS4H implements PlugIn, Command {
  @Override
  public void run(String s) {
    new ImageAlignment().run("");
  }
  
  @Override
  public void run() {
    this.run("");
  }
  
  public static void main(final String... args) {
    new ImageAlignment().run("");
  }
}
