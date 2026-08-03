package com.cleanroommc.relauncher;

import net.miginfocom.swing.MigLayout;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.ExitWrapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

        JSeparator jSeparator1 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator3 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator4 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator5 = new JSeparator(JSeparator.HORIZONTAL);

        setInteractable = value -> {
            confirmButton.setEnabled(value && (Config.useLocalPack || isLatestVersionAvailable()));
            if (!value || !useLocalCheckbox.isSelected()) {
                proxyAddrTextField.setEnabled(value);
                portSpinner.setEnabled(value);
            }
            useLocalCheckbox.setEnabled(value);
            mmcLocalPackPathField.setEnabled(value && Config.useLocalPack);
            mmcLocalPackBrowserButton.setEnabled(value && Config.useLocalPack);
        };

        confirmButton = new JButton(Messages.get("button.confirm"));
        updateConfirmButtonState();

        int row = 0;
        mainFrame.add(useLocalCheckbox, "cell 0 " + row + " 2 1, grow, align center");
        GUIUtils.enlargeFont(useLocalCheckbox);
        useLocalCheckbox.setHorizontalAlignment(JCheckBox.CENTER);
        row++;

        mainFrame.add(mmcLocalPackPathField, "cell 0 " + row + " 3 1, grow");
        mainFrame.add(mmcLocalPackBrowserButton, "cell 3 " + row + ", grow");
        row++;

        mainFrame.add(jSeparator1, "cell 0 " + row + " 4 1, grow");
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

                try {
                    Thread workingThread = new Thread(() -> {
                        try {
                            mainStatusLabel.setText(Messages.get("status.installing"));
                            MMCPackDownloader.downloadAndExtract();
                            File packDir = new File(Relauncher.workingDir, "mmcpack").getAbsoluteFile();
                            File instance = Launch.minecraftHome.getParentFile();
                            Relauncher.LOGGER.info("Instance directory: {}", instance.getAbsolutePath());
                            File packFile = new File(packDir, "mmcpack.zip");
                            File sourceLibraries = new File(packDir, "libraries");
                            File patches = new File(instance, "patches");
                            patches.mkdirs();
                            try {
                                if (MMCPackDownloader.containsLibrariesDirectory(packFile)) {
                                    if (!sourceLibraries.isDirectory()) {
                                        throw new IOException("MMC pack libraries directory was not extracted: " + sourceLibraries.getAbsolutePath());
                                    }
                                    FileUtils.copyDirectory(sourceLibraries, new File(instance, "libraries"));
                                }
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

        mmcLocalPackPathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange(); }
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
        updateVersionStatusBar();
        fetchLatestVersion();
        packMainFrame();
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
        latestVersionFetched = null;
        latestVersionFetchFailed = false;
        latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + Messages.get("status.fetching_version"));
        updateConfirmButtonState();
        packMainFrame();
        new Thread(() -> {
            try {
                CleanroomVersionParser.resetVersion();
                String ver = CleanroomVersionParser.getVersion();
                latestVersionFetched = ver;
                latestVersionFetchFailed = false;
                versionFetchInProgress = false;
                SwingUtilities.invokeLater(() -> {
                    latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + ver);
                    updateConfirmButtonState();
                    packMainFrame();
                });
            } catch (Exception e) {
                latestVersionFetchFailed = true;
                versionFetchInProgress = false;
                SwingUtilities.invokeLater(() -> {
                    latestVersionLabel.setText(Messages.get("label.latest_version") + ": " + Messages.get("status.version_fetch_failed"));
                    updateConfirmButtonState();
                    packMainFrame();
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

        if (Config.useLocalPack) {
            String targetVersion = CleanroomVersionParser.BUNDLED_VERSION;
            if (!Config.localPackPath.isEmpty()) {
                String localVer = CleanroomVersionParser.readVersionFromLocalMMCPack(Config.localPackPath);
                targetVersion = localVer != null ? localVer : "?";
            }
            localVersionLabel.setText(Messages.get("label.local_version") + ": " + installedDisplay + " -> " + targetVersion);
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
        updateConfirmButtonState();
        packMainFrame();
    }

    private static void updateConfirmButtonState() {
        confirmButton.setEnabled(Config.useLocalPack || isLatestVersionAvailable());
    }

    private static boolean isLatestVersionAvailable() {
        return !versionFetchInProgress && !latestVersionFetchFailed && latestVersionFetched != null;
    }

    private static void packMainFrame() {
        mainFrame.pack();
        GUIUtils.setCentral(mainFrame);
    }
}
