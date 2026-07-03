package com.cleanroommc.relauncher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.miginfocom.swing.MigLayout;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.ExitWrapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;


import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MMCInstaller {

    private static final JLabel mainStatusLabel = new JLabel();
    private static JLabel latestVersionLabel;
    private static JLabel localVersionLabel;
    private static String latestVersionFetched = null;
    private static boolean latestVersionFetchFailed = false;
    private static boolean versionFetchInProgress = false;
    private static JButton confirmButton;
    private static final JFrame mainFrame = new JFrame();
    private static Consumer<Boolean> setInteractable;
    public static void showGUI() {
        mainFrame.setLayout(new MigLayout("", "[][grow][grow][]", "[][][][][][][][][][][]"));

        JLabel host = new JLabel(Messages.get("label.proxy_host"));
        JTextField proxyAddrTextField = new JTextField();
        proxyAddrTextField.setText(Config.proxyAddr);

        JLabel port = new JLabel(Messages.get("label.proxy_port"));
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
        portSpinner.setValue(Config.proxyPort);
        JCheckBox useLocalCheckbox = new JCheckBox(Messages.get("checkbox.use_local_pack"));

        JTextField mmcLocalPackPathField = new JTextField();
        mmcLocalPackPathField.setText(Config.localPackPath);
        mmcLocalPackPathField.setEnabled(Config.useLocalPack);
        mmcLocalPackPathField.setToolTipText(Messages.get("tooltip.local_pack_path"));
        mmcLocalPackPathField.setMinimumSize(new Dimension(200, 10));
        GUIUtils.enlargeFont(mmcLocalPackPathField);

        JButton mmcLocalPackBrowserButton = new JButton(Messages.get("button.browser"));
        mmcLocalPackBrowserButton.setEnabled(Config.useLocalPack);
        GUIUtils.enlargeFont(mmcLocalPackBrowserButton);

        JFileChooser mmcPackPicker = new JFileChooser(SystemUtils.getUserDir());
        mmcPackPicker.setMultiSelectionEnabled(false);
        mmcPackPicker.setAcceptAllFileFilterUsed(false);
        mmcPackPicker.setFileHidingEnabled(false);
        mmcPackPicker.setFileSelectionMode(JFileChooser.FILES_ONLY);
        mmcPackPicker.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(".zip") || file.isDirectory();
            }
            @Override
            public String getDescription() {
                return Messages.get("filter.mmc_zip");
            }
        });

        JCheckBox mirrorCheckBox = new JCheckBox(Messages.get("checkbox.chinese_mirror"));
        mirrorCheckBox.setSelected(Config.useMirror);
        mirrorCheckBox.setToolTipText(Messages.get("tooltip.chinese_mirror"));
        GUIUtils.enlargeFont(mirrorCheckBox);

        JLabel javaPathLabel = new JLabel(Messages.get("label.java_path"));
        javaPathLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(javaPathLabel);

        JTextField javaPathField = new JTextField();
        javaPathField.setToolTipText(Messages.get("tooltip.java_path"));
        javaPathField.setMinimumSize(new Dimension(300, 10));
        GUIUtils.enlargeFont(javaPathField);

        JLabel jvmStatus = new JLabel("\u2718");
        jvmStatus.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(jvmStatus);

        JButton detectJvmButton = new JButton(Messages.get("button.detect_java"));
        GUIUtils.enlargeFont(detectJvmButton);
        JButton browseJvmButton = new JButton(Messages.get("button.browse_java"));
        GUIUtils.enlargeFont(browseJvmButton);

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

        JLabel warningLabel = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        warningLabel.setText(Messages.get("warning.need_java25"));
        warningLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(warningLabel);

        JSeparator jSeparator1 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator2 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator3 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator4 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator5 = new JSeparator(JSeparator.HORIZONTAL);

        Runnable verifyJvm = () -> {
            if (Initializer.isJavaNewerThan21(javaPathField.getText())) {
                jvmStatus.setText("\u2714");
                jvmStatus.setForeground(Color.GREEN);
                confirmButton.setEnabled(true);
            } else {
                jvmStatus.setText("\u2718");
                jvmStatus.setForeground(Color.RED);
                confirmButton.setEnabled(false);
            }
        };

        setInteractable = value -> {
            confirmButton.setEnabled(value);
            if (!value || !useLocalCheckbox.isSelected()) {
                proxyAddrTextField.setEnabled(value);
                portSpinner.setEnabled(value);
            }
            useLocalCheckbox.setEnabled(value);
            mirrorCheckBox.setEnabled(value);
            mmcLocalPackPathField.setEnabled(value && Config.useLocalPack);
            mmcLocalPackBrowserButton.setEnabled(value && Config.useLocalPack);
            javaPathField.setEnabled(value);
            detectJvmButton.setEnabled(value);
            browseJvmButton.setEnabled(value);
        };

        confirmButton = new JButton(Messages.get("button.confirm"));
        confirmButton.setEnabled(false);

        List<JVMInfo> jvms = JavaDetector.getInstalledJVMs();
        boolean hasJava25 = false;
        if (!jvms.isEmpty()) {
            javaPathField.setText(jvms.get(0).getFile().getAbsolutePath());
            for (JVMInfo jvm : jvms) {
                if (jvm.getSpecification() >= 25) {
                    hasJava25 = true;
                    break;
                }
            }
        }
        verifyJvm.run();

        int row = 0;
        mainFrame.add(useLocalCheckbox, "cell 0 " + row + " 2 1, grow, align center");
        GUIUtils.enlargeFont(useLocalCheckbox);
        useLocalCheckbox.setHorizontalAlignment(JCheckBox.CENTER);
        mainFrame.add(mirrorCheckBox, "cell 2 " + row + " 2 1, grow, align center");
        mirrorCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
        row++;

        mainFrame.add(mmcLocalPackPathField, "cell 0 " + row + " 3 1, grow");
        mainFrame.add(mmcLocalPackBrowserButton, "cell 3 " + row + ", grow");
        row++;

        mainFrame.add(jSeparator1, "cell 0 " + row + " 4 1, grow");
        row++;

        mainFrame.add(javaPathLabel, "cell 0 " + row + ", grow");
        mainFrame.add(javaPathField, "cell 1 " + row + " 2 1, grow");
        mainFrame.add(jvmStatus, "cell 3 " + row + ", grow");
        row++;

        mainFrame.add(detectJvmButton, "cell 0 " + row + " 2 1, grow");
        mainFrame.add(browseJvmButton, "cell 2 " + row + " 2 1, grow");
        row++;

        if (!hasJava25) {
            mainFrame.add(warningLabel, "cell 0 " + row + " 4 1, grow");
            row++;
        }

        mainFrame.add(jSeparator2, "cell 0 " + row + " 4 1, grow");
        row++;

        JPanel proxyPanel = new JPanel(new MigLayout("", "[][grow][][60px]", "[]"));
        proxyPanel.add(host);
        GUIUtils.enlargeFont(host);
        proxyPanel.add(proxyAddrTextField, "grow");
        GUIUtils.enlargeFont(proxyAddrTextField);
        proxyAddrTextField.setMinimumSize(new Dimension(150, 10));
        proxyPanel.add(port);
        GUIUtils.enlargeFont(port);
        proxyPanel.add(portSpinner);
        GUIUtils.enlargeFont(portSpinner);
        portSpinner.setMinimumSize(new Dimension(80, 10));
        mainFrame.add(proxyPanel, "cell 0 " + row + " 4 1, grow");
        row++;

        mainFrame.add(jSeparator4, "cell 0 " + row + " 4 1, grow");
        row++;

        mainFrame.add(mainStatusLabel, "cell 0 " + row + " 4 1, grow, align center");
        GUIUtils.enlargeFont(mainStatusLabel);
        row++;

        mainFrame.add(jSeparator5, "cell 0 " + row + " 4 1, grow");
        row++;

        latestVersionLabel = new JLabel(Messages.get("label.latest_version") + ": " + Messages.get("status.fetching_version"));
        latestVersionLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(latestVersionLabel);
        localVersionLabel = new JLabel(Messages.get("label.local_version") + ": ?");
        localVersionLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(localVersionLabel);
        mainFrame.add(latestVersionLabel, "cell 0 " + row + " 2 1, grow, align center");
        mainFrame.add(localVersionLabel, "cell 2 " + row + " 2 1, grow, align center");
        row++;

        mainFrame.add(jSeparator3, "cell 0 " + row + " 4 1, grow");
        row++;

        mainFrame.add(confirmButton, "cell 0 " + row + " 4 1, grow");
        GUIUtils.enlargeFont(confirmButton, Font.BOLD, 20);

        mainFrame.setTitle(Messages.get("window.title"));
        mainFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                ExitWrapper.exit(0);
            }
        });


        confirmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirmButton)) {
                setInteractable.accept(false);
                Config.useLocalPack = useLocalCheckbox.isSelected();
                Config.localPackPath = mmcLocalPackPathField.getText();
                Config.proxyAddr = proxyAddrTextField.getText();
                Config.proxyPort = (int) portSpinner.getValue();
                Config.useMirror = mirrorCheckBox.isSelected();
                Config.javaPath = javaPathField.getText();

                try {
                    Thread workingThread = new Thread(() -> {
                        try {
                            mainStatusLabel.setText(Messages.get("status.installing"));
                            MMCPackDownloader.downloadAndExtract();
                            File packDir = new File(Relauncher.workingDir, "mmcpack").getAbsoluteFile();
                            File instance = Launch.minecraftHome.getParentFile();
                            Relauncher.LOGGER.info("Instance directory: {}", instance.getAbsolutePath());
                            File libraries = new File(instance, "libraries");
                            File patches = new File(instance, "patches");
                            libraries.mkdirs();
                            patches.mkdirs();
                            if (Config.useMirror) {
                                SwingUtilities.invokeLater(() -> mainStatusLabel.setText(Messages.get("mmc.status.replacing_urls")));
                                replacePatchUrls(new File(packDir, "patches"));
                            }
                            try {
                                FileUtils.copyDirectory(new File(packDir, "libraries"), libraries);
                                FileUtils.copyDirectory(new File(packDir, "patches"), patches);
                                FileUtils.copyFile(new File(packDir, "mmc-pack.json"), new File(instance, "mmc-pack.json"));
                            } catch (IOException e) {
                                throw new RuntimeException("Failed to copy MMC pack files", e);
                            }
                            Relauncher.LOGGER.info("Install finished");
                            Config.showConfigGUI = false;
                            Config.save();
                            JDialog dialog = finishedDialog();
                            GUIUtils.setCentral(dialog);
                            dialog.setVisible(true);
                        } catch (Throwable e) {
                            Relauncher.LOGGER.error("MMC installation failed: {}", e.getMessage(), e);
                            SwingUtilities.invokeLater(() -> {
                                mainStatusLabel.setText(Messages.get("mmc.status.install_failed"));
                                setInteractable.accept(true);
                            });
                        }
                    }, "MMC installation Working Thread");
                    workingThread.start();
                } catch (Throwable t) {
                    Relauncher.LOGGER.error(t.getMessage());
                    Arrays.stream(t.getStackTrace()).forEach(Relauncher.LOGGER::info);
                    Config.configFile.delete();
                    mainStatusLabel.setText(Messages.get("mmc.status.install_failed"));
                    setInteractable.accept(true);
                }
            }
        });

        javaPathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { verifyJvm.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { verifyJvm.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { verifyJvm.run(); }
        });

        detectJvmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(detectJvmButton)) {
                JvmDetectorDialog.show(mainFrame, selectedPath -> {
                    javaPathField.setText(selectedPath);
                    verifyJvm.run();
                }, () -> setInteractable.accept(false), () -> setInteractable.accept(true));
            }
        });

        browseJvmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(browseJvmButton)) {
                GUIUtils.setCentral(jvmPicker);
                int r = jvmPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    javaPathField.setText(jvmPicker.getSelectedFile().getAbsolutePath());
                    verifyJvm.run();
                }
            }
        });


        useLocalCheckbox.setToolTipText(Messages.get("tooltip.use_local_pack_mmc"));
        useLocalCheckbox.setSelected(Config.useLocalPack);
        proxyAddrTextField.setText(Config.proxyAddr);
        portSpinner.setValue(Config.proxyPort);

        useLocalCheckbox.addItemListener(itemEvent -> {
            boolean selected = itemEvent.getStateChange() == ItemEvent.SELECTED;
            Config.useLocalPack = selected;
            portSpinner.setEnabled(!selected);
            proxyAddrTextField.setEnabled(!selected);
            mmcLocalPackPathField.setEnabled(selected);
            mmcLocalPackBrowserButton.setEnabled(selected);
            updateVersionStatusBar();
        });

        mirrorCheckBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                Config.useMirror = true;
                if (versionFetchInProgress || latestVersionFetchFailed) {
                    fetchLatestVersion();
                }
            } else {
                Config.useMirror = false;
            }
        });

        mmcLocalPackBrowserButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(mmcLocalPackBrowserButton)) {
                GUIUtils.setCentral(mmcPackPicker);
                int r = mmcPackPicker.showOpenDialog(mainFrame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    File selected = mmcPackPicker.getSelectedFile();
                    mmcLocalPackPathField.setText(selected.getAbsolutePath());
                }
            }
        });

        mmcLocalPackPathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            private void onChange() {
                Config.localPackPath = mmcLocalPackPathField.getText();
                updateVersionStatusBar();
            }
        });

        proxyAddrTextField.setToolTipText(Messages.get("tooltip.proxy_addr"));
        proxyAddrTextField.setEnabled(!useLocalCheckbox.isSelected());

        portSpinner.setEnabled(!useLocalCheckbox.isSelected());
        portSpinner.setToolTipText(Messages.get("tooltip.proxy_port"));
        portSpinner.setModel(new SpinnerNumberModel());

        mainStatusLabel.setText(Messages.get("status.idle"));
        mainStatusLabel.setHorizontalAlignment(JLabel.CENTER);
        

        Relauncher.LOGGER.info("Launching GUI");
        mainFrame.validate();
        //mainFrame.setSize(GUIUtils.scaledWidth / 2, GUIUtils.scaledHeight / 4);
        mainFrame.pack();
        GUIUtils.setCentral(mainFrame);
        updateVersionStatusBar();
        fetchLatestVersion();
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

    private static void replacePatchUrls(File patchesDir) {
        File[] jsonFiles = patchesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) return;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (File jsonFile : jsonFiles) {
            try {
                String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
                JsonObject root = new JsonParser().parse(content).getAsJsonObject();
                JsonArray libraries = root.getAsJsonArray("libraries");
                if (libraries != null) {
                    for (JsonElement lib : libraries) {
                        JsonObject libObj = lib.getAsJsonObject();
                        JsonObject downloads = libObj.getAsJsonObject("downloads");
                        if (downloads != null) {
                            replaceUrlsInDownloads(downloads);
                        }
                    }
                }
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(jsonFile), StandardCharsets.UTF_8))) {
                    writer.print(gson.toJson(root));
                }
                Relauncher.LOGGER.info("Replaced URLs in {}", jsonFile.getName());
            } catch (IOException e) {
                Relauncher.LOGGER.error("Failed to replace URLs in {}", jsonFile.getName(), e);
            }
        }
    }

    private static void replaceUrlsInDownloads(JsonObject downloads) {
        JsonObject artifact = downloads.getAsJsonObject("artifact");
        if (artifact != null && artifact.has("url")) {
            artifact.addProperty("url", DownloadEntry.applyMirrorUrl(artifact.get("url").getAsString()));
        }
        JsonObject classifiers = downloads.getAsJsonObject("classifiers");
        if (classifiers != null) {
            for (Map.Entry<String, JsonElement> entry : classifiers.entrySet()) {
                JsonObject classifier = entry.getValue().getAsJsonObject();
                if (classifier.has("url")) {
                    classifier.addProperty("url", DownloadEntry.applyMirrorUrl(classifier.get("url").getAsString()));
                }
            }
        }
    }
    private static JDialog finishedDialog() {
        JDialog dialog = new JDialog();
        String message = Messages.get("mmc.complete_message");
        JTextArea area = new JTextArea(message);
        area.setFocusable(false);
        GUIUtils.enlargeFont(area, Font.BOLD, 20);
        JButton quit = new JButton(Messages.get("button.quit"));
        GUIUtils.enlargeFont(quit, Font.BOLD, 20);
        quit.setActionCommand("quit");
        quit.addActionListener(actionEvent -> {
            if (actionEvent.getActionCommand().equals("quit")) {
                ExitWrapper.exit(0);
            }
        });
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(area, BorderLayout.NORTH);
        dialog.add(quit, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);
        return dialog;
    }

    private static void fetchLatestVersion() {
        versionFetchInProgress = true;
        latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + Messages.get("status.fetching_version"));
        new Thread(() -> {
            try {
                CleanroomVersionParser.resetVersion();
                String ver = CleanroomVersionParser.getVersion();
                latestVersionFetched = ver;
                latestVersionFetchFailed = false;
                versionFetchInProgress = false;
                SwingUtilities.invokeLater(() -> {
                    latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + ver);
                });
            } catch (Exception e) {
                latestVersionFetchFailed = true;
                versionFetchInProgress = false;
                SwingUtilities.invokeLater(() -> {
                    latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + Messages.get("status.version_fetch_failed"));
                });
            }
        }, "MMC Version Fetch Thread").start();
    }

    private static void updateVersionStatusBar() {
        File instance = Launch.minecraftHome.getParentFile();
        File installedPackJson = new File(instance, "mmc-pack.json");
        String installedVersion = null;
        if (installedPackJson.exists()) {
            installedVersion = CleanroomVersionParser.readVersionFromMMCPackJson(installedPackJson);
        }
        String installedDisplay = installedVersion != null ? installedVersion : "?";

        if (Config.useLocalPack && !Config.localPackPath.isEmpty()) {
            String localVer = CleanroomVersionParser.readVersionFromLocalMMCPack(Config.localPackPath);
            localVersionLabel.setText(Messages.get("label.local_version") + ": " + installedDisplay + " -> " + (localVer != null ? localVer : "?"));
        } else {
            localVersionLabel.setText(Messages.get("label.local_version") + ": " + installedDisplay);
            if (latestVersionFetched != null) {
                latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + latestVersionFetched);
            } else if (latestVersionFetchFailed) {
                latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + Messages.get("status.version_fetch_failed"));
            } else {
                latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + Messages.get("status.fetching_version"));
            }
        }
    }
}
