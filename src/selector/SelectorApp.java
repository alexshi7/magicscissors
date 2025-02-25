package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import selector.SelectionModel.SelectionState;
import scissors.ScissorsSelectionModel;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;
    // New in A6
    /**
     * Progress bar to indicate the progress of a model that needs to do long calculations in a
     * PROCESSING state.
     */
    private JProgressBar processingProgress;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        // New in A6: Add progress bar
        processingProgress = new JProgressBar();
        frame.add(processingProgress, BorderLayout.PAGE_START);

        // Add status bar
        statusLabel = new JLabel();
        frame.add(statusLabel,BorderLayout.PAGE_END);

        // Add image component with scrollbars
        imgPanel = new ImagePanel();
        JScrollPane scroll = new JScrollPane(imgPanel);
        scroll.setPreferredSize(new Dimension(400,700));
        frame.add(scroll, BorderLayout.CENTER);


        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        frame.add(makeControlPanel(),BorderLayout.LINE_END);

        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);



        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        //embellishment
        openItem.setMnemonic(KeyEvent.VK_A);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.SHIFT_MASK));
        saveItem.setMnemonic(KeyEvent.VK_B);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.SHIFT_MASK));
        closeItem.setMnemonic(KeyEvent.VK_C);
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.SHIFT_MASK));
        exitItem.setMnemonic(KeyEvent.VK_D);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.SHIFT_MASK));

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(5,0));
        cancelButton = new JButton(("Cancel"));
        undoButton = new JButton(("Undo"));
        resetButton = new JButton(("Reset"));
        finishButton = new JButton(("Finish"));
        String[] selections = {"Point-to-Point","Intelligent Scissors"};
        JComboBox SelectionModels = new JComboBox(selections);
        SelectionModels.setMaximumSize(new Dimension(SelectionModels.getPreferredSize().width,
                SelectionModels.getPreferredSize().height));
        buttons.add(SelectionModels);
        buttons.add(cancelButton);
        buttons.add(undoButton);
        buttons.add(resetButton);
        buttons.add(finishButton);
        SelectionModels.addActionListener(e -> {
            JComboBox cb = (JComboBox)e.getSource();
            String currentselection = (String)cb.getSelectedItem();
            if (currentselection.equals("Point-to-Point")){
                SelectionModel tracker = new PointToPointSelectionModel(model);
                setSelectionModel(tracker);
            }
            if (currentselection.equals("Intelligent Scissors")){
                SelectionModel tracker = new ScissorsSelectionModel("CrossGradMono",model);
                setSelectionModel(tracker);
            }

        });

        cancelButton.addActionListener(e -> model.cancelProcessing());
        undoButton.addActionListener(e -> model.undo());
        resetButton.addActionListener(e -> model.reset());
        finishButton.addActionListener(e -> model.finishSelection());

        return buttons;
        // throw new UnsupportedOperationException();  // Replace this line
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include:
     * * "state": Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (("progress".equals(evt.getPropertyName()))){
            int progress = (int) evt.getNewValue();
            System.out.println("Received progress update: " + progress);
            processingProgress.setValue(progress);
            processingProgress.setIndeterminate(false);
        }
        if ("state".equals(evt.getPropertyName())) {
                reflectSelectionState(model.state());
                if (model.state().equals(PROCESSING)){
                    System.out.println("Setting progress bar to indeterminate mode.");
                    processingProgress.setIndeterminate(true);
                } else {
                    processingProgress.setIndeterminate(false);
                    processingProgress.setValue(0);
                }
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());


        cancelButton.setEnabled(state==PROCESSING);
        undoButton.setEnabled(state!=NO_SELECTION);
        resetButton.setEnabled(state!=NO_SELECTION);
        finishButton.setEnabled(state==SELECTING);

    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());
        // New in A6: Listen for "progress" events
        model.addPropertyChangeListener("progress", this);
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));

        int returnval = chooser.showOpenDialog(frame);
        if (returnval == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null){
                    this.setImage(img);
                }else{
                    JOptionPane.showMessageDialog(frame,"Invalid Image File at "+file.getName(),
                            "Unsupported Image Format",
                            JOptionPane.ERROR_MESSAGE);
                    openImage();
                }
            }catch(IOException e){
                JOptionPane.showMessageDialog(frame,"File does not exist "+ file.getName(),"Error"
                        ,JOptionPane.ERROR_MESSAGE);
                openImage();
            }
        }
        //throw new UnsupportedOperationException();  // Replace this line
    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog.
     * Show an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));

        int retVal = chooser.showSaveDialog(frame);
        if (retVal==JFileChooser.APPROVE_OPTION){
            File ans = chooser.getSelectedFile();
            if (ans.getName().toLowerCase().endsWith(".png")==false){
                ans = new File(ans.getAbsolutePath() + ".png");
            }
            if (ans.exists()){
                Object[] options = {"Yes","No","Cancel"};
                int retVal2 = JOptionPane.showOptionDialog(frame,"Are you sure you want to overwrite this file"
                        ,"File ALready Exists",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,
                        null,options,options[2]);
                if (retVal2 == JOptionPane.YES_OPTION){
                    try{
                        model.saveSelection(new FileOutputStream(ans));
                    }
                    catch (IOException e){
                        JOptionPane.showMessageDialog(frame, e.getClass().getSimpleName() + ": " + e.getMessage(),
                                "Error Saving Image", JOptionPane.ERROR_MESSAGE);
                    }
                    catch (IllegalStateException e){
                        JOptionPane.showMessageDialog(frame,e.getMessage(),"No image available to save",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                if (retVal2 == JOptionPane.NO_OPTION){
                    saveSelection();
                }
                if (retVal2 == JOptionPane.CANCEL_OPTION){
                    saveSelection();
                }
            }
            try{
                model.saveSelection(new FileOutputStream(ans));

            }
            catch (IOException e){
                JOptionPane.showMessageDialog(frame, e.getClass().getSimpleName() + ": " + e.getMessage(),
                        "Error Saving Image", JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }

            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}
