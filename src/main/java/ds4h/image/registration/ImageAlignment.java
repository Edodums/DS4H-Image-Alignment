package ds4h.image.registration;

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
import ij.*;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import loci.common.enumeration.EnumException;
import loci.formats.FormatException;
import loci.formats.UnknownFormatException;
import net.imagej.ImageJ;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.xfeatures2d.FREAK;
import org.scijava.AbstractContextual;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Plugin(type = Command.class, headless = true, menuPath = "Plugins>Registration>DSH4 Image Alignment")
public class ImageAlignment extends AbstractContextual implements Command, OnMainDialogEventListener, OnPreviewDialogEventListener, OnAlignDialogEventListener, OnRemoveDialogEventListener {
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
  private static final String IMAGE_SIZE_TOO_BIG = "During computation the expected file size overcame imagej file limit. To continue, deselect \"keep all pixel data\" option.";
  private static final String MAIN_DIALOG_TITLE_PATTERN = "Editor Image {0}/{1}";
  private static final String CAREFUL_NOW_TITLE = "Careful now";
  private static final String TIFF_EXT = ".tiff";
  private static final String NULL_PATH = "nullnull";
  private final List<String> tempImages = new ArrayList<>();
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
  
  public static void main(final String... args) {
    ImageJ ij = new ImageJ();
    ij.launch(args);
    ImageAlignment plugin = new ImageAlignment();
    plugin.setContext(ij.getContext());
    plugin.run();
  }
  
  @Override
  public void run() {
    // Chiediamo come prima cosa il file all'utente
    // String pathFile = promptForFile();
    List<String> filePaths = promptForFiles();
    for (String filePath : filePaths) {
      if (filePath.equals(NULL_PATH)) return;
    }
    this.initialize(filePaths);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      for (String tempImage : this.tempImages) {
        try {
          Files.deleteIfExists(Paths.get(tempImage));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }));
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
        autoAlign();
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
        System.err.println("No event known was called");
        break;
    }
  }
  
  /*
   * New Feature
   * TODO: refactor codebase when you have time
   **/
  private void autoAlign() {
    try {
      List<Mat> images = new ArrayList<>();
      for (ImageFile imageFile : manager.getImageFiles()) {
        System.out.println(imageFile);
        Mat image = Imgcodecs.imread(imageFile.getPathFile());
        
        images.add(image);
      }
  
      images.forEach(System.out::println);
      
      int maxNumFeatures = 1000;
      FREAK freak = FREAK.create(true, true, 1.0f);
      
      List<Mat> descriptors = new ArrayList<>();
      List<MatOfKeyPoint> keyPoints = new ArrayList<>();
      
      images.forEach(image -> {
        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        Mat descriptor = new Mat();
        freak.detectAndCompute(image, new Mat(), keypoint, descriptor);
        descriptors.add(descriptor);
        keyPoints.add(keypoint);
      });
      
      DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
      MatOfDMatch matches = new MatOfDMatch();
      descriptors.forEach(descriptor -> matcher.match(descriptor, matches));
      keyPoints.forEach(System.out::println);
      descriptors.forEach(System.out::println);
      Mat imgMatches = new Mat();
      IntStream.range(1, images.size())
            .forEachOrdered(i -> Features2d.drawMatches(
                  images.get(i - 1),
                  keyPoints.get(i - 1),
                  images.get(i),
                  keyPoints.get(i),
                  matches,
                  imgMatches
            ));
      
      HighGui.imshow("Matches", imgMatches);
      System.out.println(imgMatches);
      HighGui.waitKey(0);
      
    } catch (Exception e) {
      e.printStackTrace();
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
    List<Integer> imageIndexes = manager.getRoiManagers().stream().filter(roiManager -> roiManager.getRoisAsArray().length != 0).map(roiManager -> manager.getRoiManagers().indexOf(roiManager)).filter(index -> index != manager.getCurrentIndex())// remove the index of the current image, if present.
          .collect(Collectors.toList());
    
    Object[] options = imageIndexes.stream().map(imageIndex -> "Image " + (imageIndex + 1)).toArray();
    JComboBox<Object> optionList = new JComboBox<>(options);
    optionList.setSelectedIndex(0);
    
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
  
  private void align(AlignEvent dialogEvent) {
    this.loadingDialog.showDialog();
    // Timeout is necessary to ensure that the loadingDialog is shown
    Utilities.setTimeout(() -> {
      try {
        VirtualStack virtualStack;
        ImagePlus transformedImagesStack;
        if (dialogEvent.isKeepOriginal()) {
          // MAX IMAGE SIZE SEARCH AND SOURCE IMG SELECTION
          // search for the maximum size of the images and the index of the image with the maximum width
          List<Dimension> dimensions = manager.getImagesDimensions();
          Dimension maximumSize = getMaxSizeImage(dimensions);
          int sourceImgIndex = getSourceIndex(maximumSize);
          BufferedImage sourceImg = manager.get(sourceImgIndex, true);
          
          // FINAL STACK SIZE CALCULATION AND OFFSETS
          Dimension finalStackDimension = new Dimension(maximumSize.width, maximumSize.height);
          List<Integer> offsetsX = new ArrayList<>();
          List<Integer> offsetsY = new ArrayList<>();
          List<RoiManager> managers = manager.getRoiManagers();
          offsetHandler(sourceImgIndex, sourceImg, offsetsX, offsetsY, managers);
          Optional<Integer> offsetXMax = offsetsX.stream().max(Comparator.naturalOrder());
          if (!offsetXMax.isPresent()) {return;}
          int maxOffsetX = offsetXMax.get();
          int maxOffsetXIndex = offsetsX.indexOf(maxOffsetX);
          if (maxOffsetX <= 0) {
            maxOffsetX = 0;
            maxOffsetXIndex = -1;
          }
  
          Optional<Integer> offsetYMax = offsetsX.stream().max(Comparator.naturalOrder());
          if (!offsetYMax.isPresent()) {return;}
          int maxOffsetY = offsetYMax.get();
          int maxOffsetYIndex = offsetsY.indexOf(maxOffsetY);
          if (maxOffsetY <= 0) {
            maxOffsetY = 0;
          }
          // Calculate the final stack size. It is calculated as maximumImageSize + maximum offset in respect of the source image
          finalStackDimension.width = finalStackDimension.width + maxOffsetX;
          finalStackDimension.height += sourceImg.getHeight() == maximumSize.height ? maxOffsetY : 0;
          
          // The final stack of the image is exceeding the maximum size of the images for imagej (see http://imagej.1557.x6.nabble.com/Large-image-td5015380.html)
          if (((double) finalStackDimension.width * finalStackDimension.height) > Integer.MAX_VALUE) {
            JOptionPane.showMessageDialog(null, IMAGE_SIZE_TOO_BIG, "Error: image size too big", JOptionPane.ERROR_MESSAGE);
            loadingDialog.hideDialog();
            return;
          }
          
          ImageProcessor processor = sourceImg.getProcessor().createProcessor(finalStackDimension.width, finalStackDimension.height);
          processor.insert(sourceImg.getProcessor(), maxOffsetX, maxOffsetY);
          
          virtualStack = new VirtualStack(finalStackDimension.width, finalStackDimension.height, ColorModel.getRGBdefault(), IJ.getDir("temp"));
          addToVirtualStack(new ImagePlus("", processor), virtualStack);
          
          for (int i = 0; i < manager.getNImages(); i++) {
            if (i == sourceImgIndex) continue;
            ImageProcessor newProcessor = new ColorProcessor(finalStackDimension.width, maximumSize.height);
            ImagePlus transformedImage = LeastSquareImageTransformation.transform(manager.get(i, true), sourceImg, dialogEvent.isRotate());
            
            BufferedImage transformedOriginalImage = manager.get(i, true);
            int offsetXOriginal = 0;
            if (offsetsX.get(i) < 0) offsetXOriginal = Math.abs(offsetsX.get(i));
            offsetXOriginal += maxOffsetXIndex != i ? maxOffsetX : 0;
            
            int offsetXTransformed = 0;
            if (offsetsX.get(i) > 0 && maxOffsetXIndex != i) offsetXTransformed = Math.abs(offsetsX.get(i));
            offsetXTransformed += maxOffsetX;
            
            int difference = (int) (managers.get(maxOffsetYIndex).getRoisAsArray()[0].getYBase() - managers.get(i).getRoisAsArray()[0].getYBase());
            newProcessor.insert(transformedOriginalImage.getProcessor(), offsetXOriginal, difference);
            if (transformedImage != null) {
              newProcessor.insert(transformedImage.getProcessor(), offsetXTransformed, (maxOffsetY));
            }
            addToVirtualStack(new ImagePlus("", newProcessor), virtualStack);
          }
        } else {
          BufferedImage sourceImg = manager.get(0, true);
          virtualStack = new VirtualStack(sourceImg.getWidth(), sourceImg.getHeight(), ColorModel.getRGBdefault(), IJ.getDir("temp"));
          addToVirtualStack(sourceImg, virtualStack);
          for (int i = 1; i < manager.getNImages(); i++) {
            ImagePlus img = LeastSquareImageTransformation.transform(manager.get(i, true), sourceImg, dialogEvent.isRotate());
            if (img != null) {
              addToVirtualStack(img, virtualStack);  
            }
          }
        }
        transformedImagesStack = new ImagePlus("", virtualStack);
        String filePath = IJ.getDir("temp") + transformedImagesStack.hashCode() + TIFF_EXT;
        
        new ImageConverter(transformedImagesStack).convertToRGB();
        new FileSaver(transformedImagesStack).saveAsTiff(filePath);
        tempImages.add(filePath);
        this.loadingDialog.hideDialog();
        alignDialog = new AlignDialog(transformedImagesStack, this);
        alignDialog.pack();
        alignDialog.setVisible(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
      this.loadingDialog.hideDialog();
    }, 10);
  }
  
  private void offsetHandler(int sourceImgIndex, BufferedImage sourceImg, List<Integer> offsetsX, List<Integer> offsetsY, List<RoiManager> managers) {
    IntStream.range(0, managers.size()).forEach(i -> {
      if (i == sourceImgIndex) {
        offsetsX.add(0);
        offsetsY.add(0);
        return;
      }
      Roi roi = managers.get(i).getRoisAsArray()[0];
      offsetsX.add((int) (roi.getXBase() - sourceImg.getManager().getRoisAsArray()[0].getXBase()));
      offsetsY.add((int) (roi.getYBase() - sourceImg.getManager().getRoisAsArray()[0].getYBase()));
    });
  }
  
  private void handleDeselectedRoi(DeselectedRoiEvent dialogEvent) {
    Arrays.stream(image.getManager().getSelectedRoisAsArray()).forEach(roi -> roi.setStrokeColor(Color.BLUE));
    image.getManager().select(dialogEvent.getRoiIndex());
    previewDialog.drawRois();
  }
  
  private void handleSelectedRoiFromOval(SelectedRoiFromOvalEvent dialogEvent) {
    mainDialog.lst_rois.setSelectedIndex(dialogEvent.getRoiIndex());
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
  
  private int getSourceIndex(Dimension maximumSize) {
    return manager.getImagesDimensions().indexOf(maximumSize);
  }
  
  private Dimension getMaxSizeImage(List<Dimension> dimensions) {
    Dimension maximumSize = new Dimension();
    for (Dimension dimension : dimensions) {
      if (dimension.width > maximumSize.width) {
        maximumSize.width = dimension.width;
      }
      if (dimension.height > maximumSize.height) {
        maximumSize.height = dimension.height;
      }
    }
    return maximumSize;
  }
  
  private void addToVirtualStack(ImagePlus img, VirtualStack virtualStack) {
    String path = IJ.getDir("temp") + img.getProcessor().hashCode() + TIFF_EXT;
    new FileSaver(img).saveAsTiff(path);
    virtualStack.addSlice(new File(path).getName());
    this.tempImages.add(path);
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
      new FileSaver(alignDialog.getImagePlus()).saveAsTiff(path);
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
        this.run();
      }
    }
  }
  
  private void showIfMemoryIsInsufficient(String pathFile) throws IOException, FormatException {
    try {
      long memory = ImageFile.estimateMemoryUsage(pathFile);
      totalMemory += memory;
      if (totalMemory >= Runtime.getRuntime().maxMemory()) {
        JOptionPane.showMessageDialog(null, INSUFFICIENT_MEMORY_MESSAGE, INSUFFICIENT_MEMORY_TITLE, JOptionPane.ERROR_MESSAGE);
        this.run();
      }
    } catch (UnknownFormatException e) {
      loadingDialog.hideDialog();
      JOptionPane.showMessageDialog(null, UNKNOWN_FORMAT_MESSAGE, UNKNOWN_FORMAT_TITLE, JOptionPane.ERROR_MESSAGE);
    }
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
  
  private String promptForFile() {
    OpenDialog od = new OpenDialog("Select an image");
    String dir = od.getDirectory();
    String name = od.getFileName();
    return (dir + name);
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
}

