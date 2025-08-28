package com.cleanroommc.relauncher;

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
    private static final JProgressBar mainProgressbar = new JProgressBar();
    private static final JLabel subStatusLabel = new JLabel();
    private static final JProgressBar subProgressbar = new JProgressBar(0, 100);
    private static JButton confirmButton;
    private static final JFrame mainFrame = new javax.swing.JFrame();
    private static Consumer<Boolean> setInteractable;
    public static void showGUI() {
        mainFrame.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JCheckBox proxyCheckbox = new JCheckBox("Use Proxy");
        JTextField proxyAddrTextField = new JTextField();
        JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
        JCheckBox useLocalCheckbox = new JCheckBox("Use Local Pack");

        setInteractable = value -> {
            confirmButton.setEnabled(value);
            proxyCheckbox.setEnabled(value);
            if (!value || proxyCheckbox.isSelected()) {
                proxyAddrTextField.setEnabled(value);
                portSpinner.setEnabled(value);
            }
            useLocalCheckbox.setEnabled(value);
        };

        confirmButton = new JButton("Confirm");

        c.gridheight = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.CENTER;
        mainFrame.add(useLocalCheckbox, c);
        c.gridx = 2;
        mainFrame.add(proxyCheckbox, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        mainFrame.add(proxyAddrTextField, c);
        c.gridx = 2;
        c.gridwidth = 1;
        mainFrame.add(portSpinner, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 3;
        c.ipady = 10;
        mainFrame.add(mainStatusLabel, c);
        c.gridy = 3;
        mainFrame.add(mainProgressbar, c);
        c.gridy = 4;
        mainFrame.add(subStatusLabel, c);
        c.gridy = 5;
        mainFrame.add(subProgressbar, c);
        c.gridy = 6;
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


        confirmButton.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirmButton)) {
                setInteractable.accept(false);

                mainProgressbar.setIndeterminate(false);
                subProgressbar.setIndeterminate(false);

                try {
                    Thread workingThread = new Thread(() -> {
                        try {
                            MMCPackDownloader.downloadAndExtract();
                            mainStatusLabel.setText("Installing Cleanroom...");
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
                    mainProgressbar.setIndeterminate(true);
                    subProgressbar.setIndeterminate(true);
                }
            }
        });




        useLocalCheckbox.setToolTipText("Will use first Cleanroom-MMC-instance-*.zip in relauncher dir");
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
