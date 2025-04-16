package com.cleanroommc.relauncher;

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
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class Initializer {
    private static final String valid = "Valid JVM ✔";
    private static final String invalid = "Invalid JVM X";
    private static final String toBeVerify = "Verify JVM";
    private static volatile boolean verified = false;
    private static Color verifyButtonDefaultForeground;
    private static int argsTextHeight;
    private static final JLabel mainStatusLabel = new JLabel();
    private static final JProgressBar mainProgressbar = new JProgressBar();
    private static final JLabel subStatusLabel = new JLabel();
    private static final JProgressBar subProgressbar = new JProgressBar(0, 100);
    private static JButton confirmButton;
    private static final JFrame mainFrame = new JFrame();
    private static Consumer<Boolean> setInteractable;
    public static void InitJavaAndArg() {
        mainFrame.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JLabel pathLabel = new JLabel("↓ Java Path ↓");
        JTextField pathText = new JTextField();
        JFileChooser jvmPicker = getJavaFileChooser();
        JButton verifyButton = new JButton(toBeVerify);
        JButton detectJvmButton = new JButton("Detect Installed JVMs");
        JButton browserButton = new JButton("Browser...");
        JLabel libraryPathLabel = new JLabel("↓ Library Path ↓");
        JTextField libraryPathText = new JTextField();
        JFileChooser libraryPicker = getLibraryPathChooser();
        JCheckBox groupNameInPathCheckbox = new JCheckBox("Place Libraries in Group Name");
        JButton libraryBrowserButton = new JButton("Browser...");
        JLabel argsLabel = new JLabel("↓ Java Arguments ↓");
        JTextArea args = new JTextArea("-Xmx4g -Xms4g");
        JCheckBox proxyCheckbox = new JCheckBox("Use Proxy");
        JTextField proxyAddrTextField = new JTextField();
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
        JCheckBox useLocalCheckbox = new JCheckBox("Use Local Pack");
        JCheckBox multipartCheckbox = new JCheckBox("Enable Multipart");

        setInteractable = value -> {
            confirmButton.setEnabled(value);
            pathText.setEnabled(value);
            verifyButton.setEnabled(value);
            detectJvmButton.setEnabled(value);
            browserButton.setEnabled(value);
            libraryPathText.setEnabled(value);
            libraryBrowserButton.setEnabled(value);
            groupNameInPathCheckbox.setEnabled(value);
            args.setEnabled(value);
            proxyCheckbox.setEnabled(value);
            if (!value || proxyCheckbox.isSelected()) {
                proxyAddrTextField.setEnabled(value);
                portSpinner.setEnabled(value);
            }
            useLocalCheckbox.setEnabled(value);
            multipartCheckbox.setEnabled(value);
        };

        confirmButton = new JButton("Confirm");
        confirmButton.setActionCommand("confirm");
        confirmButton.setEnabled(false);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.ipady = 10;
        c.gridwidth = 3;
        mainFrame.add(pathLabel, c);
        c.ipady = 0;
        c.gridy = 1;
        mainFrame.add(pathText, c);
        c.gridy = 2;
        c.gridwidth = 1;
        mainFrame.add(verifyButton, c);
        c.gridx = 1;
        mainFrame.add(detectJvmButton, c);
        c.gridx = 2;
        mainFrame.add(browserButton, c);
        c.gridwidth = 3;
        c.gridx = 0;
        c.gridy = 3;
        mainFrame.add(libraryPathLabel, c);
        c.gridy = 4;
        mainFrame.add(libraryPathText, c);
        c.gridy = 5;
        c.gridwidth = 2;
        mainFrame.add(groupNameInPathCheckbox, c);
        c.gridx = 2;
        c.gridwidth = 1;
        mainFrame.add(libraryBrowserButton, c);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 3;
        mainFrame.add(argsLabel, c);
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 3;
        c.gridheight = 2;
        mainFrame.add(args, c);
        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.CENTER;
        mainFrame.add(useLocalCheckbox, c);
        c.gridx = 1;
        mainFrame.add(multipartCheckbox, c);
        c.gridx = 2;
        mainFrame.add(proxyCheckbox, c);
        c.gridx = 0;
        c.gridy = 10;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        mainFrame.add(proxyAddrTextField, c);
        c.gridx = 2;
        c.gridwidth = 1;
        mainFrame.add(portSpinner, c);
        c.gridx = 0;
        c.gridy = 11;
        c.gridwidth = 3;
        c.ipady = 10;
        mainFrame.add(mainStatusLabel, c);
        c.gridy = 12;
        mainFrame.add(mainProgressbar, c);
        c.gridy = 13;
        mainFrame.add(subStatusLabel, c);
        c.gridy = 14;
        mainFrame.add(subProgressbar, c);
        c.gridy = 15;
        c.ipady = 0;
        mainFrame.add(confirmButton, c);

        mainFrame.setTitle("Relauncher Initialization Settings");
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                ExitWrapper.exit(0);
            }
        });
        GUIUtils.enlargeFont(pathLabel);
        pathLabel.setHorizontalAlignment(JLabel.CENTER);
        pathText.setToolTipText("Input your Java 21+ executable here");

        GUIUtils.enlargeFont(libraryPathLabel);
        libraryPathLabel.setHorizontalAlignment(JLabel.CENTER);

        detectJvmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(detectJvmButton)) {
                JDialog detect = getDetectorDialog(pathText, verifyButton);
                detect.setVisible(true);
            }
        });

        confirmButton.addActionListener(actionEvent -> {
            if (actionEvent.getActionCommand().equals("confirm")) {
                if (verified || isJavaNewerThan21(pathText.getText())) {
                    Relauncher.LOGGER.info("Java valid and saved");
                    setInteractable.accept(false);
                    Config.javaPathDefault = pathText.getText();
                    Config.jvmArgsDefault = args.getText();
                    Config.proxyAddrDefault = proxyAddrTextField.getText().replace('\n', ' ');
                    Config.proxyPortDefault = (int) portSpinner.getValue();
                    Config.useLocalPackDefault = useLocalCheckbox.isSelected();
                    Config.enableMultipartDownloadDefault = multipartCheckbox.isSelected();
                    Config.respectLibraryStructureDefault = groupNameInPathCheckbox.isSelected();
                    Config.libraryPathDefault = libraryPathText.getText();

                    Config.syncOnly();

                    mainProgressbar.setIndeterminate(false);
                    subProgressbar.setIndeterminate(false);

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
                        subProgressbar.setIndeterminate(true);
                    }
                } else {
                    Relauncher.LOGGER.warn("Invalid Java");
                    confirmButton.setEnabled(false);
                    verifyButton.doClick();
                }
            }
        });


        verifyButtonDefaultForeground = verifyButton.getForeground();
        verifyButton.setActionCommand("verify");
        verifyButton.addActionListener(actionEvent -> {
            if (actionEvent.getActionCommand().equals("verify")) {
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
                verified = false;
                verifyButton.setText(toBeVerify);
                verifyButton.setForeground(verifyButtonDefaultForeground);
                confirmButton.setEnabled(false);
            }
        });

        browserButton.setActionCommand("browser");
        browserButton.addActionListener(actionEvent -> {
            if (actionEvent.getActionCommand().equals("browser")) {
                GUIUtils.setCentral(jvmPicker);
                int r = jvmPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    pathText.setText(jvmPicker.getSelectedFile().getAbsolutePath());
                    verifyButton.doClick();
                }
            }
        });

        libraryBrowserButton.setActionCommand("browser_library");
        libraryBrowserButton.addActionListener(actionEvent -> {
            if (actionEvent.getActionCommand().equals("browser_library")) {
                GUIUtils.setCentral(libraryPicker);
                int r = libraryPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    libraryPathText.setText(libraryPicker.getSelectedFile().getAbsolutePath());
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
        GUIUtils.enlargeFont(argsLabel);
        useLocalCheckbox.setToolTipText("Will use first Cleanroom-MMC-instance-*.zip in relauncher dir");
        multipartCheckbox.setToolTipText("Will enable multipart download, use when your network is slow");
        multipartCheckbox.setSelected(true);
        GUIUtils.enlargeFont(confirmButton);

        proxyCheckbox.setToolTipText("Require Proxy Address & Port");
        proxyCheckbox.addItemListener(itemEvent -> {
            boolean selected = itemEvent.getStateChange() == ItemEvent.SELECTED;
            portSpinner.setEnabled(selected);
            proxyAddrTextField.setEnabled(selected);
        });

        proxyAddrTextField.setToolTipText("Proxy Address");
        proxyAddrTextField.setEnabled(false);

        portSpinner.setEnabled(false);
        portSpinner.setToolTipText("Proxy Port");
        portSpinner.setModel(new SpinnerNumberModel());

        mainProgressbar.setIndeterminate(true);
        mainStatusLabel.setHorizontalAlignment(JTextField.CENTER);
        mainStatusLabel.setText("Idle");
        subProgressbar.setIndeterminate(true);
        subProgressbar.setMaximum(100);
        subProgressbar.setMinimum(0);
        subStatusLabel.setHorizontalAlignment(JTextField.CENTER);
        subStatusLabel.setText("Idle");

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

    public static JLabel getSubStatusLabel() {
        return subStatusLabel;
    }

    public static JProgressBar getSubProgressbar() {
        return subProgressbar;
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

    private static JDialog getDetectorDialog(JTextField pathField, JButton verify) {
        JDialog detector = new JDialog();
        detector.setLayout(new GridBagLayout());
        DefaultListModel<JVMInfo> model = new DefaultListModel<>();
        for (JVMInfo i : JavaDetector.getInstalledJVMs()) {
            model.addElement(i);
        }
        JList<JVMInfo> list = new JList<>(model);
        JButton confirm = new JButton("Confirm");
        JButton cancel = new JButton("Cancel");
        GridBagConstraints c =new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.gridheight = 4;
        detector.add(list, c);
        c.gridy = 4;
        c.gridheight = 1;
        c.gridwidth = 1;
        detector.add(cancel, c);
        c.gridx = 1;
        detector.add(confirm, c);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);

        detector.pack();
        detector.setResizable(false);
        detector.setAlwaysOnTop(true);
        GUIUtils.setCentral(detector);


        cancel.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(cancel)) {
                detector.setVisible(false);
            }
        });

        confirm.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirm)) {
                detector.setVisible(false);
                pathField.setText(list.getSelectedValue().getFile().getAbsolutePath());
                verify.doClick();
            }
        });

        return detector;

    }

}
