/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import com.imsweb.geocoder.AboutDialog;
import com.imsweb.geocoder.Utils;
import com.imsweb.geocoder.component.GeocodeResultPanel;
import com.imsweb.geocoder.entity.GeocodeResult;

public class PanelLab extends JFrame implements ActionListener {

    public static final String VERSION = getVersion();

    public PanelLab() {
        this.setTitle("NAACCR Geocoder Review " + VERSION);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

        this.setPreferredSize(new Dimension(1024, 800));

        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu(" File ");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        bar.add(fileMenu);
        JMenuItem exitItem = new JMenuItem("Exit       ");
        exitItem.setActionCommand("menu-exit");
        exitItem.addActionListener(this);
        fileMenu.add(exitItem);
        JMenu helpMenu = new JMenu(" Help ");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        JMenuItem helpItem = new JMenuItem("View Help       ");
        helpItem.setActionCommand("menu-help");
        helpItem.addActionListener(this);
        helpMenu.add(helpItem);
        helpMenu.addSeparator();
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setActionCommand("menu-about");
        aboutItem.addActionListener(this);
        helpMenu.add(aboutItem);
        bar.add(helpMenu);
        this.setJMenuBar(bar);

        JPanel centerPnl = new JPanel(new BorderLayout());

        // !!!!!!!!!!!!!!!!!!!!!!!! BEGIN LAB

        JPanel contentPnl = new JPanel();
        contentPnl.setLayout(new BoxLayout(contentPnl, BoxLayout.X_AXIS));

        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("sample_input_csv.json");
            Assert.assertNotNull(url);
            for (GeocodeResult result : Utils.parseGeocodeResults(IOUtils.toString(url, StandardCharsets.US_ASCII))) {
                contentPnl.add(new GeocodeResultPanel(result));
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        JScrollPane pane = new JScrollPane(contentPnl);
        pane.setBorder(null);
        centerPnl.add(pane);

        // !!!!!!!!!!!!!!!!!!!!!!!! END LAB

        this.getContentPane().add(centerPnl, BorderLayout.CENTER);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> SwingUtilities.invokeLater(() -> {
            String msg = "An unexpected error happened, it is recommended to close the application.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
            JOptionPane.showMessageDialog(PanelLab.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }));
    }

    private static String getVersion() {
        String version = null;

        // this will make it work when running from the JAR file
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("NAACCR-GEOCODER-REVIEW-VERSION")) {
            if (is != null)
                version = IOUtils.readLines(is, StandardCharsets.US_ASCII).get(0);
        }
        catch (IOException e) {
            version = null;
        }

        // this will make it work when running from an IDE
        if (version == null) {
            try (FileInputStream is = new FileInputStream(System.getProperty("user.dir") + File.separator + "VERSION")) {
                version = IOUtils.readLines(is, StandardCharsets.US_ASCII).get(0);
            }
            catch (IOException e) {
                version = null;
            }
        }

        if (version == null)
            version = "??";

        return "v" + version;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ("menu-exit".equals(cmd))
            System.exit(0);
        else if ("menu-help".equals(cmd)) {
            try {
                File targetFile = File.createTempFile("naaccr-geocoder-help", ".html");
                targetFile.deleteOnExit();
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("help/help.html");
                OutputStream os = new FileOutputStream(targetFile);
                IOUtils.copy(is, os);
                is.close();
                os.close();
                Desktop.getDesktop().open(targetFile);
            }
            catch (RuntimeException | IOException ex) {
                JOptionPane.showMessageDialog(this, "Unable to display help.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if ("menu-about".equals(cmd)) {
            final JDialog dlg = new AboutDialog(this);
            dlg.pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Point center = new Point(screenSize.width / 2, screenSize.height / 2);
            dlg.setLocation(center.x - dlg.getWidth() / 2, center.y - dlg.getHeight() / 2);
            SwingUtilities.invokeLater(() -> dlg.setVisible(true));
        }
    }

    public static JLabel createItalicLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.ITALIC));
        return lbl;
    }

    public static JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        return lbl;
    }

    public static String formatNumber(int num) {
        DecimalFormat format = new DecimalFormat();
        format.setDecimalSeparatorAlwaysShown(false);
        return format.format(num);
    }

    public static String formatTime(long timeInMilli) {
        long hourBasis = 60;

        StringBuilder formattedTime = new StringBuilder();

        long secTmp = timeInMilli / 1000;
        long sec = secTmp % hourBasis;
        long minTmp = secTmp / hourBasis;
        long min = minTmp % hourBasis;
        long hour = minTmp / hourBasis;

        if (hour > 0) {
            formattedTime.append(hour).append(" hour");
            if (hour > 1)
                formattedTime.append("s");
        }

        if (min > 0) {
            if (formattedTime.length() > 0)
                formattedTime.append(", ");
            formattedTime.append(min).append(" minute");
            if (min > 1)
                formattedTime.append("s");
        }

        if (sec > 0) {
            if (formattedTime.length() > 0)
                formattedTime.append(", ");
            formattedTime.append(sec).append(" second");
            if (sec > 1)
                formattedTime.append("s");
        }

        if (formattedTime.length() > 0)
            return formattedTime.toString();

        return "< 1 second";
    }

    public static String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        else if (size < 1024 * 1024)
            return new DecimalFormat("#.# KB").format((double)size / 1024);
        else if (size < 1024 * 1024 * 1024)
            return new DecimalFormat("#.# MB").format((double)size / 1024 / 1024);

        return new DecimalFormat("#.# GB").format((double)size / 1024 / 1024 / 1024);
    }

    public static void main(String[] args) {

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
                // ignored, the look and feel will be the default Java one...
            }
            UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
            Insets insets = UIManager.getInsets("TabbedPane.tabAreaInsets");
            insets.bottom = 0;
            UIManager.put("TabbedPane.tabAreaInsets", insets);
        }

        final JFrame frame = new PanelLab();
        frame.pack();

        // start in the middle of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point center = new Point(screenSize.width / 2, screenSize.height / 2);
        frame.setLocation(center.x - frame.getWidth() / 2, center.y - frame.getHeight() / 2);

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }
}
