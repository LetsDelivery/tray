package qz.ui;

import qz.auth.RequestState;
import qz.common.Constants;
import qz.ui.component.IconCache;
import qz.ui.component.LinkLabel;
import qz.utils.SystemUtilities;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Created by Tres on 2/19/2015.
 * A basic allow/block dialog with support for displaying Certificate information
 */
public class GatewayDialog extends JDialog implements Themeable {

    private JLabel verifiedLabel;
    private JLabel descriptionLabel;
    private LinkLabel certInfoLabel;
    private JPanel descriptionPanel;

    private DetailsDialog detailsDialog;

    private JButton allowButton;
    private JButton blockButton;
    private JPanel optionsPanel;

    private JCheckBox persistentCheckBox;
    private JPanel bottomPanel;

    private JPanel mainPanel;

    private final IconCache iconCache;

    private String description;
    private RequestState request;
    private boolean approved;
    private boolean persistent;

    public GatewayDialog(Frame owner, String title, IconCache iconCache) {
        super(owner, title, true);
        this.iconCache = iconCache;
        this.description = "";
        this.approved = false;
        this.setIconImages(iconCache.getImages(IconCache.Icon.TASK_BAR_ICON));
        initComponents();
        refreshComponents();
    }

    private void initComponents() {
        descriptionPanel = new JPanel();
        verifiedLabel = new JLabel();
        verifiedLabel.setBorder(new EmptyBorder(3, 3, 3, 3));
        descriptionLabel = new JLabel();

        descriptionPanel.add(verifiedLabel);
        descriptionPanel.add(descriptionLabel);
        descriptionPanel.setBorder(new EmptyBorder(3, 3, 3, 3));

        optionsPanel = new JPanel();
        allowButton = new JButton("Allow", iconCache.getIcon(IconCache.Icon.ALLOW_ICON));
        allowButton.setMnemonic(KeyEvent.VK_A);
        blockButton = new JButton("Block", iconCache.getIcon(IconCache.Icon.BLOCK_ICON));
        blockButton.setMnemonic(KeyEvent.VK_B);
        allowButton.addActionListener(buttonAction);
        blockButton.addActionListener(buttonAction);

        detailsDialog = new DetailsDialog(iconCache);
        certInfoLabel = new LinkLabel();
        certInfoLabel.setAlignmentX(LEFT_ALIGNMENT);
        certInfoLabel.addActionListener(e -> {
            detailsDialog.updateDisplay(request);
            JOptionPane.showMessageDialog(
                    GatewayDialog.this,
                    detailsDialog,
                    "Details",
                    JOptionPane.PLAIN_MESSAGE);
        });

        bottomPanel = new JPanel();
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        persistentCheckBox = new JCheckBox(Constants.REMEMBER_THIS_DECISION, false);
        persistentCheckBox.setMnemonic(KeyEvent.VK_R);
        persistentCheckBox.addActionListener(e -> allowButton.setEnabled(!persistentCheckBox.isSelected() || request.isVerified()));
        persistentCheckBox.setAlignmentX(RIGHT_ALIGNMENT);

        bottomPanel.add(certInfoLabel);
        bottomPanel.add(persistentCheckBox);

        optionsPanel.add(allowButton);
        optionsPanel.add(blockButton);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        mainPanel.add(descriptionPanel);
        mainPanel.add(optionsPanel);
        mainPanel.add(new JSeparator());
        mainPanel.add(bottomPanel);

        getContentPane().add(mainPanel);

        allowButton.requestFocusInWindow();

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
        pack();

        setAlwaysOnTop(true);
        setLocationRelativeTo(null);    // center on main display
    }

    @Override
    public void refresh() {
        ThemeUtilities.refreshAll(this, detailsDialog);
        refreshComponents();
    }

    private final transient ActionListener buttonAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            approved = e.getSource().equals(allowButton);
            persistent = persistentCheckBox.isSelected();

            // Require confirmation for permanent block
            if (!approved && persistent) {
                ConfirmDialog confirmDialog = new ConfirmDialog(null, "Please Confirm", iconCache);
                String message = Constants.BLOCK_SITES_TEXT.replace(" blocked ", " block ") + "?";
                message = String.format(message, request.hasCertificate()? request.getCertName():"");
                if (!confirmDialog.prompt(message)) {
                    persistent = false;
                    return;
                }

                // FIXME: Handle race condition with MacOS Ventura; see also https://github.com/qzind/tray/issues/1007
                if (SystemUtilities.isMac() && SystemUtilities.getOSVersion().getMajorVersion() == 13) {
                    try {
                        Thread.sleep(500);
                    }
                    catch(InterruptedException ignore) {}
                }
            }
            setVisible(false);
        }
    };

    public final void refreshComponents() {
        if (request != null) {
            // TODO:  Add name, publisher
            descriptionLabel.setText("<html>" +
                                             String.format(description, "<p>" + request.getCertName()) +
                                             "</p><strong>" + request.getValidityInfo() + "</strong>" +
                                             "</html>");
            certInfoLabel.setText("View request details");

            IconCache.Icon trustIcon;
            Color detailColor = Constants.TRUSTED_COLOR;
            if (request.isVerified()) {
                //cert and signature are good
                trustIcon = IconCache.Icon.TRUST_VERIFIED_ICON;
            } else if (request.getCertUsed().isValid()) {
                //cert is good, but there is an issue with the signature
                trustIcon = IconCache.Icon.TRUST_ISSUE_ICON;
                detailColor = Constants.WARNING_COLOR;
            } else {
                //nothing is good
                trustIcon = IconCache.Icon.TRUST_MISSING_ICON;
                detailColor = Constants.WARNING_COLOR;
            }

            verifiedLabel.setIcon(iconCache.getIcon(trustIcon));
            certInfoLabel.setForeground(detailColor);
        } else {
            descriptionLabel.setText(description);
            verifiedLabel.setIcon(null);
        }

        persistentCheckBox.setSelected(false);
        allowButton.setEnabled(true);
        allowButton.requestFocusInWindow();
        pack();
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setRequest(RequestState req) {
        request = req;
    }

    public RequestState getRequest() {
        return request;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean prompt(String description, RequestState request, Point position) {
        //reset dialog state on new prompt
        approved = false;
        persistent = false;
        persistentCheckBox.setSelected(false);

        if (request == null || request.hasBlockedCert()) {
            approved = false;
            return false;
        }
        if (request.hasSavedCert()) {
            approved = true;
            return true;
        }

        setDescription(description);
        setRequest(request);
        refreshComponents();
        SystemUtilities.centerDialog(this, position);
        setVisible(true);

        return isApproved();
    }
}

