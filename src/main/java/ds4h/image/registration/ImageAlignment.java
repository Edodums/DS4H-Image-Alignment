/*
 * COPYRIGHT:* Copyright (c) 2021, AUSL Romagna* Azienda USL della Romagna, Italy* All rights reserved.**
 * The informed consent published in integral part on the website of Azienda USL della Romagna
 * (Informed Consent AUSL, prot. N. 1683), must be citied.**
 * This material is free; you can redistribute it and/or modify it under the terms of the CC BY 4.0.*
 * This material is distributed WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package ds4h.image.registration;

import ds4h.builder.AbstractBuilder;
import ds4h.builder.FREAKBuilder;
import ds4h.builder.LeastSquareTransformationBuilder;
import ds4h.dialog.about.AboutDialog;
import ds4h.dialog.align.AlignDialog;
import ds4h.dialog.align.OnAlignDialogEventListener;
import ds4h.dialog.align.event.IAlignDialogEvent;
import ds4h.dialog.align.event.ReuseImageEvent;
import ds4h.dialog.align.event.SaveEvent;
import ds4h.dialog.loading.LoadingDialog;
import ds4h.dialog.main.MainDialog;
import ds4h.dialog.main.OnMainDialogEventListener;
import ds4h.dialog.main.event.*;
import ds4h.dialog.preview.OnPreviewDialogEventListener;
import ds4h.dialog.preview.PreviewDialog;
import ds4h.dialog.preview.event.CloseDialogEvent;
import ds4h.dialog.preview.event.IPreviewDialogEvent;
import ds4h.dialog.remove.OnRemoveDialogEventListener;
import ds4h.dialog.remove.RemoveImageDialog;
import ds4h.dialog.remove.event.IRemoveDialogEvent;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.manager.ImagesManager;
import ds4h.image.model.ImageFile;
import ds4h.utils.Utilities;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import loci.common.enumeration.EnumException;
import loci.formats.FormatException;
import loci.formats.UnknownFormatException;
import net.imagej.ImageJ;
import org.opencv.core.Core;
import org.scijava.AbstractContextual;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ImageAlignment extends AbstractContextual implements PlugIn, OnMainDialogEventListener, OnPreviewDialogEventListener, OnAlignDialogEventListener, OnRemoveDialogEventListener {
  private static final String IMAGES_SCALED_MESSAGE = "Image size too large: image has been scaled for compatibility.";
  private static final String SINGLE_IMAGE_MESSAGE = "Only one image detected in the stack: align operation will be unavailable.";
  private static final String IMAGES_OVERSIZE_MESSAGE = "Cannot open the selected image: image exceed supported dimensions.";
  private static final String ALIGNED_IMAGE_NOT_SAVED_MESSAGE = "Aligned images not saved: are you sure you want to exit without saving?";
  private static final String DELETE_ALL_IMAGES = "Do you confirm to delete all the images of the stack?";
  private static final String IMAGE_SAVED_MESSAGE = "Image successfully saved";
  private static final String ROI_NOT_ADDED_MESSAGE = "One or more corner points not added: they exceed the image bounds";
  private static final String INSUFFICIENT_MEMORY_MESSAGE = "Insufficient computer memory (RAM) available. \n\n\t Try to increase the allocated memory by going to \n\n\t                Edit  ▶ Options  ▶ Memory & Threads \n\n\t Change \"Maximum Memory\" to, at most, 1000 MB less than your computer's total RAM.";
  private static final String INSUFFICIENT_MEMORY_TITLE = "Error: insufficient memory";
  private static final String UNKNOWN_FORMAT_MESSAGE = "Error: trying to open a file with a unsupported format.";
  private static final String UNKNOWN_FORMAT_TITLE = "Error: unknown format";
  private static final String MAIN_DIALOG_TITLE_PATTERN = "Editor Image {0}/{1}";
  private static final String CAREFUL_NOW_TITLE = "Careful now";
  private static final String TIFF_EXT = ".tiff";
  private static final String NULL_PATH = "nullnull";
  private static final String TEMP_PATH = "temp";
  
  static {
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
  }
  
  private List<String> tempImages = new ArrayList<>();
  private ImagesManager manager;
  private BufferedImage image = null;
  private MainDialog mainDialog;
  private PreviewDialog previewDialog;
  private AlignDialog alignDialog;
  private LoadingDialog loadingDialog;
  private AboutDialog aboutDialog;
  private RemoveImageDialog removeImageDialog;
  private boolean alignedImageSaved = false;
  private long totalMemory = 0;
  @Parameter
  private LogService logService;
  
  public static void main(final String... args) {
    ImageJ ij = new ImageJ();
    ij.launch(args);
    ImageAlignment plugin = new ImageAlignment();
    plugin.setContext(ij.getContext());
    plugin.run("");
  }
  
  @Override
  public void onMainDialogEvent(IMainDialogEvent dialogEvent) {
    if (image != null) {
      WindowManager.setCurrentWindow(image.getWindow());
    }
    switch (dialogEvent.getClass().getSimpleName()) {
      case "PreviewImageEvent":
        previewImage((PreviewImageEvent) dialogEvent);
        break;
      case "ChangeImageEvent":
        getChangeImageThread((ChangeImageEvent) dialogEvent);
        break;
      case "DeleteRoiEvent":
        deleteRoi((DeleteRoiEvent) dialogEvent);
        break;
      case "AddRoiEvent":
        addRoi((AddRoiEvent) dialogEvent);
        break;
      case "SelectedRoiEvent":
        handleSelectedRoi((SelectedRoiEvent) dialogEvent);
        break;
      case "SelectedRoiFromOvalEvent":
        handleSelectedRoiFromOval((SelectedRoiFromOvalEvent) dialogEvent);
        break;
      case "DeselectedRoiEvent":
        handleDeselectedRoi((DeselectedRoiEvent) dialogEvent);
        break;
      case "AlignEvent":
        align((AlignEvent) dialogEvent);
        break;
      case "AutoAlignEvent":
        autoAlign((AutoAlignEvent) dialogEvent);
        break;
      case "OpenFileEvent":
      case "ExitEvent":
        openOrExitEventHandler(dialogEvent);
        break;
      case "OpenAboutEvent":
        this.aboutDialog.setVisible(true);
        break;
      case "MovedRoiEvent":
        handleMovedRoi();
        break;
      case "AddFileEvent":
        addFile((AddFileEvent) dialogEvent);
        break;
      case "CopyCornersEvent":
        copyCorners();
        break;
      case "RemoveImageEvent":
        if (this.removeImageDialog == null || !this.removeImageDialog.isVisible()) {
          removeImage();
        }
        break;
      default:
        logService.log(LogLevel.ERROR, "No event known was called");
        break;
    }
  }
  
  private void handleMovedRoi() {
    this.mainDialog.refreshROIList(image.getManager());
    if (previewDialog != null) this.previewDialog.drawRois();
  }
  
  private void removeImage() {
    this.loadingDialog.showDialog();
    Utilities.setTimeout(() -> {
      this.removeImageDialog = new RemoveImageDialog(this.manager.getImageFiles(), this);
      this.removeImageDialog.setVisible(true);
      this.loadingDialog.hideDialog();
      this.loadingDialog.requestFocus();
    }, 20);
  }
  
  private void openOrExitEventHandler(IMainDialogEvent dialogEvent) {
    boolean roisPresent = manager.getRoiManagers().stream().anyMatch(it -> it.getRoisAsArray().length != 0);
    if (roisPresent && handleRoisPresence(dialogEvent)) {
      if (dialogEvent instanceof OpenFileEvent) {
        String pathFile = promptForFile();
        if (!pathFile.equals(NULL_PATH)) {
          this.disposeAll();
          this.initialize(Collections.singletonList(pathFile));
        }
      }
      
      if (dialogEvent instanceof ExitEvent) {
        disposeAll();
        System.exit(0);
      }
    }
  }
  
  private boolean handleRoisPresence(IMainDialogEvent dialogEvent) {
    String[] buttons = {"Yes", "No"};
    String message = dialogEvent instanceof OpenFileEvent ? "This will replace the existing image. Proceed anyway?" : "You will lose the existing added landmarks. Proceed anyway?";
    int answer = JOptionPane.showOptionDialog(null, message, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
    return answer != 1;
  }
  
  private void copyCorners() {
    // get the indexes of all roi managers with at least a roi added
    List<Integer> imageIndexes;
    // remove the index of the current image, if present.
    List<Integer> list = new ArrayList<>();
    for (RoiManager roiManager : manager.getRoiManagers()) {
      if (roiManager.getRoisAsArray().length != 0) {
        int index = manager.getRoiManagers().indexOf(roiManager);
        if (index != manager.getCurrentIndex()) {
          list.add(index);
        }
      }
    }
    imageIndexes = list;
    Object[] options = imageIndexes.stream().map(imageIndex -> "Image " + (imageIndex + 1)).toArray();
    JComboBox<Object> optionList = new JComboBox<>(options);
    optionList.setSelectedIndex(0);
    // n means most certainly "answer"
    int n = JOptionPane.showOptionDialog(new JFrame(), optionList, "Copy from", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Copy", "Cancel"}, JOptionPane.YES_OPTION);
    if (n == JOptionPane.YES_OPTION) {
      RoiManager selectedManager = manager.getRoiManagers().get(imageIndexes.get(optionList.getSelectedIndex()));
      List<Point> roiPoints = Arrays.stream(selectedManager.getRoisAsArray()).map(roi -> new Point((int) roi.getRotationCenter().xpoints[0], (int) roi.getRotationCenter().ypoints[0])).collect(Collectors.toList());
      roiPoints.stream().filter(roiCoords -> roiCoords.x < image.getWidth() && roiCoords.y < image.getHeight()).forEach(roiCoords -> this.onMainDialogEvent(new AddRoiEvent(roiCoords)));
      
      if (roiPoints.stream().anyMatch(roiCoords -> roiCoords.x > image.getWidth() || roiCoords.y > image.getHeight()))
        JOptionPane.showMessageDialog(null, ROI_NOT_ADDED_MESSAGE, "Warning", JOptionPane.WARNING_MESSAGE);
      
      this.image.setCopyCornersMode();
    }
  }
  
  private void addFile(AddFileEvent dialogEvent) {
    String pathFile = dialogEvent.getFilePath();
    try {
      long memory = ImageFile.estimateMemoryUsage(pathFile);
      totalMemory += memory;
      if (totalMemory >= Runtime.getRuntime().maxMemory()) {
        JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
      }
      manager.addFile(pathFile);
    } catch (UnknownFormatException e) {
      loadingDialog.hideDialog();
      JOptionPane.showMessageDialog(null, UNKNOWN_FORMAT_MESSAGE, "Error: unknow format", JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      e.printStackTrace();
    }
    mainDialog.setPrevImageButtonEnabled(manager.hasPrevious());
    mainDialog.setNextImageButtonEnabled(manager.hasNext());
    mainDialog.setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, manager.getCurrentIndex() + 1, manager.getNImages()));
    refreshRoiGUI();
  }
  
  /*
   * New Feature
   * TODO: refactor codebase when you have time
   **/
  private void autoAlign(AutoAlignEvent event) {
    AbstractBuilder builder = new FREAKBuilder(this.loadingDialog, this, this.manager, event);
    builder.getLoadingDialog().showDialog();
    Utilities.setTimeout(() -> {
      try {
        this.alignHandler(builder);
      } catch (Exception e) {
        e.printStackTrace();
      }
      builder.getLoadingDialog().hideDialog();
    }, 10);
  }
  
  
  private void align(AlignEvent event) {
    AbstractBuilder builder = new LeastSquareTransformationBuilder(this.loadingDialog, this.manager, event, this);
    builder.getLoadingDialog().showDialog();
    Utilities.setTimeout(() -> {
      try {
        if (event.isKeepOriginal()) {
          this.alignHandler(builder);
        } else {
          builder.build();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      builder.getLoadingDialog().hideDialog();
    }, 10);
  }
  
  private void alignHandler(AbstractBuilder builder) {
    builder.init();
    builder.setTempImages(this.tempImages);
    builder.setVirtualStack();
    builder.align();
    builder.build();
    this.alignDialog = builder.getAlignDialog();
    this.tempImages = builder.getTempImages();
  }
  
  private void handleDeselectedRoi(DeselectedRoiEvent dialogEvent) {
    Arrays.stream(image.getManager().getSelectedRoisAsArray()).forEach(roi -> roi.setStrokeColor(Color.BLUE));
    image.getManager().select(dialogEvent.getRoiIndex());
    previewDialog.drawRois();
  }
  
  private void handleSelectedRoiFromOval(SelectedRoiFromOvalEvent dialogEvent) {
    mainDialog.jListRois.setSelectedIndex(dialogEvent.getRoiIndex());
  }
  
  private void handleSelectedRoi(SelectedRoiEvent dialogEvent) {
    Arrays.stream(image.getManager().getSelectedRoisAsArray()).forEach(roi -> roi.setStrokeColor(Color.BLUE));
    image.getManager().select(dialogEvent.getRoiIndex());
    image.getRoi().setStrokeColor(Color.yellow);
    image.updateAndDraw();
    if (previewDialog != null && previewDialog.isVisible()) {
      previewDialog.drawRois();
    }
  }
  
  private void addRoi(AddRoiEvent dialogEvent) {
    Prefs.useNamesAsLabels = true;
    Prefs.noPointLabels = false;
    int roiWidth = Math.max(Toolkit.getDefaultToolkit().getScreenSize().width, image.getWidth());
    roiWidth = (int) (roiWidth * 0.03);
    OvalRoi outer = new OvalRoi(dialogEvent.getClickCoords().x - (roiWidth / 2), dialogEvent.getClickCoords().y - (roiWidth / 2), roiWidth, roiWidth);
    // get roughly the 0,25% of the width of the image as stroke width of th rois added.
    // If the resultant value is too small, set it as the minimum value
    int strokeWidth = Math.max((int) (image.getWidth() * 0.0025), 3);
    outer.setStrokeWidth(strokeWidth);
    outer.setImage(image);
    outer.setStrokeColor(Color.BLUE);
    Overlay over = new Overlay();
    over.drawBackgrounds(false);
    over.drawLabels(false);
    over.drawNames(true);
    over.setLabelFontSize(Math.round(strokeWidth * 1f), "scale");
    over.setLabelColor(Color.BLUE);
    over.setStrokeWidth((double) strokeWidth);
    over.setStrokeColor(Color.BLUE);
    outer.setName("•");
    Arrays.stream(image.getManager().getRoisAsArray()).forEach(over::add);
    over.add(outer);
    image.getManager().setOverlay(over);
    refreshRoiGUI();
    refreshRoiGUI();
  }
  
  private void deleteRoi(DeleteRoiEvent dialogEvent) {
    image.getManager().select(dialogEvent.getRoiIndex());
    image.getManager().runCommand("Delete");
    refreshRoiGUI();
  }
  
  
  private void getChangeImageThread(ChangeImageEvent dialogEvent) {
    if (image != null) {
      image.removeMouseListeners();
    }
    boolean isNext = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.NEXT && !manager.hasNext();
    boolean isPrevious = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.PREV && !manager.hasPrevious();
    if (isNext || isPrevious) {
      this.loadingDialog.hideDialog();
    }
    image = dialogEvent.getChangeDirection() == ChangeImageEvent.ChangeDirection.NEXT ? this.manager.next() : this.manager.previous();
    if (image != null) {
      mainDialog.changeImage(image);
      mainDialog.setPrevImageButtonEnabled(manager.hasPrevious());
      mainDialog.setNextImageButtonEnabled(manager.hasNext());
      mainDialog.setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, manager.getCurrentIndex() + 1, manager.getNImages()));
      image.buildMouseListener();
      this.loadingDialog.hideDialog();
      refreshRoiGUI();
      this.loadingDialog.showDialog();
      this.loadingDialog.hideDialog();
    }
  }
  
  private void previewImage(PreviewImageEvent dialogEvent) {
    new Thread(() -> {
      if (!dialogEvent.getValue()) {
        previewDialog.close();
        return;
      }
      
      try {
        this.loadingDialog.showDialog();
        previewDialog = new PreviewDialog(manager.get(manager.getCurrentIndex()), this, manager.getCurrentIndex(), manager.getNImages(), "Preview Image " + (manager.getCurrentIndex() + 1) + "/" + manager.getNImages());
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.loadingDialog.hideDialog();
      previewDialog.pack();
      previewDialog.setVisible(true);
      previewDialog.drawRois();
    }).start();
  }
  
  @Override
  public void onPreviewDialogEvent(IPreviewDialogEvent dialogEvent) {
    if (dialogEvent instanceof ds4h.dialog.preview.event.ChangeImageEvent) {
      ds4h.dialog.preview.event.ChangeImageEvent event = (ds4h.dialog.preview.event.ChangeImageEvent) dialogEvent;
      new Thread(() -> {
        WindowManager.setCurrentWindow(image.getWindow());
        BufferedImage previewImage = manager.get(event.getIndex());
        previewDialog.changeImage(previewImage, "Preview Image " + (event.getIndex() + 1) + "/" + manager.getNImages());
        this.loadingDialog.hideDialog();
      }).start();
      this.loadingDialog.showDialog();
    }
    
    if (dialogEvent instanceof CloseDialogEvent) {
      mainDialog.setPreviewWindowCheckBox(false);
    }
  }
  
  @Override
  public void onAlignDialogEventListener(IAlignDialogEvent dialogEvent) {
    if (dialogEvent instanceof SaveEvent) {
      SaveDialog saveDialog = new SaveDialog("Save as", "aligned", TIFF_EXT);
      if (saveDialog.getFileName() == null) {
        loadingDialog.hideDialog();
        return;
      }
      String path = saveDialog.getDirectory() + saveDialog.getFileName();
      loadingDialog.showDialog();
      new FileSaver(this.alignDialog.getImagePlus()).saveAsTiff(path);
      loadingDialog.hideDialog();
      JOptionPane.showMessageDialog(null, IMAGE_SAVED_MESSAGE, "Save complete", JOptionPane.INFORMATION_MESSAGE);
      this.alignedImageSaved = true;
    }
    
    if (dialogEvent instanceof ReuseImageEvent) {
      this.disposeAll();
      this.initialize(Collections.singletonList(tempImages.get(tempImages.size() - 1)));
    }
    
    if (dialogEvent instanceof ds4h.dialog.align.event.ExitEvent) {
      if (!alignedImageSaved) {
        String[] buttons = {"Yes", "No"};
        int answer = JOptionPane.showOptionDialog(null, ALIGNED_IMAGE_NOT_SAVED_MESSAGE, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
        if (answer == 1) return;
      }
      alignDialog.setVisible(false);
      alignDialog.dispose();
    }
  }
  
  @Override
  public void onRemoveDialogEvent(IRemoveDialogEvent removeEvent) {
    if (removeEvent instanceof ds4h.dialog.remove.event.ExitEvent) {
      removeImageDialog.setVisible(false);
      removeImageDialog.dispose();
    }
    if (removeEvent instanceof ds4h.dialog.remove.event.RemoveImageEvent) {
      int imageFileIndex = ((ds4h.dialog.remove.event.RemoveImageEvent) removeEvent).getImageFileIndex();
      // only a image is available: if user remove this image we need to ask him to choose another one!
      if (this.manager.getImageFiles().size() == 1) {
        String[] buttons = {"Yes", "No"};
        int answer = JOptionPane.showOptionDialog(null, DELETE_ALL_IMAGES, CAREFUL_NOW_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, buttons, buttons[1]);
        
        if (answer == 0) {
          String pathFile = promptForFile();
          if (pathFile.equals(NULL_PATH)) {
            disposeAll();
            System.exit(0);
            return;
          }
          this.disposeAll();
          this.initialize(Collections.singletonList(pathFile));
        }
      } else {
        // remove the image selected
        this.removeImageDialog.removeImageFile(imageFileIndex);
        this.manager.removeImageFile(imageFileIndex);
        image = manager.get(manager.getCurrentIndex());
        mainDialog.changeImage(image);
        mainDialog.setPrevImageButtonEnabled(manager.hasPrevious());
        mainDialog.setNextImageButtonEnabled(manager.hasNext());
        mainDialog.setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, manager.getCurrentIndex() + 1, manager.getNImages()));
        this.refreshRoiGUI();
      }
    }
  }
  
  /**
   * Refresh all the Roi-based guis in the MainDialog
   */
  private void refreshRoiGUI() {
    mainDialog.drawRois(image.getManager());
    if (previewDialog != null && previewDialog.isVisible()) {
      previewDialog.drawRois();
    }
    // Get the number of rois added in each image. If they are all the same (and at least one is added), we can enable the "align" functionality
    List<Integer> roisNumber = manager.getRoiManagers().stream().map(roiManager -> roiManager.getRoisAsArray().length).collect(Collectors.toList());
    boolean alignButtonEnabled = roisNumber.get(0) >= LeastSquareImageTransformation.MINIMUM_ROI_NUMBER && manager.getNImages() > 1 && roisNumber.stream().distinct().count() == 1;
    // check if: the number of images is more than 1, ALL the images has the same number of rois added and the ROI numbers are more than 3
    mainDialog.setAlignButtonEnabled(alignButtonEnabled);
    
    boolean copyCornersEnabled = manager.getRoiManagers().stream().filter(roiManager -> roiManager.getRoisAsArray().length != 0).map(roiManager -> manager.getRoiManagers().indexOf(roiManager)).filter(index -> index != manager.getCurrentIndex()).count() != 0;
    mainDialog.setCopyCornersEnabled(copyCornersEnabled);
  }
  
  /**
   * Initialize the plugin opening the file specified in the mandatory param
   */
  public void initialize(List<String> filePaths) {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
      this.loadingDialog.hideDialog();
      if (e instanceof OutOfMemoryError) {
        this.loadingDialog.hideDialog();
        JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
        System.exit(0);
      }
    });
    this.aboutDialog = new AboutDialog();
    this.loadingDialog = new LoadingDialog();
    this.loadingDialog.showDialog();
    alignedImageSaved = false;
    boolean complete = false;
    try {
      for (String filePath : filePaths) {
        this.showIfMemoryIsInsufficient(filePath);
      }
      manager = new ImagesManager(filePaths);
      image = manager.next();
      mainDialog = new MainDialog(image, this);
      mainDialog.setPrevImageButtonEnabled(manager.hasPrevious());
      mainDialog.setNextImageButtonEnabled(manager.hasNext());
      mainDialog.setTitle(MessageFormat.format(MAIN_DIALOG_TITLE_PATTERN, manager.getCurrentIndex() + 1, manager.getNImages()));
      mainDialog.pack();
      mainDialog.setVisible(true);
      this.loadingDialog.hideDialog();
      if (image.isReduced())
        JOptionPane.showMessageDialog(null, IMAGES_SCALED_MESSAGE, "Info", JOptionPane.INFORMATION_MESSAGE);
      if (manager.getNImages() == 1)
        JOptionPane.showMessageDialog(null, SINGLE_IMAGE_MESSAGE, "Warning", JOptionPane.WARNING_MESSAGE);
      complete = true;
    } catch (ImagesManager.ImageOversizeException e) {
      JOptionPane.showMessageDialog(null, IMAGES_OVERSIZE_MESSAGE);
    } catch (UnknownFormatException | EnumException e) {
      JOptionPane.showMessageDialog(null, UNKNOWN_FORMAT_MESSAGE, UNKNOWN_FORMAT_TITLE, JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      this.loadingDialog.hideDialog();
      if (!complete) {
        this.run("");
      }
    }
  }
  
  private void showIfMemoryIsInsufficient(String pathFile) throws IOException, FormatException {
    try {
      long memory = ImageFile.estimateMemoryUsage(pathFile);
      totalMemory += memory;
      if (totalMemory >= Runtime.getRuntime().maxMemory()) {
        JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
        this.run("");
      }
    } catch (UnknownFormatException e) {
      loadingDialog.hideDialog();
      JOptionPane.showMessageDialog(null, UNKNOWN_FORMAT_MESSAGE, UNKNOWN_FORMAT_TITLE, JOptionPane.ERROR_MESSAGE);
    }
  }
  
  private String promptForFile() {
    OpenDialog od = new OpenDialog("Select an image");
    String dir = od.getDirectory();
    String name = od.getFileName();
    return (dir + name);
  }
  
  private List<String> promptForFiles() {
    FileDialog fileDialog = new FileDialog((Frame) null);
    fileDialog.setMultipleMode(true);
    fileDialog.setMode(FileDialog.LOAD);
    fileDialog.setResizable(true);
    // Note: Other options must be put before this line, otherwise they won't be applied
    fileDialog.setVisible(true);
    return Arrays.stream(fileDialog.getFiles()).map(File::getPath).collect(Collectors.toList());
  }
  
  /**
   * Dispose all the opened workload objects.
   */
  private void disposeAll() {
    this.mainDialog.dispose();
    this.loadingDialog.hideDialog();
    this.loadingDialog.dispose();
    if (this.previewDialog != null) this.previewDialog.dispose();
    if (this.alignDialog != null) this.alignDialog.dispose();
    if (this.removeImageDialog != null) this.removeImageDialog.dispose();
    this.manager.dispose();
    totalMemory = 0;
  }
  
  public List<String> getTempImages() {
    return this.tempImages;
  }
  
  @Override
  public void run(String s) {
    try {
      List<String> filePaths = this.promptForFiles();
      filePaths.removeIf(filePath -> filePath.equals(NULL_PATH));
      this.initialize(filePaths);
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        this.getTempImages().forEach(tempImage -> {
          try {
            Files.deleteIfExists(Paths.get(tempImage));
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      }));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

