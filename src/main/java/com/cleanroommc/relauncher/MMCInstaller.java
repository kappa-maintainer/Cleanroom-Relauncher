package com.cleanroommc.relauncher;

import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.ExitWrapper;
import org.apache.commons.io.FileUtils;


import javax.swing.*;
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
    private static JButton confirmButton;
    private static final JFrame mainFrame = new JFrame();
    private static Consumer<Boolean> setInteractable;
    public static void showGUI() {
        mainFrame.setLayout(new MigLayout());

        JLabel host = new JLabel("Proxy Host");
        JTextField proxyAddrTextField = new JTextField();
        proxyAddrTextField.setText(Config.proxyAddr);

        JLabel port = new JLabel("Proxy Port");
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
        portSpinner.setValue(Config.proxyPort);
        JCheckBox useLocalCheckbox = new JCheckBox("Use Local Pack");

        JSeparator jSeparator1 = new JSeparator(JSeparator.HORIZONTAL);
        JSeparator jSeparator2 = new JSeparator(JSeparator.HORIZONTAL);
        
        setInteractable = value -> {
            confirmButton.setEnabled(value);
            if (!value || !useLocalCheckbox.isSelected()) {
                proxyAddrTextField.setEnabled(value);
                portSpinner.setEnabled(value);
            }
            useLocalCheckbox.setEnabled(value);
        };

        confirmButton = new JButton("Confirm");
        
        mainFrame.add(useLocalCheckbox, "cell 0 0 2 1, grow, align center");
        GUIUtils.enlargeFont(useLocalCheckbox);
        useLocalCheckbox.setHorizontalAlignment(JCheckBox.CENTER);
        
        mainFrame.add(jSeparator1, "cell 0 1 2 1, grow");
        
        mainFrame.add(host, "cell 0 2, grow");
        GUIUtils.enlargeFont(host);
        mainFrame.add(proxyAddrTextField, "cell 1 2, grow");
        GUIUtils.enlargeFont(proxyAddrTextField);
        proxyAddrTextField.setMinimumSize(new Dimension(200, 10));
        mainFrame.pack();
        mainFrame.add(port, "cell 0 3, grow");
        GUIUtils.enlargeFont(port);
        mainFrame.add(portSpinner, "cell 1 3, grow");
        GUIUtils.enlargeFont(portSpinner);
        portSpinner.setMinimumSize(new Dimension(200, 10));
        int width = port.getWidth() + portSpinner.getWidth();
        mainFrame.add(mainStatusLabel, "cell 0 4 2 1, grow");
        mainStatusLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(mainStatusLabel);
        mainStatusLabel.setMinimumSize(new Dimension(width, 10));
        
        mainFrame.add(jSeparator2, "cell 0 5 2 1, grow");
        
        jSeparator1.setMinimumSize(new Dimension(width, 0));
        jSeparator2.setMinimumSize(new Dimension(width, 0));;
        
        mainFrame.add(confirmButton, "cell 0 6 2 1, grow");
        GUIUtils.enlargeFont(confirmButton, Font.BOLD, 20);
        confirmButton.setMinimumSize(new Dimension(width, 10));

        mainFrame.setTitle("Relauncher Initialization Settings");
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
                Config.proxyAddr = proxyAddrTextField.getText();
                Config.proxyPort = (int) portSpinner.getValue();

                try {
                    Thread workingThread = new Thread(() -> {
                        try {
                            mainStatusLabel.setText("Installing Cleanroom...");
                            MMCPackDownloader.downloadAndExtract();
                            File packDir = new File(Relauncher.workingDir, "mmcpack").getAbsoluteFile();
                            File instance = Launch.minecraftHome.getParentFile();
                            Relauncher.LOGGER.info("Instance directory: {}", instance.getAbsolutePath());
                            File libraries = new File(instance, "libraries");
                            File patches = new File(instance, "patches");
                            libraries.mkdirs();
                            patches.mkdirs();
                            try {
                                FileUtils.copyDirectory(new File(packDir, "libraries"), libraries);
                                FileUtils.copyDirectory(new File(packDir, "patches"), patches);
                                FileUtils.copyFile(new File(packDir, "mmc-pack.json"), new File(instance, "mmc-pack.json"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            Relauncher.LOGGER.info("Install finished");
                            JDialog dialog = finishedDialog();
                            GUIUtils.setCentral(dialog);
                            dialog.setVisible(true);
                            synchronized (Relauncher.o) {
                                Relauncher.o.wait();
                            }

                        } catch (Throwable e) {
                            mainStatusLabel.setText(e.getMessage());
                        }
                        Config.save();
                        synchronized (Relauncher.o) {
                            Relauncher.o.notify();
                        }
                    }, "MMC installation Working Thread");
                    workingThread.start();
                } catch (Throwable t) {
                    Relauncher.LOGGER.error(t.getMessage());
                    Arrays.stream(t.getStackTrace()).forEach(Relauncher.LOGGER::info);
                    Config.configFile.delete();
                    mainStatusLabel.setText(t.getMessage());
                    setInteractable.accept(true);
                }
            }
        });




        useLocalCheckbox.setToolTipText("Will use first Cleanroom-MMC-instance-*.zip in relauncher dir");
        useLocalCheckbox.setSelected(Config.useLocalPack);
        proxyAddrTextField.setText(Config.proxyAddr);
        portSpinner.setValue(Config.proxyPort);

        useLocalCheckbox.setToolTipText("Use the pack placed in /relauncher. Will disable proxy settings.");
        useLocalCheckbox.addItemListener(itemEvent -> {
            boolean selected = itemEvent.getStateChange() == ItemEvent.SELECTED;
            portSpinner.setEnabled(!selected);
            proxyAddrTextField.setEnabled(!selected);
        });

        proxyAddrTextField.setToolTipText("Proxy Address");
        proxyAddrTextField.setEnabled(!useLocalCheckbox.isSelected());

        portSpinner.setEnabled(!useLocalCheckbox.isSelected());
        portSpinner.setToolTipText("Proxy Port");
        portSpinner.setModel(new SpinnerNumberModel());

        mainStatusLabel.setText("Status: Idle");
        mainStatusLabel.setHorizontalAlignment(JTextField.CENTER);
        

        Relauncher.LOGGER.info("Launching GUI");
        mainFrame.validate();
        //mainFrame.setSize(GUIUtils.scaledWidth / 2, GUIUtils.scaledHeight / 4);
        mainFrame.pack();
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

    private static JDialog finishedDialog() {
        JDialog dialog = new JDialog();
        JTextArea area = new JTextArea("MMC pack installation complete. After clicking the quit button, please:\n1. Switch your java in instance settings to Java 21+\n2. Make sure you have installed Scalar Legacy and Fugue");
        area.setFocusable(false);
        GUIUtils.enlargeFont(area, Font.BOLD, 20);
        JButton quit = new JButton("Quit");
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
}
