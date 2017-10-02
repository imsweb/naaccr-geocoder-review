/*
 * Copyright (C) 2017 Information Management Services, Inc.
 */
package com.imsweb.geocoder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.io.IOUtils;

import com.imsweb.geocoder.component.InputSelectionPanel;
import com.imsweb.geocoder.component.OutputSelectionPanel;
import com.imsweb.geocoder.component.ProcessingPanel;
import com.imsweb.geocoder.component.SummaryPanel;
import com.imsweb.geocoder.entity.Session;

public class Standalone extends JFrame implements ActionListener {

    public static final String VERSION = getVersion();

    public static final String PANEL_ID_INPUT = "input";
    public static final String PANEL_ID_OUTPUT = "output";
    public static final String PANEL_ID_PROCESS = "process";
    public static final String PANEL_ID_SUMMARY = "summary";

    private Session _session;

    private CardLayout _layout;
    private JPanel _centerPnl;
    private ProcessingPanel _processingPanel;

    public Standalone() {
        this.setTitle("NAACCR Geocoder Review " + VERSION);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performExit();
            }
        });
        this.getContentPane().setLayout(new BorderLayout());
        int prefWidth = 1020;
        if (Toolkit.getDefaultToolkit().getScreenSize().width > 1200)
            prefWidth = 1200;
        int prefHeight = 700;
        if (Toolkit.getDefaultToolkit().getScreenSize().width > 800)
            prefHeight = 800;
        this.setPreferredSize(new Dimension(prefWidth, prefHeight));

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
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setActionCommand("menu-about");
        aboutItem.addActionListener(this);
        helpMenu.add(aboutItem);
        bar.add(helpMenu);
        this.setJMenuBar(bar);

        _session = new Session();
        _session.setVersion(getVersion().replace("v", ""));

        _layout = new CardLayout();
        _centerPnl = new JPanel(_layout);
        _centerPnl.add(PANEL_ID_INPUT, new InputSelectionPanel(this));
        this.getContentPane().add(_centerPnl, BorderLayout.CENTER);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> SwingUtilities.invokeLater(() -> {
            boolean isLocationException = e instanceof IllegalComponentStateException; // https://bugs.openjdk.java.net/browse/JDK-8179665
            if (!isLocationException) {

                // copy the full stacktrace to the clipboard, little trick to make tech support possible
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                e.printStackTrace(ps);
                StringSelection data = new StringSelection(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(data, data);

                String msg = "An unexpected error happened, it is recommended to close the application.\n\n   Error: " + (e.getMessage() == null ? "null access" : e.getMessage());
                JOptionPane.showMessageDialog(Standalone.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }));
    }

    public Session getSession() {
        return _session;
    }

    public void showPanel(String panelId) {
        if (PANEL_ID_OUTPUT.equals(panelId))
            _centerPnl.add(PANEL_ID_OUTPUT, new OutputSelectionPanel(this));
        else if (PANEL_ID_PROCESS.equals(panelId)) {
            _processingPanel = new ProcessingPanel(this);
            // If we are done with the file, don't show the processing panel
            if (!_processingPanel.reachedEndOfFile())
                _centerPnl.add(PANEL_ID_PROCESS, _processingPanel);
        }
        else if (PANEL_ID_SUMMARY.equals(panelId))
            _centerPnl.add(PANEL_ID_SUMMARY, new SummaryPanel(this));
        SwingUtilities.invokeLater(() -> _layout.show(_centerPnl, panelId));
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

    public void performExit() {
        if (_processingPanel != null) {
            _processingPanel.closeStreams();

            // If we are not done processing, create a progress file
            // If we are done, remove progress and tmp files from this session
            if (!_processingPanel.reachedEndOfFile()) {
                File inputFile = _session.getInputFile();
                try {
                    Utils.writeSessionToProgressFile(_session, Utils.getProgressFile(inputFile));
                }
                catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Unable to save progress file. Your progress will be lost.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            else {
                // Delete any existing progress file for this session
                File inputFile = getSession().getInputFile();
                File progressFile = Utils.getProgressFile(inputFile);
                if (progressFile.exists())
                    if (!progressFile.delete())
                        JOptionPane.showMessageDialog(this, "Unable to delete progress file. Please delete it by hand.", "Error", JOptionPane.ERROR_MESSAGE);

                // Delete any existing tmp file for this session
                File tmpFile = getSession().getTmpInputFile();
                if (tmpFile != null && tmpFile.exists())
                    if (!tmpFile.delete())
                        JOptionPane.showMessageDialog(this, "Unable to delete tmp file. Please delete it by hand.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        System.exit(0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ("menu-exit".equals(cmd))
            performExit();
        else if ("menu-about".equals(cmd)) {
            final JDialog dlg = new AboutDialog(this);
            dlg.pack();
            Point center = new Point();
            center.setLocation(this.getLocationOnScreen().x + this.getWidth() / 2, this.getLocationOnScreen().y + this.getHeight() / 2);
            dlg.setLocation(center.x - dlg.getWidth() / 2, center.y - dlg.getHeight() / 2);
            SwingUtilities.invokeLater(() -> dlg.setVisible(true));
        }
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

        final JFrame frame = new Standalone();
        frame.pack();

        // start in the middle of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point center = new Point(screenSize.width / 2, screenSize.height / 2);
        frame.setLocation(center.x - frame.getWidth() / 2, center.y - frame.getHeight() / 2);

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }
}
