package com.cleanroommc.relauncher;

import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Consumer;

public class JvmDetectorDialog {

    public static void show(JFrame parent, Consumer<String> onSelected, Runnable onOpen, Runnable onClose) {
        JDialog detector = new JDialog(parent);
        detector.setFocusableWindowState(true);
        detector.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        detector.setLayout(new MigLayout(
            "",
            "[grow][grow]",
            "[grow][grow][grow][grow]"
        ));
        DefaultListModel<JVMInfo> model = new DefaultListModel<>();
        JList<JVMInfo> list = new JList<>(model);
        JLabel info = new JLabel(Messages.get("status.idle"));
        info.setHorizontalAlignment(JLabel.CENTER);
        JButton confirm = new JButton(Messages.get("button.confirm"));
        JButton cancel = new JButton(Messages.get("button.cancel"));

        JLabel warningLabel = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        warningLabel.setText(Messages.get("warning.need_java25"));
        warningLabel.setHorizontalAlignment(JLabel.CENTER);
        GUIUtils.enlargeFont(warningLabel);

        detector.add(list, "cell 0 0 2 2, grow");
        GUIUtils.enlargeFont(list);
        detector.add(warningLabel, "cell 0 2 2 1, grow");
        detector.add(info, "cell 0 3 2 1, grow");
        GUIUtils.enlargeFont(info);
        detector.add(cancel, "cell 0 4, grow");
        GUIUtils.enlargeFont(cancel);
        detector.add(confirm, "cell 1 4, grow");
        GUIUtils.enlargeFont(confirm, Font.BOLD, 20);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.addListSelectionListener(listSelectionEvent -> {
            JVMInfo selectedValue = list.getSelectedValue();
            if (selectedValue != null) {
                String path = selectedValue.getFile().getAbsolutePath();
                info.setText(path);
                info.setToolTipText(path);
                detector.pack();
            }
        });
        list.setMinimumSize(new Dimension(300, 200));

        cancel.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(cancel)) {
                detector.setVisible(false);
                onClose.run();
            }
        });

        confirm.addActionListener(actionEvent -> {
            if (actionEvent.getSource().equals(confirm)) {
                JVMInfo selected = list.getSelectedValue();
                if (selected != null) {
                    detector.setVisible(false);
                    onClose.run();
                    onSelected.accept(selected.getFile().getAbsolutePath());
                }
            }
        });

        cancel.setEnabled(false);
        confirm.setEnabled(false);
        info.setText(Messages.get("status.scanning"));
        Relauncher.LOGGER.info("Scanning...");
        detector.pack();
        GUIUtils.setCentral(detector);
        detector.setAlwaysOnTop(true);
        onOpen.run();
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
}
