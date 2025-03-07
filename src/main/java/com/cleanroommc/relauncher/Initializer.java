package com.cleanroommc.relauncher;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Initializer {
    private static final Object o = new Object();
    public static void InitJavaAndArg() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame mainDialog = new JFrame();
        mainDialog.setLayout(new BorderLayout());
        JFileChooser jvmPicker = new JFileChooser(new File(System.getProperty("user.home")));
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

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
        jvmPicker.setControlButtonsAreShown(false);
        mainDialog.add(jvmPicker, BorderLayout.NORTH);

        JTextField args = new JTextField("-Xmx4g -Xms4g");
        mainDialog.add(args, BorderLayout.CENTER);

        JCheckBox local = new JCheckBox("Use local pack");
        local.setToolTipText("Will use first Cleanroom-MMC-instance-*.zip in relauncher dir");
        local.addItemListener(itemEvent -> Config.useLocalPack = itemEvent.getStateChange() == ItemEvent.SELECTED);
        panel.add(local, BorderLayout.WEST);

        JDialog warning = new JDialog(mainDialog, Dialog.ModalityType.APPLICATION_MODAL);
        warning.setLayout(new BorderLayout());
        warning.add(new JLabel("Invalid Java path"), BorderLayout.CENTER);
        warning.setResizable(false);
        warning.setAlwaysOnTop(true);
        warning.pack();
        warning.setLocationRelativeTo(mainDialog);
        mainDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        mainDialog.setResizable(false);
        mainDialog.setLocationRelativeTo(null);

        JButton button = new JButton("Confirm");
        button.setActionCommand("confirm");
        button.addActionListener(actionEvent -> {
            if (actionEvent.getActionCommand().equals("confirm")) {
                mainDialog.dispose();
                synchronized (o) {
                    o.notify();
                }
                /*if (isJavaNewerThan21(jvmPicker.getSelectedFile())) {
                    mainDialog.dispose();
                    Relauncher.LOGGER.info("Java set");
                } else {
                    Relauncher.LOGGER.warn("Invalid Java");
                    warning.setVisible(true);
                }*/
            }
        });
        panel.add(button, BorderLayout.EAST);
        mainDialog.add(panel, BorderLayout.SOUTH);


        Relauncher.LOGGER.info("Launching GUI");
        mainDialog.pack();
        mainDialog.setVisible(true);
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                Relauncher.LOGGER.error(e);
            }
        }

        Config.javaPathDefault = jvmPicker.getSelectedFile().getAbsolutePath();
        Config.jvmArgsDefault = args.getText();
    }

    public static boolean isJavaNewerThan21(File file) {
        if (file == null) return false;
        String command = file.getPath() + " -version";

        Relauncher.LOGGER.info(command);
        ProcessBuilder builder = new ProcessBuilder(file.getAbsolutePath(), "-version");
        try {
            Process p = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<String> output = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
            reader.close();
            Relauncher.LOGGER.info(output);
            for (String s : output) {
                if (s.contains("version")) {
                    return Integer.parseInt(s.split("\"")[1].split("\\.")[0]) >= 21;
                }
            }
            return false;
        } catch (IOException e) {
            Relauncher.LOGGER.error(e);
            return false;
        }
    }
}
