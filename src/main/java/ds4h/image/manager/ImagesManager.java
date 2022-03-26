package ds4h.image.manager;

import ds4h.image.buffered.BufferedImage;
import ds4h.image.model.ImageFile;
import ds4h.observer.Observable;
import ds4h.services.FileService;
import ij.ImageListener;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import loci.formats.FormatException;

import java.awt.*;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * ListIterator of ImagePlus that has imagesFiles
 * If you find why does it make sense please explain it to me.
 * The previous developer has clearly not implemented a real iterator.
 * I want to finish my things, I don't want always to fix things, come on.
 */
public class ImagesManager implements ListIterator<ImagePlus>, Observable {
  // the YYYY-MM-DD format grants to the user the fact that the sorting can be done always by the name
  private final static String TODAY_FORMAT = "yyyy-MM-dd";
  private final PropertyChangeSupport support = new PropertyChangeSupport(this);
  private final List<ImageFile> imageFiles = new ArrayList<>();
  private int imageIndex;
  private int currentIndex;
  
  public ImagesManager(List<String> filesPath) throws ImageOversizeException, FormatException, IOException {
    this.imageIndex = -1;
    for (String filePath : filesPath) {
      this.addFile(filePath);
    }
  }
  
  public void addFile(String pathFile) throws IOException, FormatException, ImageOversizeException {
    ImageFile imageFile = new ImageFile(pathFile);
    this.getImageFiles().add(imageFile);
    this.addListener(imageFile);
  }
  
  private void addListener(ImageFile file) {
    ImagePlus.addImageListener(new ImageListener() {
      @Override
      public void imageOpened(ImagePlus imagePlus) {
        // Nothing for now
      }
      
      @Override
      public void imageClosed(ImagePlus imagePlus) {
        // Nothing for now
      }
      
      @Override
      public void imageUpdated(ImagePlus imagePlus) {
        if (imagePlus.changes) {
          handleUpdatedImageChanges(imagePlus, file);
          // then remove it
          ImagePlus.removeImageListener(this);
        }
      }
    });
  }
  
  private void handleUpdatedImageChanges(ImagePlus imagePlus, ImageFile file) {
    String path = this.saveUpdatedImage(imagePlus, file);
    // delete from stack the old one
    this.getImageFiles().remove(this.currentIndex + 1);
    // add new one
    try {
      this.addFile(path);
      firePropertyChange("updatedImage", file.getPathFile(), path);
    } catch (IOException | FormatException | ImageOversizeException e) {
      e.printStackTrace();
    }
  }
  
  private String saveUpdatedImage(ImagePlus imagePlus, ImageFile file) {
    final String baseDir = this.getDirFromPath(file.getPathFile());
    // Thanks to this you can have a more organized folder
    final String todayDir = this.getTodayDate() + "/";
    FileService.createDirectoryIfNotExist(baseDir + todayDir);
    final String dir = baseDir + todayDir;
    String path = String.format("%s%d.tiff", dir, imagePlus.getProcessor().hashCode());
    new FileSaver(imagePlus).saveAsTiff(path);
    return path;
  }
  
  private String getTodayDate() {
    LocalDate dateObj = LocalDate.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TODAY_FORMAT);
    return dateObj.format(formatter);
  }
  
  private String getDirFromPath(String path) {
    int lastIndexOfSeparator = path.lastIndexOf(File.separator);
    if (lastIndexOfSeparator != -1) {
      path = path.substring(0, lastIndexOfSeparator + 1);
    }
    return path;
  }
  
  private BufferedImage getImage(int index, boolean wholeSlide) {
    int progressive = 0;
    ImageFile imageFile = null;
    for (int i = 0; i < this.getImageFiles().size(); i++) {
      if (progressive + this.getImageFiles().get(i).getNImages() > index) {
        imageFile = this.getImageFiles().get(i);
        break;
      }
      progressive += this.getImageFiles().get(i).getNImages();
    }
    try {
      if (index == -1) {
        index = 0; // Just an ugly patch
      }
      if (imageFile != null) {
        this.currentIndex = index - progressive;
        final BufferedImage image = imageFile.getImage(this.currentIndex, wholeSlide);
        image.setFilePath(imageFile.getPathFile());
        image.setTitle(MessageFormat.format("Editor Image {0}/{1}", index + 1, this.getNImages()));
        return image;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  @Override
  public boolean hasNext() {
    return this.getImageIndex() < this.getNImages() - 1;
  }
  
  @Override
  public BufferedImage next() {
    if (!hasNext()) {
      return null;
    }
    this.imageIndex++;
    return getImage(this.getImageIndex(), false);
  }
  
  @Override
  public boolean hasPrevious() {
    return this.getImageIndex() > 0;
  }
  
  @Override
  public BufferedImage previous() {
    if (!hasPrevious()) {
      return null;
    }
    this.imageIndex--;
    return getImage(this.getImageIndex(), false);
  }
  
  @Override
  public int nextIndex() {
    return this.imageIndex + 1;
  }
  
  @Override
  public int previousIndex() {
    return this.imageIndex - 1;
  }
  
  @Override
  public void remove() {
  }
  
  @Override
  public void set(ImagePlus imagePlus) {
  
  }
  
  @Override
  public void add(ImagePlus imagePlus) {
  }
  
  public int getCurrentIndex() {
    return this.imageIndex;
  }
  
  public int getNImages() {
    return this.getImageFiles().stream().mapToInt(ImageFile::getNImages).sum();
  }
  
  /**
   * This flag indicates whenever the manger uses a reduced-size image for compatibility
   */
  public void dispose() {
    this.getImageFiles().forEach(imageFile -> {
      try {
        imageFile.dispose();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
  
  public BufferedImage get(int index) {
    return this.getImage(index, false);
  }
  
  public BufferedImage get(int index, boolean wholeSlide) {
    return this.getImage(index, wholeSlide);
  }
  
  public List<RoiManager> getRoiManagers() {
    List<RoiManager> result = new ArrayList<>();
    this.getImageFiles().forEach(imageFile -> result.addAll(imageFile.getRoiManagers()));
    return result;
  }
  
  // ?? Unused ??
  public Dimension getMaximumSize() {
    Dimension maximumSize = new Dimension();
    this.getImageFiles().forEach(imageFile -> {
      Dimension dimension = imageFile.getMaximumSize();
      maximumSize.width = (double) dimension.width > maximumSize.width ? dimension.width : maximumSize.width;
      maximumSize.height = (double) dimension.height > maximumSize.height ? dimension.height : maximumSize.height;
    });
    return maximumSize;
  }
  
  public List<Dimension> getImagesDimensions() {
    List<Dimension> dimensions;
    dimensions = this.getImageFiles().stream().reduce(new ArrayList<>(), (accDimensions, imageFile) -> {
      accDimensions.addAll(imageFile.getImagesDimensions());
      return accDimensions;
    }, (accumulated, value) -> accumulated);
    return dimensions;
  }
  
  public List<ImageFile> getImageFiles() {
    return this.imageFiles;
  }
  
  /**
   * Remove the imageFile from the manager and updates the image index
   *
   * @param index
   */
  public void removeImageFile(int index) {
    this.getImageFiles().remove(index);
    this.imageIndex = this.getImageIndex() >= this.getNImages() ? index - 1 : index;
  }
  
  private int getImageIndex() {
    return this.imageIndex;
  }
  
  @Override
  public PropertyChangeSupport getSupport() {
    return this.support;
  }
  
  public static class ImageOversizeException extends Exception {
  }
}
