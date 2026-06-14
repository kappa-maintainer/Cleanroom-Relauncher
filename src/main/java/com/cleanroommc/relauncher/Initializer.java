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
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.swing.Timer;

public class Initializer {
    private static final String valid = "✔";
    private static final String invalid = "❌";
    private static volatile boolean verified = false;
    private static final Set<String> activeDownloads = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final AtomicInteger cycleIdx = new AtomicInteger();
    private static Timer cycleTimer;
    private static final JLabel mainStatusLabel = new JLabel();
    private static final JProgressBar mainProgressbar = new JProgressBar();
    private static JButton launchButton;
    private static final JFrame mainFrame = new JFrame();
    private static Consumer<Boolean> setInteractable;
    private static Runnable verifyJVM;
    
    public static void InitJavaAndArg() {
        Config.syncConfig();
        if (JavaDetector.getInstalledJVMs().isEmpty()) {
            checkJavaAndWarn();
        }
        mainFrame.setLayout(new MigLayout("", "[][grow][grow][grow]", "[grow][grow][grow][grow][grow][grow][grow]"));

        JLabel pathLabel = new JLabel(Messages.get("label.java_path"));
        JTextField pathText = new JTextField();
        JLabel jvmStatus = new JLabel(invalid);
        JFileChooser jvmPicker = getJavaFileChooser();
        JButton detectJvmButton = new JButton(Messages.get("button.detect_java"));
        JButton browserButton = new JButton(Messages.get("button.browse_java"));
        JLabel argsLabel = new JLabel(Messages.get("label.java_args"));
        JTextArea args = new JTextArea(1, 0);
        args.setLineWrap(true);
        args.setWrapStyleWord(false);
        JScrollPane argsScroll = new JScrollPane(args);
        argsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        argsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JButton advSetting = new JButton(Messages.get("button.advanced_settings"));

        setInteractable = value -> {
            launchButton.setEnabled(value);
            pathText.setEnabled(value);
            detectJvmButton.setEnabled(value);
            browserButton.setEnabled(value);
            args.setEnabled(value);
            argsScroll.setEnabled(value);
            advSetting.setEnabled(value);
        };

        launchButton = new JButton(Messages.get("button.launch"));
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
        mainFrame.add(argsScroll, "cell 1 3 3 1, grow");
        argsScroll.setMinimumSize(new Dimension(300, 10));
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

        mainFrame.setTitle(Messages.get("window.title"));
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                ExitWrapper.exit(0);
            }
        });
        
        pathLabel.setHorizontalAlignment(JLabel.CENTER);
        pathText.setToolTipText(Messages.get("tooltip.java_path"));

        detectJvmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(detectJvmButton)) {
                showDetectorDialog(pathText);
            }
        });
        
        pathText.setText(Config.javaPath);
        args.setText(Config.jvmArgs);
        args.setToolTipText(args.getText());
        argsScroll.setToolTipText(args.getText());
        
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
                args.setToolTipText(args.getText());
                argsScroll.setToolTipText(args.getText());
            }
        });

        argsLabel.setHorizontalAlignment(JLabel.CENTER);

        mainProgressbar.setIndeterminate(true);
        mainProgressbar.addChangeListener(changeEvent -> updateStatusLabel());
        mainStatusLabel.setHorizontalAlignment(JTextField.CENTER);
        mainStatusLabel.setText(Messages.get("status.idle"));

        cycleTimer = new Timer(1500, e -> {
            String[] files = activeDownloads.toArray(new String[0]);
            if (files.length > 0) {
                int idx = cycleIdx.getAndUpdate(i -> (i + 1) % files.length);
                SwingUtilities.invokeLater(() -> mainStatusLabel.setText(Messages.get("status.downloading", mainProgressbar.getValue(), mainProgressbar.getMaximum(), files[idx])));
            }
        });
        cycleTimer.start();

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
                return Messages.get("filter.java_executable");
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
        JLabel info = new JLabel(Messages.get("status.idle"));
        info.setHorizontalAlignment(JLabel.CENTER);
        JButton confirm = new JButton(Messages.get("button.confirm"));
        JButton cancel = new JButton(Messages.get("button.cancel"));
        
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
        info.setText(Messages.get("status.scanning"));
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
            info.setText(Messages.get("status.scan_complete"));
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
        JLabel libraryPathLabel = new JLabel(Messages.get("label.library_path"));
        libraryPathLabel.setHorizontalAlignment(JLabel.CENTER);
        JTextField libraryPathText = new JTextField();
        libraryPathText.setText(Config.libraryPath);
        JButton cancel = new JButton(Messages.get("button.cancel"));
        JButton confirm = new JButton(Messages.get("button.confirm"));
        JFileChooser libraryPicker = getLibraryPathChooser();
        JCheckBox groupNameInPathCheckbox = new JCheckBox(Messages.get("checkbox.place_libs_in_group"));
        groupNameInPathCheckbox.setSelected(Config.respectLibraryStructure);
        JButton libraryBrowserButton = new JButton(Messages.get("button.browser"));
        JLabel proxyLabel = new JLabel(Messages.get("label.proxy_host"));
        proxyLabel.setHorizontalAlignment(JLabel.CENTER);
        JLabel proxyPortLabel = new JLabel(Messages.get("label.proxy_port"));
        proxyPortLabel.setHorizontalAlignment(JLabel.CENTER);
        JTextField proxyAddrTextField = new JTextField();
        proxyAddrTextField.setText(Config.proxyAddr);
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(Config.proxyPort, 0, 65535, 1));
        JCheckBox useLocalCheckbox = new JCheckBox(Messages.get("checkbox.use_local_pack"));
        useLocalCheckbox.setSelected(Config.useLocalPack);
        JCheckBox chineseModeCheckbox = new JCheckBox(Messages.get("checkbox.chinese_mirror"));
        chineseModeCheckbox.setSelected(Config.chineseMode);
        JLabel maxRetryLabel = new JLabel(Messages.get("label.max_retry"));
        maxRetryLabel.setHorizontalAlignment(JLabel.CENTER);
        JSpinner maxRetrySpinner = new JSpinner(new SpinnerNumberModel(Config.maxRetry, 1, 65535, 1));
        JLabel maxSessionLabel = new JLabel(Messages.get("label.max_sessions"));
        maxSessionLabel.setHorizontalAlignment(JLabel.CENTER);
        JSpinner maxSessionSpinner = new JSpinner(new SpinnerNumberModel(Config.maxDownloadSession, 1, 65535, 1));

        libraryPathText.setToolTipText(Messages.get("tooltip.library_path"));

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
        
        groupNameInPathCheckbox.setToolTipText(Messages.get("tooltip.place_libs_in_group"));
        
        chineseModeCheckbox.setToolTipText(Messages.get("tooltip.chinese_mirror"));

        
        libraryBrowserButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(libraryBrowserButton)) {
                GUIUtils.setCentral(libraryPicker);
                int r = libraryPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    libraryPathText.setText(libraryPicker.getSelectedFile().getAbsolutePath());
                }
            }
        });
        

        useLocalCheckbox.setToolTipText(Messages.get("tooltip.use_local_pack"));

        proxyAddrTextField.setToolTipText(Messages.get("tooltip.proxy_addr"));
        
        portSpinner.setToolTipText(Messages.get("tooltip.proxy_port"));
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
        JLabel oldJava = new JLabel(Messages.get("warning.outdated_java"));
        oldJava.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(oldJava);
        JLabel oldJava2 = new JLabel(Messages.get("warning.download_java8"));
        oldJava2.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(oldJava2);
        LinkButton java8button = new LinkButton("https://adoptium.net/temurin/releases?version=8&os=any&arch=any");
        GUIUtils.enlargeFont(java8button);
        JLabel newJava = new JLabel(Messages.get("warning.need_java21"));
        newJava.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(newJava);
        JLabel newJava2 = new JLabel(Messages.get("warning.download_java21"));
        newJava2.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(newJava2);
        LinkButton java21button = new LinkButton("https://adoptium.net/temurin/releases?version=21&os=any&arch=any");
        GUIUtils.enlargeFont(java21button);
        JButton confirm = new JButton(Messages.get("button.continue"));
        GUIUtils.enlargeFont(confirm, Font.BOLD, 20);
        JButton quit = new JButton(Messages.get("button.quit"));
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
    
    public synchronized static void addProgress(String fileName) {
        activeDownloads.remove(fileName);
        mainProgressbar.setValue(mainProgressbar.getValue() + 1);
    }

    public static void setCurrentFile(String name) {
        if (name == null) {
            activeDownloads.clear();
        } else {
            activeDownloads.add(name);
        }
        updateStatusLabel();
    }

    private static void updateStatusLabel() {
        String[] files = activeDownloads.toArray(new String[0]);
        if (files.length > 0) {
            mainStatusLabel.setText(Messages.get("status.downloading", mainProgressbar.getValue(), mainProgressbar.getMaximum(), files[cycleIdx.get() % files.length]));
        } else {
            mainStatusLabel.setText(Messages.get("status.downloading_count", mainProgressbar.getValue(), mainProgressbar.getMaximum()));
        }
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
