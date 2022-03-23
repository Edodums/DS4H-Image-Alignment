package ds4h.dialog.main;

import ds4h.dialog.main.event.*;
import ds4h.image.buffered.BufferedImage;
import ds4h.image.buffered.event.RoiSelectedEvent;
import ij.IJ;
import ij.Prefs;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.Zoom;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.Stream;

public class MainDialog extends ImageWindow {
  private static final String DIALOG_STATIC_TITLE = "DS4H Image Alignment.";
  public static BufferedImage currentImage = null;
  private final OnMainDialogEventListener eventListener;
  private final JButton btnCopyCorners;
  private final JCheckBox checkShowPreview;
  private final JButton btnDeleteRoi;
  private final JButton btnPrevImage;
  private final JButton btnNextImage;
  private final JButton btnAlignImages;
  private final JCheckBox checkRotateImages;
  private final JCheckBox checkKeepOriginal;
  private final DefaultListModel<String> jListRoisModel;
  public JList<String> jListRois;
  private BufferedImage image;
  private boolean mouseOverCanvas;
  private Rectangle oldRect = null;
  
  public MainDialog(BufferedImage plus, OnMainDialogEventListener listener) {
    super(plus, new CustomCanvas(plus));
    this.image = plus;
    final CustomCanvas canvas = (CustomCanvas) getCanvas();
    checkShowPreview = new JCheckBox("Show preview window");
    checkShowPreview.setToolTipText("Show a preview window");
    btnDeleteRoi = new JButton("DELETE CORNER");
    btnDeleteRoi.setToolTipText("Delete current corner point selected");
    btnDeleteRoi.setEnabled(false);
    btnPrevImage = new JButton("PREV IMAGE");
    btnPrevImage.setToolTipText("Select previous image in the stack");
    btnNextImage = new JButton("NEXT IMAGE");
    btnNextImage.setToolTipText("Select next image in the stack");
    btnAlignImages = new JButton("ALIGN IMAGES");
    btnAlignImages.setToolTipText("Align the images based on the added corner points");
    btnAlignImages.setEnabled(false);
    JButton btnAutoAlignment = new JButton("AUTO-ALIGN IMAGES");
    btnAutoAlignment.setToolTipText("Align the images automatically without thinking what it is need to be done");
    btnAutoAlignment.setEnabled(true);
    checkRotateImages = new JCheckBox("Apply image rotation");
    checkRotateImages.setToolTipText("Apply rotation algorithms for improved images alignment.");
    checkRotateImages.setSelected(true);
    checkRotateImages.setEnabled(false);
    checkKeepOriginal = new JCheckBox("Keep all pixel data");
    checkKeepOriginal.setToolTipText("Keep the original images boundaries, applying stitching where necessary. NOTE: this operation is resource-intensive.");
    checkKeepOriginal.setSelected(true);
    checkKeepOriginal.setEnabled(false);
    // Remove the canvas from the window, to add it later
    removeAll();
    setTitle(DIALOG_STATIC_TITLE);
    // Training panel (left side of the GUI)
    JPanel cornersJPanel = new JPanel();
    cornersJPanel.setBorder(BorderFactory.createTitledBorder("Corners"));
    GridBagLayout trainingLayout = new GridBagLayout();
    GridBagConstraints trainingConstraints = new GridBagConstraints();
    trainingConstraints.anchor = GridBagConstraints.NORTHWEST;
    trainingConstraints.fill = GridBagConstraints.HORIZONTAL;
    trainingConstraints.gridwidth = 1;
    trainingConstraints.gridheight = 1;
    trainingConstraints.gridx = 0;
    trainingConstraints.gridy = 0;
    cornersJPanel.setLayout(trainingLayout);
    JLabel cornerLabel = new JLabel("Press \"C\" to add a corner point");
    cornerLabel.setForeground(Color.gray);
    cornersJPanel.add(cornerLabel, trainingConstraints);
    trainingConstraints.gridy++;
    trainingConstraints.gridy++;
    jListRois = new JList<>();
    JScrollPane scrollPane = new JScrollPane(jListRois);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setPreferredSize(new Dimension(180, 180));
    scrollPane.setMinimumSize(new Dimension(180, 180));
    scrollPane.setMaximumSize(new Dimension(180, 180));
    cornersJPanel.add(scrollPane, trainingConstraints);
    trainingConstraints.insets = new Insets(5, 0, 10, 0);
    trainingConstraints.gridy++;
    jListRois.setBackground(Color.white);
    jListRois.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    btnCopyCorners = new JButton();
    btnCopyCorners.setText("COPY CORNERS");
    btnCopyCorners.setEnabled(false);
    cornersJPanel.add(btnCopyCorners, trainingConstraints);
    cornersJPanel.setLayout(trainingLayout);
    // Options panel
    JPanel actionsJPanel = new JPanel();
    actionsJPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
    final GridBagLayout actionsLayout = new GridBagLayout();
    final GridBagConstraints actionsConstraints = new GridBagConstraints();
    actionsConstraints.anchor = GridBagConstraints.NORTHWEST;
    actionsConstraints.fill = GridBagConstraints.HORIZONTAL;
    actionsConstraints.weightx = 1;
    actionsConstraints.gridx = 0;
    actionsConstraints.insets = new Insets(5, 5, 6, 6);
    actionsJPanel.setLayout(actionsLayout);
    JLabel changeImageLabel = new JLabel("Press \"A\" or \"D\" to change image", JLabel.LEFT);
    changeImageLabel.setForeground(Color.gray);
    actionsJPanel.add(changeImageLabel, actionsConstraints);
    actionsJPanel.add(checkShowPreview, actionsConstraints);
    actionsJPanel.add(btnDeleteRoi, actionsConstraints);
    actionsJPanel.add(btnPrevImage, actionsConstraints);
    actionsJPanel.add(btnNextImage, actionsConstraints);
    actionsJPanel.setLayout(actionsLayout);
    // Options panel
    JPanel alignJPanel = new JPanel();
    alignJPanel.setBorder(BorderFactory.createTitledBorder("Alignment"));
    final GridBagLayout alignLayout = new GridBagLayout();
    final GridBagConstraints alignConstraints = new GridBagConstraints();
    alignConstraints.anchor = GridBagConstraints.NORTHWEST;
    alignConstraints.fill = GridBagConstraints.HORIZONTAL;
    alignConstraints.weightx = 1;
    alignConstraints.gridx = 0;
    alignConstraints.insets = new Insets(5, 5, 6, 6);
    alignJPanel.setLayout(alignLayout);
    alignJPanel.add(checkRotateImages, actionsConstraints);
    alignJPanel.add(checkKeepOriginal, actionsConstraints);
    alignJPanel.add(btnAlignImages, actionsConstraints);
    alignJPanel.add(btnAutoAlignment, actionsConstraints);
    alignJPanel.setLayout(alignLayout);
    // Buttons panel
    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setBackground(Color.GRAY);
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
    buttonsPanel.add(cornersJPanel);
    buttonsPanel.add(actionsJPanel);
    buttonsPanel.add(alignJPanel);
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints allConstraints = new GridBagConstraints();
    Panel all = new Panel();
    all.setLayout(layout);
    // sets little of padding to ensure that the @ImagePlus text is shown and not covered by the panel
    allConstraints.insets = new Insets(5, 0, 0, 0);
    allConstraints.anchor = GridBagConstraints.NORTHWEST;
    allConstraints.gridwidth = 1;
    allConstraints.gridheight = 1;
    allConstraints.gridx = 0;
    allConstraints.gridy = 0;
    allConstraints.weightx = 0;
    allConstraints.weighty = 0;
    all.add(buttonsPanel, allConstraints);
    allConstraints.gridx++;
    allConstraints.weightx = 1;
    allConstraints.weighty = 1;
    // this is just a cheap trick i made 'cause i don't properly know java swing: let's fake the background of the window so the it seems the column on the left is full length vertically
    all.setBackground(new Color(238, 238, 238));
    all.add(canvas, allConstraints);
    GridBagLayout wingb = new GridBagLayout();
    GridBagConstraints winc = new GridBagConstraints();
    winc.insets = new Insets(5, 0, 0, 0);
    winc.anchor = GridBagConstraints.NORTHWEST;
    winc.fill = GridBagConstraints.BOTH;
    winc.weightx = 1;
    winc.weighty = 1;
    setLayout(wingb);
    add(all, winc);
    // Propagate all listeners
    Stream.<Component>of(all, buttonsPanel).forEach(component -> Arrays.stream(getKeyListeners()).forEach(component::addKeyListener));
    this.eventListener = listener;
    btnCopyCorners.addActionListener(e -> this.eventListener.onMainDialogEvent(new CopyCornersEvent()));
    checkShowPreview.addItemListener(e -> this.eventListener.onMainDialogEvent(new PreviewImageEvent(checkShowPreview.isSelected())));
    btnDeleteRoi.addActionListener(e -> {
      int index = jListRois.getSelectedIndex();
      this.eventListener.onMainDialogEvent(new DeleteRoiEvent(jListRois.getSelectedIndex()));
      jListRois.setSelectedIndex(index);
    });
    btnPrevImage.addActionListener(e -> this.eventListener.onMainDialogEvent(new ChangeImageEvent(ChangeImageEvent.ChangeDirection.PREV)));
    btnNextImage.addActionListener(e -> this.eventListener.onMainDialogEvent(new ChangeImageEvent(ChangeImageEvent.ChangeDirection.NEXT)));
    btnAlignImages.addActionListener(e -> this.eventListener.onMainDialogEvent(new AlignEvent(checkRotateImages.isSelected(), checkKeepOriginal.isSelected())));
    btnAutoAlignment.addActionListener(e -> this.eventListener.onMainDialogEvent(new AutoAlignEvent()));
    
    // Markers addition handlers
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyboardEventDispatcher());
    canvas.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        // Check if the Rois in the image have changed position by user input; if so, update the list and notify the controller
        Rectangle bounds = getImagePlus().getRoi().getBounds();
        if (!bounds.equals(oldRect)) {
          oldRect = (Rectangle) bounds.clone();
          eventListener.onMainDialogEvent(new MovedRoiEvent());
        }
      }
    });
    canvas.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        mouseOverCanvas = true;
        super.mouseEntered(e);
      }
      
      @Override
      public void mouseExited(MouseEvent e) {
        mouseOverCanvas = false;
        super.mouseExited(e);
      }
    });
    // Rois list handling
    jListRois.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    jListRoisModel = new DefaultListModel<>();
    jListRois.setModel(jListRoisModel);
    jListRois.setSelectionModel(new DefaultListSelectionModel() {
      // Thanks to https://stackoverflow.com/a/31336378/1306679
      @Override
      public void setSelectionInterval(int startIndex, int endIndex) {
        if (startIndex == endIndex) {
          eventListener.onMainDialogEvent(new SelectedRoiEvent(startIndex));
          btnDeleteRoi.setEnabled(true);
          super.setSelectionInterval(startIndex, endIndex);
        }
      }
    });
    MenuBar menuBar = new MenuBar();
    Menu fileMenu = new Menu("File");
    MenuItem menuItem = new MenuItem("Open file...");
    menuItem.addActionListener(e -> eventListener.onMainDialogEvent(new OpenFileEvent()));
    fileMenu.add(menuItem);
    menuItem = new MenuItem("Add image to current stack");
    menuItem.addActionListener(e -> {
      JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory(new File(image.getFilePath()));
      chooser.setMultiSelectionEnabled(true);
      chooser.showOpenDialog(null);
      File[] files = chooser.getSelectedFiles();
      Arrays.stream(files).forEach(file -> eventListener.onMainDialogEvent(new AddFileEvent(file.getPath())));
    });
    fileMenu.add(menuItem);
    menuItem = new MenuItem("Remove image...");
    menuItem.addActionListener(e -> eventListener.onMainDialogEvent(new RemoveImageEvent()));
    fileMenu.add(menuItem);
    fileMenu.addSeparator();
    menuItem = new MenuItem("Exit");
    menuItem.addActionListener(e -> eventListener.onMainDialogEvent(new ExitEvent()));
    fileMenu.add(menuItem);
    Menu aboutMenu = new Menu("?");
    menuItem = new MenuItem("About...");
    menuItem.addActionListener(e -> eventListener.onMainDialogEvent(new OpenAboutEvent()));
    aboutMenu.add(menuItem);
    MainDialog.currentImage = image;
    menuBar.add(fileMenu);
    menuBar.add(aboutMenu);
    this.addEventListenerToImage();
    this.setMenuBar(menuBar);
    new Zoom().run("scale");
    pack();
  }
  
  /**
   * Change the actual image displayed in the main view, based on the given BufferedImage istance
   *
   * @param image
   */
  public void changeImage(BufferedImage image) {
    if (image != null) {
      MainDialog.currentImage = image;
      this.setImage(image);
      image.backupRois();
      image.getManager().reset();
      this.image = image;
      if (jListRois.getSelectedIndex() == -1) {
        btnDeleteRoi.setEnabled(false);
      }
      this.image.restoreRois();
      this.drawRois(image.getManager());
      this.addEventListenerToImage();
      // Let's call the zoom plugin to scale the image to fit in the user window
      // The zoom scaling command works on the current active window: to be 100% sure it will work, we need to forcefully select the preview window.
      IJ.selectWindow(this.getImagePlus().getID());
      new Zoom().run("scale");
      this.pack();
    }
  }
  
  /**
   * Adds an event listener to the current image
   */
  private void addEventListenerToImage() {
    MainDialog root = this;
    this.image.addEventListener(event -> {
      if (event instanceof RoiSelectedEvent) {
        // if a roi is marked as selected, select the appropriate ROI in the listbox in the left of the window
        RoiSelectedEvent roiSelectedEvent = (RoiSelectedEvent) event;
        int index = Arrays.asList(root.image.getManager().getRoisAsArray()).indexOf(roiSelectedEvent.getRoiSelected());
        eventListener.onMainDialogEvent(new SelectedRoiFromOvalEvent(index));
      }
    });
  }
  
  /**
   * Update the Roi List based on the given RoiManager instance
   * // THIS PIECE OF CODE IS REPEATED A LOT OF TIMES //
   * // TAKE IT INTO ACCOUNT WHEN MAKING THE SERIOUS REFACTORING //
   * @param manager
   */
  public void drawRois(RoiManager manager) {
    Prefs.useNamesAsLabels = true;
    Prefs.noPointLabels = false;
    int strokeWidth = Math.max((int) (image.getWidth() * 0.0025), 3);
    Overlay over = new Overlay();
    over.drawBackgrounds(false);
    over.drawLabels(false);
    over.drawNames(true);
    over.setLabelFontSize(Math.round(strokeWidth * 1f), "scale");
    over.setLabelColor(Color.BLUE);
    over.setStrokeWidth((double) strokeWidth);
    over.setStrokeColor(Color.BLUE);
    Arrays.stream(image.getManager().getRoisAsArray()).forEach(over::add);
    image.getManager().setOverlay(over);
    this.refreshROIList(manager);
    if (jListRois.getSelectedIndex() == -1) {
      btnDeleteRoi.setEnabled(false);
    }
  }
  
  public void refreshROIList(RoiManager manager) {
    jListRoisModel.removeAllElements();
    int idx = 0;
    for (Roi roi : manager.getRoisAsArray()) {
      jListRoisModel.add(idx++, MessageFormat.format("{0} - {1},{2}", idx, (int) roi.getXBase() + (int) (roi.getFloatWidth() / 2), (int) roi.getYBase() + (int) (roi.getFloatHeight() / 2)));
    }
  }
  
  public void setPreviewWindowCheckBox(boolean value) {
    this.checkShowPreview.setSelected(value);
  }
  
  public void setNextImageButtonEnabled(boolean enabled) {
    this.btnNextImage.setEnabled(enabled);
  }
  
  public void setPrevImageButtonEnabled(boolean enabled) {
    this.btnPrevImage.setEnabled(enabled);
  }
  
  public void setAlignButtonEnabled(boolean enabled) {
    this.btnAlignImages.setEnabled(enabled);
    this.checkRotateImages.setEnabled(enabled);
    this.checkKeepOriginal.setEnabled(enabled);
  }
  
  public void setCopyCornersEnabled(boolean enabled) {
    this.btnCopyCorners.setEnabled(enabled);
  }
  
  @Override
  public void setTitle(String title) {
    super.setTitle(DIALOG_STATIC_TITLE + " " + title);
  }
  
  // a simple debounce variable that can put "on hold" a key_release event
  private boolean debounce = false;
  private class KeyboardEventDispatcher implements KeyEventDispatcher {
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
      boolean isReleased = e.getID() == KeyEvent.KEY_RELEASED;
      if (isReleased && e.getKeyCode() == KeyEvent.VK_C && mouseOverCanvas) {
        Point clickCoordinates = getCanvas().getCursorLoc();
        eventListener.onMainDialogEvent(new AddRoiEvent(clickCoordinates));
      }
      if (!debounce) {
        debounce = true;
        new Thread(() -> {
          try {
            ChangeImageEvent.ChangeDirection direction = null;
            if (isReleased && e.getKeyCode() == KeyEvent.VK_A) {
              direction = ChangeImageEvent.ChangeDirection.PREV;
            }
            if (isReleased && e.getKeyCode() == KeyEvent.VK_D) {
              direction = ChangeImageEvent.ChangeDirection.NEXT;
            }
            if (direction != null) {
              eventListener.onMainDialogEvent(new ChangeImageEvent(direction));
              e.consume();
            }
          } catch (Exception e1) {
            e1.printStackTrace();
          }
        }).start();
      }
      return false;
    }
  }
}
