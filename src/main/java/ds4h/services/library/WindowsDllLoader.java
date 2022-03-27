/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.services.library;

import ds4h.image.registration.ImageAlignment;
import java.io.InputStream;
import java.util.List;

import static ds4h.services.library.OpenCVUtility.*;
import static java.util.stream.Collectors.toList;

public class WindowsDllLoader implements ResourceLoader, OpenCVUtility {
  private static final String OPENCV_PATH = "opencv_java";
  
  @Override
  public InputStream loadInputAsStream() {
    return ImageAlignment.class.getResourceAsStream(String.format("%s%s%s%s", getDir(), OPENCV_PATH, getVersion(), getExt()));
  }
  
  @Override
  public List<InputStream> loadExtra() {
    return OpenCVUtility
          .loadExtraLibraries()
          .stream()
          .map(ImageAlignment.class::getResourceAsStream)
          .collect(toList());
  }
}
