package com.cleanroommc.relauncher;

import com.google.common.base.Strings;
import net.miginfocom.swing.MigLayout;
import net.minecraftforge.fml.ExitWrapper;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.function.Consumer;

public class Initializer {
    private static final String valid = "✔";
    private static final String invalid = "❌";
    private static volatile boolean verified = false;
    private static final JLabel mainStatusLabel = new JLabel();
    private static final JProgressBar mainProgressbar = new JProgressBar();
    private static JButton launchButton;
    private static final JFrame mainFrame = new JFrame();
    private static Consumer<Boolean> setInteractable;
    private static Runnable verifyJVM;
    
    public static void InitJavaAndArg() {
        Config.syncConfig();
        if (Strings.isNullOrEmpty(Config.javaPath) || !isJavaNewerThan21(Config.javaPath)) {
            checkJavaAndWarn();
        }
        mainFrame.setLayout(new MigLayout("", "[grow][grow][grow][grow]", "[grow][grow][grow][grow][grow][grow][grow]"));

        JLabel pathLabel = new JLabel("Java Path*");
        JTextField pathText = new JTextField();
        JLabel jvmStatus = new JLabel(invalid);
        JFileChooser jvmPicker = getJavaFileChooser();
        JButton detectJvmButton = new JButton("Detect Java");
        JButton browserButton = new JButton("Browse Java");
        JLabel argsLabel = new JLabel("Java Args");
        JTextField args = new JTextField();
        JButton advSetting = new JButton("Advanced Settings");

        setInteractable = value -> {
            launchButton.setEnabled(value);
            pathText.setEnabled(value);
            detectJvmButton.setEnabled(value);
            browserButton.setEnabled(value);
            args.setEnabled(value);
            advSetting.setEnabled(value);
        };

        launchButton = new JButton("Launch");
        launchButton.setEnabled(false);
        // java related
        mainFrame.add(pathLabel, "cell 0 0, grow");
        pathLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(pathLabel);
        mainFrame.add(pathText, "cell 1 0 2 1, grow");
        pathText.setMinimumSize(new Dimension(300, 10));
        GUIUtils.enlargeFont(pathText);
        mainFrame.add(jvmStatus, "cell 3 0, grow");
        jvmStatus.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(jvmStatus);
        mainFrame.add(detectJvmButton, "cell 0 1 2 1, grow");
        GUIUtils.enlargeFont(detectJvmButton);
        mainFrame.add(browserButton, "cell 2 1 21, grow");
        GUIUtils.enlargeFont(browserButton);
        
        mainFrame.add(new JSeparator(JSeparator.HORIZONTAL), "cell 0 2 4 1, grow");
        // args
        mainFrame.add(argsLabel, "cell 0 3, grow");
        argsLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(argsLabel);
        mainFrame.add(args, "cell 1 3 3 1, grow");
        args.setMinimumSize(new Dimension(300, 10));
        GUIUtils.enlargeFont(args);

        // adv settings
        mainFrame.add(advSetting, "cell 0 4 4 1, grow");
        advSetting.setMinimumSize(new Dimension(300, 10));
        GUIUtils.enlargeFont(advSetting);

        mainFrame.add(new JSeparator(JSeparator.HORIZONTAL), "cell 0 5 4 1, grow");
        
        mainFrame.add(mainStatusLabel, "cell 0 6 4 1, grow");
        mainStatusLabel.setMinimumSize(new Dimension(300, 10));
        mainStatusLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(mainStatusLabel);
        mainFrame.add(mainProgressbar, "cell 0 7 4 1, grow");
        mainProgressbar.setMinimumSize(new Dimension(300, 10));
        GUIUtils.enlargeFont(mainProgressbar);
        
        mainFrame.add(launchButton, "cell 0 8 4 1, grow");
        launchButton.setMinimumSize(new Dimension(300, 10));
        GUIUtils.enlargeFont(launchButton, Font.BOLD, 20);

        mainFrame.setTitle("Relauncher Initialization Settings");
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                ExitWrapper.exit(0);
            }
        });
        
        pathLabel.setHorizontalAlignment(JLabel.CENTER);
        pathText.setToolTipText("Input your Java 21+ executable here");

        detectJvmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(detectJvmButton)) {
                showDetectorDialog(pathText);
            }
        });
        
        pathText.setText(Config.javaPath);
        args.setText(Config.jvmArgs);
        
        advSetting.addActionListener(actionEvent -> showAdvancedSettingDialog());

        launchButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(launchButton)) {
                if (verified || isJavaNewerThan21(pathText.getText())) {
                    Relauncher.LOGGER.info("Java valid and saved");
                    setInteractable.accept(false);
                    Config.javaPath = pathText.getText();
                    Config.jvmArgs = args.getText();

                    mainProgressbar.setIndeterminate(false);

                    try {
                        Thread workingThread = new Thread(() -> {
                            try {
                                MMCPackDownloader.downloadAndExtract();
                                MMCPackParser.parseMMCPack();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            synchronized (Relauncher.o) {
                                Relauncher.o.notify();
                            }
                        }, "Relauncher Working Thread");
                        workingThread.start();
                    } catch (Throwable t) {
                        Relauncher.LOGGER.error(t.getMessage());
                        Arrays.stream(t.getStackTrace()).forEach(Relauncher.LOGGER::info);
                        mainStatusLabel.setText(t.getMessage());
                        setInteractable.accept(true);
                        mainProgressbar.setIndeterminate(true);
                    }
                } else {
                    Relauncher.LOGGER.warn("Invalid Java");
                    launchButton.setEnabled(false);
                    verifyJVM.run();
                }
            }
        });
        
        verifyJVM = () -> {
            if (isJavaNewerThan21(pathText.getText())) {
                verified = true;
                jvmStatus.setText(valid);
                jvmStatus.setForeground(Color.GREEN);

                launchButton.setEnabled(true);
            } else {
                verified = false;
                jvmStatus.setText(invalid);
                jvmStatus.setForeground(Color.RED);

                launchButton.setEnabled(false);
            }
        };

        verifyJVM.run();

        pathText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                onChange();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                onChange();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                onChange();
            }

            private void onChange() {
                verifyJVM.run();
                pathText.setToolTipText(pathText.getText());
            }
        });
        
        browserButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(browserButton)) {
                GUIUtils.setCentral(jvmPicker);
                int r = jvmPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    pathText.setText(jvmPicker.getSelectedFile().getAbsolutePath());
                    verifyJVM.run();
                }
            }
        });

        args.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                onChange();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                onChange();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                onChange();
            }

            private void onChange() {
                args.setToolTipText(pathText.getText());
            }
        });

        argsLabel.setHorizontalAlignment(JLabel.CENTER);

        mainProgressbar.setIndeterminate(true);
        mainProgressbar.addChangeListener(changeEvent -> mainStatusLabel.setText(String.format("Downloading: %d/%d", mainProgressbar.getValue(), mainProgressbar.getMaximum())));
        mainStatusLabel.setHorizontalAlignment(JTextField.CENTER);
        mainStatusLabel.setText("Status: Idle");

        Relauncher.LOGGER.info("Launching GUI");
        mainFrame.validate();
        mainFrame.pack();
        GUIUtils.setCentral(mainFrame);
        mainFrame.setVisible(true);
        mainFrame.setMinimumSize(mainFrame.getSize());
        synchronized (Relauncher.o) {
            try {
                Relauncher.o.wait();
            } catch (InterruptedException e) {
                Relauncher.LOGGER.error(e);
            }
        }

    }

    private static JFileChooser getJavaFileChooser() {
        JFileChooser jvmPicker = new JFileChooser(SystemUtils.getUserDir());

        jvmPicker.setMultiSelectionEnabled(false);
        jvmPicker.setAcceptAllFileFilterUsed(false);
        jvmPicker.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().equals("javaw.exe") || file.getName().equals("java") || file.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Java executable, javaw.exe on Windows and java on other OS";
            }
        });
        jvmPicker.setFileHidingEnabled(false);
        jvmPicker.setFileSelectionMode(JFileChooser.FILES_ONLY);
        return jvmPicker;
    }

    private static JFileChooser getLibraryPathChooser() {
        JFileChooser jvmPicker = new JFileChooser(SystemUtils.getUserDir());

        jvmPicker.setMultiSelectionEnabled(false);
        jvmPicker.setAcceptAllFileFilterUsed(false);
        jvmPicker.setFileHidingEnabled(false);
        jvmPicker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return jvmPicker;
    }

    public static boolean isJavaNewerThan21(String path) {
        if (path == null) return false;
        Relauncher.LOGGER.info("Checking path {}", path);
        return new JVMInfo(path).getSpecification() > 20;
    }

    public static JLabel getMainStatusLabel() {
        return mainStatusLabel;
    }

    public static JProgressBar getMainProgressbar() {
        return mainProgressbar;
    }
    
    public static JButton getLaunchButton() {
        return launchButton;
    }

    public static void setGUIInteractable(boolean enable) {
        setInteractable.accept(enable);
    }

    public static void hideWindow() {
        mainFrame.setVisible(false);
    }

    private static void showDetectorDialog(JTextField pathField) {
        JDialog detector = new JDialog(mainFrame.getOwner());
        detector.setFocusableWindowState(true);
        detector.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        detector.setLayout(new MigLayout(
                "",
                "[grow][grow]",
                "[grow][grow][grow]"
        ));
        DefaultListModel<JVMInfo> model = new DefaultListModel<>();
        JList<JVMInfo> list = new JList<>(model);
        JLabel info = new JLabel("Status: Idle");
        info.setHorizontalAlignment(JLabel.CENTER);
        JButton confirm = new JButton("Confirm");
        JButton cancel = new JButton("Cancel");
        
        detector.add(list, "cell 0 0 2 2, grow");
        GUIUtils.enlargeFont(list);
        detector.add(info, "cell 0 2 2 1, grow");
        GUIUtils.enlargeFont(info);
        detector.add(cancel, "cell 0 3, grow");
        GUIUtils.enlargeFont(cancel);
        detector.add(confirm, "cell 1 3, grow");
        GUIUtils.enlargeFont(confirm, Font.BOLD, 20);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.addListSelectionListener(listSelectionEvent -> {
            String path = list.getSelectedValue().getFile().getAbsolutePath();
            info.setText(path);
            info.setToolTipText(path);
            detector.pack();
        });
        list.setMinimumSize(new Dimension(300, 200));

        cancel.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(cancel)) {
                detector.setVisible(false);
                setGUIInteractable(true);
            }
        });

        confirm.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirm)) {
                detector.setVisible(false);
                setGUIInteractable(true);
                pathField.setText(list.getSelectedValue().getFile().getAbsolutePath());
                verifyJVM.run();
            }
        });

        cancel.setEnabled(false);
        confirm.setEnabled(false);
        info.setText("Scanning...");
        Relauncher.LOGGER.info("Scanning...");
        detector.pack();
        GUIUtils.setCentral(detector);
        detector.setAlwaysOnTop(true);
        setGUIInteractable(false);
        detector.setVisible(true);
        detector.setMinimumSize(detector.getSize());

        Thread scan = new Thread(() -> {
            for (JVMInfo i : JavaDetector.getInstalledJVMs()) {
                model.addElement(i);
            }
            cancel.setEnabled(true);
            confirm.setEnabled(true);
            info.setText("Scan complete.");
            detector.pack();
            GUIUtils.setCentral(detector);
        });
        scan.start();

    }

    private static void showAdvancedSettingDialog() {
        JDialog advSetting = new JDialog(mainFrame.getOwner());
        advSetting.setLayout(new MigLayout(
                "",
                "[grow][grow][grow][grow]",
                "[grow][grow][grow][grow][grow][grow][grow][grow]"
        ));
        advSetting.setFocusableWindowState(true);
        JLabel libraryPathLabel = new JLabel("Library Path");
        libraryPathLabel.setHorizontalAlignment(JLabel.CENTER);
        JTextField libraryPathText = new JTextField();
        libraryPathText.setText(Config.libraryPath);
        JButton cancel = new JButton("Cancel");
        JButton confirm = new JButton("Confirm");
        JFileChooser libraryPicker = getLibraryPathChooser();
        JCheckBox groupNameInPathCheckbox = new JCheckBox("Place Libraries in Group Name");
        groupNameInPathCheckbox.setSelected(Config.respectLibraryStructure);
        JButton libraryBrowserButton = new JButton("Browser...");
        JLabel proxyLabel = new JLabel("Proxy Host");
        proxyLabel.setHorizontalAlignment(JLabel.CENTER);
        JLabel proxyPortLabel = new JLabel("Proxy Port");
        proxyPortLabel.setHorizontalAlignment(JLabel.CENTER);
        JTextField proxyAddrTextField = new JTextField();
        proxyAddrTextField.setText(Config.proxyAddr);
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(Config.proxyPort, 0, 65535, 1));
        JCheckBox useLocalCheckbox = new JCheckBox("Use Local MMC Pack");
        useLocalCheckbox.setSelected(Config.useLocalPack);
        JCheckBox chineseModeCheckbox = new JCheckBox("Use Chinese Main Land Mirror");
        chineseModeCheckbox.setSelected(Config.chineseMode);
        JLabel maxRetryLabel = new JLabel("Max retry");
        maxRetryLabel.setHorizontalAlignment(JLabel.CENTER);
        JSpinner maxRetrySpinner = new JSpinner(new SpinnerNumberModel(Config.maxRetry, 1, 65535, 1));
        JLabel maxSessionLabel = new JLabel("Max sessions");
        maxSessionLabel.setHorizontalAlignment(JLabel.CENTER);
        JSpinner maxSessionSpinner = new JSpinner(new SpinnerNumberModel(Config.maxDownloadSession, 1, 65535, 1));

        libraryPathText.setToolTipText("Path to place the libraries, leave it empty to use default location");

        advSetting.add(libraryPathLabel, "cell 0 0, grow");
        GUIUtils.enlargeFont(libraryPathLabel);
        advSetting.add(libraryPathText, "cell 1 0 3 1, grow");
        GUIUtils.enlargeFont(libraryPathText);
        advSetting.add(groupNameInPathCheckbox, "cell 1 1 2 1, grow");
        GUIUtils.enlargeFont(groupNameInPathCheckbox);
        advSetting.add(libraryBrowserButton, "cell 3 1, grow");
        GUIUtils.enlargeFont(libraryBrowserButton);
        
        advSetting.add(new JSeparator(JSeparator.HORIZONTAL), "cell 0 2 4 1, grow");

        advSetting.add(chineseModeCheckbox, "cell 0 3 2 1, grow");
        GUIUtils.enlargeFont(chineseModeCheckbox);
        advSetting.add(useLocalCheckbox, "cell 2 3 2 1, grow");
        GUIUtils.enlargeFont(useLocalCheckbox);
        
        advSetting.add(proxyLabel, "cell 0 4, grow");
        GUIUtils.enlargeFont(proxyLabel);
        advSetting.add(proxyAddrTextField, "cell 1 4, grow");
        GUIUtils.enlargeFont(proxyAddrTextField);
        advSetting.add(proxyPortLabel, "cell 2 4,grow");
        GUIUtils.enlargeFont(proxyPortLabel);
        advSetting.add(portSpinner, "cell 3 4, grow");
        GUIUtils.enlargeFont(portSpinner);
        
        advSetting.add(maxRetryLabel, "cell 0 5, grow");
        GUIUtils.enlargeFont(maxRetryLabel);
        advSetting.add(maxRetrySpinner, "cell 1 5, grow");
        GUIUtils.enlargeFont(maxRetrySpinner);
        advSetting.add(maxSessionLabel, "cell 2 5, grow");
        GUIUtils.enlargeFont(maxSessionLabel);
        advSetting.add(maxSessionSpinner, "cell 3 5, grow");
        GUIUtils.enlargeFont(maxSessionSpinner);
        
        advSetting.add(new JSeparator(JSeparator.HORIZONTAL), "cell 0 6 4 1, grow");

        advSetting.add(cancel, "cell 0 7 2 1, grow");
        GUIUtils.enlargeFont(cancel);
        advSetting.add(confirm, "cell 2 7 2 1, grow");
        GUIUtils.enlargeFont(confirm, Font.BOLD, 20);
        
        groupNameInPathCheckbox.setToolTipText("Place libraries in their corresponding groups. Useful when you want to reuse libraries with the launcher.");
        
        chineseModeCheckbox.setToolTipText("Use mirror and proxy sites to speed up download from Chinese main land.");

        
        libraryBrowserButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(libraryBrowserButton)) {
                GUIUtils.setCentral(libraryPicker);
                int r = libraryPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    libraryPathText.setText(libraryPicker.getSelectedFile().getAbsolutePath());
                }
            }
        });
        

        useLocalCheckbox.setToolTipText("Will use first Cleanroom-MMC-instance-*.zip in relauncher dir");

        proxyAddrTextField.setToolTipText("Proxy Address, leave it empty means no proxy");
        
        portSpinner.setToolTipText("Proxy Port, leave it 0 means no proxy");
        portSpinner.setModel(new SpinnerNumberModel());
        
        
        cancel.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(cancel)) {
                advSetting.setVisible(false);
                setGUIInteractable(true);
            }
        });
        
        confirm.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirm)) {
                advSetting.setVisible(false);
                setGUIInteractable(true);
                Config.chineseMode = chineseModeCheckbox.isSelected();
                Config.useLocalPack = useLocalCheckbox.isSelected();
                Config.proxyPort = (int) portSpinner.getValue();
                Config.proxyAddr = proxyAddrTextField.getText();
                Config.maxDownloadSession = (int) maxSessionSpinner.getValue();
                Config.maxRetry = (int) maxRetrySpinner.getValue();
                Config.respectLibraryStructure = groupNameInPathCheckbox.isSelected();
            }
        });

        advSetting.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        advSetting.setAlwaysOnTop(true);
        advSetting.pack();
        GUIUtils.setCentral(advSetting);
        advSetting.setMinimumSize(advSetting.getSize());
        setGUIInteractable(false);
        advSetting.setVisible(true);
        advSetting.requestFocus();
    }
    
    private static void checkJavaAndWarn() {
        JDialog warning = new JDialog();
        warning.setLayout(new MigLayout());
        JLabel icon = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        GUIUtils.enlargeFont(icon);
        JLabel oldJava = new JLabel("You are using an outdated Java VM, relauncher may hang and need manually retry.");
        oldJava.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(oldJava);
        JLabel oldJava2 = new JLabel("To avoid potential malfunction, please click the button to download latest java 8.");
        oldJava2.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(oldJava2);
        LinkButton java8button = new LinkButton("https://adoptium.net/temurin/releases?version=8&os=any&arch=any");
        GUIUtils.enlargeFont(java8button);
        JLabel newJava = new JLabel("You will need at least one Java 21+ JRE installed to use relauncher.");
        newJava.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(newJava);
        JLabel newJava2 = new JLabel("If you don't, please click the button to download one.");
        newJava2.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(newJava2);
        LinkButton java21button = new LinkButton("https://adoptium.net/temurin/releases?version=21&os=any&arch=any");
        GUIUtils.enlargeFont(java21button);
        JButton confirm = new JButton("Continue");
        GUIUtils.enlargeFont(confirm, Font.BOLD, 20);
        JButton quit = new JButton("Quit");
        GUIUtils.enlargeFont(quit);
        
        warning.add(icon, "cell 0 0 2 1, grow");
        
        if (JVMInfo.getCurrentUpdateNumber() < 341 && JVMInfo.getCurrentUpdateNumber() > 0) {
            warning.add(oldJava, "cell 0 1, grow");
            warning.add(oldJava2, "cell 0 2, grow");
            warning.add(java8button, "cell 1 2, grow");
            
            warning.add(new JSeparator(JSeparator.HORIZONTAL), "cell 0 3 2 1, grow");
            
            warning.add(newJava, "cell 0 4, grow");
            warning.add(newJava2, "cell 0 5, grow");
            warning.add(java21button, "cell 1 5, grow");

            warning.add(new JSeparator(JSeparator.HORIZONTAL), "cell 0 6 2 1, grow");
            
            warning.add(confirm, "cell 0 7, grow");
            warning.add(quit, "cell 1 7, grow");
        } else {
            warning.add(newJava, "cell 0 1, grow");
            warning.add(newJava2, "cell 0 2, grow");
            warning.add(java21button, "cell 1 2, grow");

            warning.add(new JSeparator(JSeparator.HORIZONTAL), "cell 0 3 2 1, grow");
            warning.add(confirm, "cell 0 4, grow");
            warning.add(quit, "cell 1 4, grow");
        }
        
        quit.addActionListener(actionEvent -> ExitWrapper.exit(0));
        
        confirm.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirm)) {
                warning.setVisible(false);
                synchronized (Relauncher.o) {
                    Relauncher.o.notify();
                }
            }
        });
        
        warning.validate();
        warning.pack();
        GUIUtils.setCentral(warning);
        warning.setVisible(true);
        warning.setResizable(false);
        
        synchronized (Relauncher.o) {
            try {
                Relauncher.o.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public synchronized static void addProgress() {
        mainProgressbar.setValue(mainProgressbar.getValue() + 1);
    }
    
    private static class LinkButton extends JButton{
        public LinkButton(String link) {
            super();
            setText("<HTML><FONT color=\"#000099\"><U>link</U></FONT></HTML>");
            addActionListener(actionEvent -> {
                if (actionEvent.getSource().equals(this)) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(new URI(link));
                        } catch (IOException | URISyntaxException e) {
                            Relauncher.LOGGER.error("Failed to open URL {}", link);
                        }
                    } else {
                        Runtime runtime = Runtime.getRuntime();
                        try {
                            runtime.exec("xdg-open " + link);
                        } catch (IOException e) {
                            Relauncher.LOGGER.error("Failed to open URL {}", link);
                        }
                    }
                }
            });
        }
    }

}
