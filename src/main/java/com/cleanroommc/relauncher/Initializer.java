package com.cleanroommc.relauncher;

import com.google.common.collect.Lists;
import net.minecraftforge.fml.ExitWrapper;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;
import java.util.function.Consumer;

public class Initializer {
    private static final String valid = "Valid JVM ✔";
    private static final String invalid = "Invalid JVM X";
    private static final String toBeVerify = "Verify JVM";
    private static volatile boolean verified = false;
    private static int argsTextHeight;
    private static final JLabel mainStatusLabel = new JLabel();
    private static final JProgressBar mainProgressbar = new JProgressBar();
    private static JButton confirmButton;
    private static final JFrame mainFrame = new JFrame();
    private static Consumer<Boolean> setInteractable;
    private static final String[] mirrors = new String[]{
            "",
            "https://maven.aliyun.com/repository/public",
            "http://mirrors.163.com/maven/repository/maven-public",
            "https://repo.huaweicloud.com/repository/maven",
            "http://mirrors.cloud.tencent.com/nexus/repository/maven-public",
            
    };
    
    public static void InitJavaAndArg() {
        Config.syncConfig();
        mainFrame.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel pathLabel = new JLabel("Java Path*");
        JTextField pathText = new JTextField();
        JFileChooser jvmPicker = getJavaFileChooser();
        JButton verifyButton = new JButton(toBeVerify);
        JButton detectJvmButton = new JButton("Detect Installed JVMs");
        JButton browserButton = new JButton("Browser...");
        JLabel argsLabel = new JLabel("Java Args");
        JTextArea args = new JTextArea("-Xmx4g -Xms4g");
        JButton advSetting = new JButton("Advanced Settings");

        setInteractable = value -> {
            confirmButton.setEnabled(value);
            pathText.setEnabled(value);
            verifyButton.setEnabled(value);
            detectJvmButton.setEnabled(value);
            browserButton.setEnabled(value);
            args.setEnabled(value);
        };

        confirmButton = new JButton("Confirm");
        confirmButton.setEnabled(false);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        mainFrame.add(pathLabel, c);
        c.gridx = 1;
        c.gridwidth = 3;
        mainFrame.add(pathText, c);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        mainFrame.add(verifyButton, c);
        c.gridx = 1;
        c.gridwidth = 2;
        mainFrame.add(detectJvmButton, c);
        c.gridwidth = 1;
        c.gridx = 3;
        mainFrame.add(browserButton, c);
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        mainFrame.add(argsLabel, c);
        c.gridx = 1;
        c.gridwidth = 3;
        mainFrame.add(args, c);
        c.gridy = 4;
        c.gridx = 0;
        c.gridwidth = 4;
        mainFrame.add(advSetting,c);
        c.gridy = 5;
        mainFrame.add(mainStatusLabel, c);
        c.gridy = 6;
        mainFrame.add(mainProgressbar, c);
        c.gridy = 7;
        mainFrame.add(confirmButton, c);

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
                showDetectorDialog(pathText, verifyButton);
            }
        });
        
        pathText.setText(Config.javaPath);
        args.setText(Config.jvmArgs);
        
        advSetting.addActionListener(actionEvent -> showAdvancedSettingDialog());

        confirmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirmButton)) {
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
                        Config.configFile.delete();
                        mainStatusLabel.setText(t.getMessage());
                        setInteractable.accept(true);
                        mainProgressbar.setIndeterminate(true);
                    }
                } else {
                    Relauncher.LOGGER.warn("Invalid Java");
                    confirmButton.setEnabled(false);
                    verifyButton.doClick();
                }
            }
        });


        verifyButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(verifyButton)) {
                if (isJavaNewerThan21(pathText.getText())) {
                    verified = true;
                    verifyButton.setText(valid);
                    verifyButton.setForeground(Color.GREEN);

                    confirmButton.setEnabled(true);
                } else {
                    verified = false;
                    verifyButton.setText(invalid);
                    verifyButton.setForeground(Color.RED);

                    confirmButton.setEnabled(false);
                }
            }
        });

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
                verifyButton.doClick();
            }
        });
        
        browserButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(browserButton)) {
                GUIUtils.setCentral(jvmPicker);
                int r = jvmPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    pathText.setText(jvmPicker.getSelectedFile().getAbsolutePath());
                    verifyButton.doClick();
                }
            }
        });

        args.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                adjustSize();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                adjustSize();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                adjustSize();
            }

            private void adjustSize() {
                if (args.getHeight() != argsTextHeight) {
                    argsTextHeight = args.getHeight();
                    mainFrame.pack();
                }
            }
        });

        argsLabel.setHorizontalAlignment(JLabel.CENTER);

        mainProgressbar.setIndeterminate(true);
        mainProgressbar.addChangeListener(changeEvent -> mainStatusLabel.setText(String.format("%d/%d", mainProgressbar.getValue(), mainProgressbar.getMaximum())));
        mainStatusLabel.setHorizontalAlignment(JTextField.CENTER);
        mainStatusLabel.setText("Idle");

        Relauncher.LOGGER.info("Launching GUI");
        mainFrame.validate();
        //mainFrame.setSize(GUIUtils.scaledWidth / 2, GUIUtils.scaledHeight / 4);
        mainFrame.pack();
        argsTextHeight = args.getHeight();
        mainFrame.setResizable(false);
        GUIUtils.setCentral(mainFrame);
        mainFrame.setVisible(true);
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
    
    public static JButton getConfirmButton() {
        return confirmButton;
    }

    public static void setGUIInteractable(boolean enable) {
        setInteractable.accept(enable);
    }

    public static void hideWindow() {
        mainFrame.setVisible(false);
    }

    private static void showDetectorDialog(JTextField pathField, JButton verify) {
        JDialog detector = new JDialog();
        detector.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        detector.setLayout(new GridBagLayout());
        DefaultListModel<JVMInfo> model = new DefaultListModel<>();
        JList<JVMInfo> list = new JList<>(model);
        JLabel info = new JLabel("Idle");
        JButton confirm = new JButton("Confirm");
        JButton cancel = new JButton("Cancel");

        GridBagConstraints c =new GridBagConstraints();
        c.fill = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.gridheight = 4;
        c.ipady = 100;
        c.ipadx = 100;
        detector.add(list, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = 0;
        c.ipadx = 0;
        c.gridy = 4;
        c.gridheight = 1;
        detector.add(info, c);
        c.gridy = 5;
        c.gridwidth = 2;
        detector.add(cancel, c);
        c.gridx = 2;
        c.gridwidth = 1;
        detector.add(confirm, c);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.addListSelectionListener(listSelectionEvent -> {
            String path = list.getSelectedValue().getFile().getAbsolutePath();
            info.setText(path);
            info.setToolTipText(path);
        });

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
                verify.doClick();
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
        JDialog advSetting = new JDialog();
        advSetting.setLayout(new GridBagLayout());
        JLabel libraryPathLabel = new JLabel("Library Path");
        JTextField libraryPathText = new JTextField();
        libraryPathText.setText(Config.libraryPath);
        JButton confirm = new JButton("Confirm");
        JFileChooser libraryPicker = getLibraryPathChooser();
        JCheckBox groupNameInPathCheckbox = new JCheckBox("Place Libraries in Group Name");
        groupNameInPathCheckbox.setSelected(Config.respectLibraryStructure);
        JButton libraryBrowserButton = new JButton("Browser...");
        JLabel proxyLabel = new JLabel("Proxy");
        JTextField proxyAddrTextField = new JTextField();
        proxyAddrTextField.setText(Config.proxyAddr);
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(Config.proxyPort, 0, 65535, 1));
        JCheckBox useLocalCheckbox = new JCheckBox("Use Local Pack");
        useLocalCheckbox.setSelected(Config.useLocalPack);
        JLabel mirrorLabel = new JLabel("Maven Mirror");
        JComboBox<String> mirrorList = new JComboBox<>(new Vector<>(Lists.newArrayList(mirrors)));
        mirrorList.setSelectedItem(Config.replaceMavenURL);
        JLabel maxRetryLabel = new JLabel("Max connection attempts");
        JSpinner maxRetrySpinner = new JSpinner(new SpinnerNumberModel(Config.maxRetry, 1, 65535, 1));
        JLabel maxSessionLabel = new JLabel("Max concurrent download sessions");
        JSpinner maxSessionSpinner = new JSpinner(new SpinnerNumberModel(Config.maxDownloadSession, 1, 65535, 1));

        libraryPathText.setToolTipText("Path to place the libraries, leave it empty to use default location");

        GridBagConstraints c =new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 1;
        c.gridwidth = 1;
        advSetting.add(libraryPathLabel, c);
        c.gridx = 1;
        c.gridwidth = 2;
        advSetting.add(libraryPathText, c);
        c.gridx = 3;
        c.gridwidth = 1;
        advSetting.add(libraryBrowserButton, c);
        c.gridy = 1;
        c.gridx = 0;
        c.gridwidth = 1;
        advSetting.add(proxyLabel, c);
        c.gridwidth = 2;
        c.gridx = 1;
        advSetting.add(proxyAddrTextField, c);
        c.gridwidth = 1;
        c.gridx = 3;
        advSetting.add(portSpinner, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        advSetting.add(mirrorLabel, c);
        c.gridx = 1;
        c.gridwidth = 3;
        advSetting.add(mirrorList, c);
        c.gridy = 3;
        c.gridwidth = 3;
        c.gridx = 0;
        advSetting.add(maxRetryLabel, c);
        c.gridx = 3;
        c.gridwidth = 1;
        advSetting.add(maxRetrySpinner, c);
        c.gridy = 4;
        c.gridwidth = 3;
        c.gridx = 0;
        advSetting.add(maxSessionLabel, c);
        c.gridx = 3;
        c.gridwidth = 1;
        advSetting.add(maxSessionSpinner, c);
        c.gridy = 5;
        c.gridx = 0;
        c.gridwidth = 1;
        advSetting.add(useLocalCheckbox, c);
        c.gridx = 1;
        advSetting.add(groupNameInPathCheckbox, c);
        c.gridx = 0;
        c.gridwidth = 4;
        c.gridy = 6;
        advSetting.add(confirm, c);
        
        groupNameInPathCheckbox.setToolTipText("Place libraries in their corresponding groups. Useful when you want to reuse libraries with the launcher.");
        
        mirrorList.setToolTipText("The mirror URL used to replace central maven. Currently only Chinese ISP may need this.");

        
        libraryBrowserButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(libraryBrowserButton)) {
                GUIUtils.setCentral(libraryPicker);
                int r = libraryPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    libraryPathText.setText(libraryPicker.getSelectedFile().getAbsolutePath());
                }
            }
        });
        //libraryPathLabel.setHorizontalAlignment(JLabel.CENTER);

        useLocalCheckbox.setToolTipText("Will use first Cleanroom-MMC-instance-*.zip in relauncher dir");

        proxyAddrTextField.setToolTipText("Proxy Address, leave it empty means no proxy");
        
        portSpinner.setToolTipText("Proxy Port, leave it 0 means no proxy");
        portSpinner.setModel(new SpinnerNumberModel());
        
        mirrorList.setEditable(true);
        mirrorList.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
        
        confirm.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirm)) {
                advSetting.setVisible(false);
                setGUIInteractable(true);
                Config.replaceMavenURL = (String) mirrorList.getSelectedItem();
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
        advSetting.setResizable(false);
        setGUIInteractable(false);
        advSetting.setVisible(true);
    }
    
    public synchronized static void addProgress() {
        mainProgressbar.setValue(mainProgressbar.getValue() + 1);
    }

}
