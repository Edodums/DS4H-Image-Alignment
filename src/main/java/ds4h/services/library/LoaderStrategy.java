/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.services.library;

import com.google.api.client.util.IOUtils;
import ij.IJ;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static ds4h.services.library.OpenCVUtility.getExt;

public class LoaderStrategy implements LibraryLoader {
  private ResourceLoader loader;
  
  @Override
  public void load() throws IOException {
    if (LibraryLoader.getOS().startsWith("Windows")) {
        this.loader = new WindowsDllLoader();
    }
    InputStream in = this.loader.loadInputAsStream();
    this.handleLoad(in);
  }
  
  private void handleLoad(InputStream in) throws IOException {
    File fileOut = File.createTempFile("lib", getExt());
    OutputStream out = FileUtils.openOutputStream(fileOut);
    if (in != null) {
      IOUtils.copy(in, out);
      in.close();
      out.close();
      System.load(fileOut.toString());
    } else {
      System.out.println("Couldn't find the right library\n the Developer didn't set the right name");
    }
  }
  
  public void loadExtra() throws IOException {
    List<InputStream> libraries = new ArrayList<>(this.loader.loadExtra());
    for (int index = 0, librariesSize = libraries.size(); index < librariesSize; index++) {
      InputStream library = libraries.get(index);
      this.handleLoad(library);
    }
  }
}
