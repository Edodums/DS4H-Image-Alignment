package histology;

import ij.IJ;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JLabel lbl_icon;
    private JPanel pnl_title;
    private JLabel lbl_credits;
    private JLabel lbl_version;
    private JLabel lbl_supervisors;
    private JLabel lbl_supervisor1;
    private JLabel lbl_supervisor2;
    private JLabel lbl_author1;
    private JPanel pnl_credits;
    private JPanel pnl_heads;
    private JPanel pnl_authors;

    public AboutDialog() {
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.setMinimumSize(new Dimension(500, 300));
        this.setMaximumSize(new Dimension(500, 300));
        this.setResizable(false);
        this.setTitle("About...");

        this.lbl_author1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.lbl_author1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    Desktop.getDesktop().mail(new URI("mailto:stefano.belli4@studio.unibo.it"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        this.lbl_supervisor1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.lbl_supervisor1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    Desktop.getDesktop().mail(new URI("mailto:antonella.carbonaro@unibo.it"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        this.lbl_supervisor2.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.lbl_supervisor2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    Desktop.getDesktop().mail(new URI("mailto:f.piccinini@unibo.it"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible)
            this.pack();
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private void createUIComponents() {
        lbl_icon = new JLabel();

        ImageIcon imageIcon = null;
        try {
            byte[] bytes = Utilities.inputStreamToByteArray(getClass().getResourceAsStream("/info.png"));
            imageIcon = new ImageIcon(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Image image = imageIcon.getImage(); // transform it
        Image newimg = image.getScaledInstance(40, 40, Image.SCALE_SMOOTH); // scale it the smooth way
        imageIcon = new ImageIcon(newimg);  // transform it back

        lbl_icon.setIcon(imageIcon);

        pnl_title = new JPanel();
        pnl_title.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        lbl_version = new JLabel();
        lbl_version.setText("Histology plugin 18.05");

        lbl_credits = new JLabel();
        lbl_credits.setText("<html><body>Made by: Stefano Belli<br>With the supervision of: Prof. Antonella Carbonaro && Prof. Alberto Piccinini</body></html>");

        pnl_credits = new JPanel();
        pnl_credits.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        contentPane.setMaximumSize(new Dimension(500, 200));
        contentPane.setMinimumSize(new Dimension(500, 200));
        contentPane.setPreferredSize(new Dimension(500, 200));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panel1.setMaximumSize(new Dimension(412, 30));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        contentPane.add(panel1, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 5));
        panel2.setAlignmentY(0.5f);
        panel1.add(panel2, BorderLayout.CENTER);
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, -1, 10, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setHorizontalAlignment(10);
        label1.setText("Copyright (©) 2019 Data Science for Health (DS4H) Group. All rights reserved");
        panel2.add(label1, BorderLayout.NORTH);
        final JLabel label2 = new JLabel();
        label2.setAlignmentY(1.0f);
        Font label2Font = this.$$$getFont$$$(null, -1, 10, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setHorizontalAlignment(10);
        label2.setText("License: GNU General Public License version 3");
        panel2.add(label2, BorderLayout.SOUTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel1.add(panel3, BorderLayout.EAST);
        buttonOK = new JButton();
        buttonOK.setAlignmentY(1.0f);
        buttonOK.setBorderPainted(true);
        buttonOK.setContentAreaFilled(true);
        buttonOK.setHorizontalTextPosition(0);
        buttonOK.setMargin(new Insets(0, 0, 0, 0));
        buttonOK.setMaximumSize(new Dimension(78, 35));
        buttonOK.setMinimumSize(new Dimension(78, 35));
        buttonOK.setPreferredSize(new Dimension(78, 35));
        buttonOK.setText("CLOSE");
        buttonOK.setVerticalAlignment(0);
        panel3.add(buttonOK);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        panel4.setMaximumSize(new Dimension(500, 140));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 0, 10);
        contentPane.add(panel4, gbc);
        pnl_title.setLayout(new GridBagLayout());
        pnl_title.setBackground(new Color(-1512467));
        pnl_title.setMaximumSize(new Dimension(515, 20));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.15;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(pnl_title, gbc);
        lbl_icon.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        pnl_title.add(lbl_icon, gbc);
        lbl_version = new JLabel();
        lbl_version.setAlignmentX(0.0f);
        lbl_version.setAlignmentY(1.0f);
        lbl_version.setText("v. 0.7.6 0619");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl_title.add(lbl_version, gbc);
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$("Droid Sans Mono", Font.PLAIN, 20, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setForeground(new Color(-16777216));
        label3.setText("Histology Plugin");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnl_title.add(label3, gbc);
        pnl_credits.setLayout(new GridBagLayout());
        pnl_credits.setMaximumSize(new Dimension(138, 50));
        pnl_credits.setMinimumSize(new Dimension(138, 50));
        pnl_credits.setOpaque(false);
        pnl_credits.setPreferredSize(new Dimension(138, 50));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 5, 0, 5);
        panel4.add(pnl_credits, gbc);
        lbl_supervisors = new JLabel();
        lbl_supervisors.setText("Head of the project");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        pnl_credits.add(lbl_supervisors, gbc);
        final JLabel label4 = new JLabel();
        label4.setText("Made by:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl_credits.add(label4, gbc);
        pnl_heads = new JPanel();
        pnl_heads.setLayout(new GridBagLayout());
        pnl_heads.setMaximumSize(new Dimension(56, 32));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        pnl_credits.add(pnl_heads, gbc);
        lbl_supervisor1 = new JLabel();
        lbl_supervisor1.setText("<html><a href=\\\"\\\">Prof. Antonella Carbonaro</a></html>");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl_heads.add(lbl_supervisor1, gbc);
        lbl_supervisor2 = new JLabel();
        lbl_supervisor2.setText("<html><body><a href=\\\"\\\">Prof. Filippo Piccinini <a/></body></html>");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        pnl_heads.add(lbl_supervisor2, gbc);
        pnl_authors = new JPanel();
        pnl_authors.setLayout(new GridBagLayout());
        pnl_authors.setMaximumSize(new Dimension(40, 16));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        pnl_credits.add(pnl_authors, gbc);
        lbl_author1 = new JLabel();
        lbl_author1.setText("<html><body><a href=\\\"\\\">Stefano Belli<a/></body></html>");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl_authors.add(lbl_author1, gbc);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
