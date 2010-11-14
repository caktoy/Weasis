/*******************************************************************************
 * Copyright (c) 2010 Nicolas Roduit.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 ******************************************************************************/
package org.weasis.core.api.gui.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Enumeration;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.weasis.core.api.Messages;

/**
 * <p>
 * Title: PetroSpector
 * </p>
 * <p>
 * Description: Thin sections analysis
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author non attribuable
 * @version 1.0
 */

public abstract class AbstractWizardDialog extends JDialog {

    protected String settingTitle;
    protected AbstractItemDialogPage currentPage = null;
    protected DefaultMutableTreeNode pagesRoot = new DefaultMutableTreeNode("root"); //$NON-NLS-1$
    private final JPanel jPanelRootPanel = new JPanel();
    private final BorderLayout borderLayout3 = new BorderLayout();
    private final JButton jButtonCancel = new JButton();
    private final BorderLayout borderLayout2 = new BorderLayout();
    private final TreeSelection tree = new TreeSelection();
    protected JPanel jPanelButtom = new JPanel();
    private final JPanel jPanelMain = new JPanel();
    protected JScrollPane jScrollPanePage = new JScrollPane();
    private final GridBagLayout gridBagLayout1 = new GridBagLayout();
    private final JScrollPane jScrollPane1 = new JScrollPane();

    public AbstractWizardDialog(Frame frame, String title, boolean modal, Dimension pageSize) {
        super(frame, title, modal);
        this.settingTitle = title;
        try {
            jScrollPanePage.setPreferredSize(pageSize);
            jbInit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        jPanelMain.setLayout(borderLayout2);

        jButtonCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        jButtonCancel.setText(Messages.getString("AbstractWizardDialog.close")); //$NON-NLS-1$

        jPanelRootPanel.setLayout(borderLayout3);
        // jScrollPanePage.getViewport().setBackground(new Color(147, 182, 210));
        jScrollPanePage.setAutoscrolls(false);
        jPanelButtom.setLayout(gridBagLayout1);
        this.getContentPane().add(jPanelRootPanel, null);
        jPanelRootPanel.add(jPanelMain, BorderLayout.CENTER);
        jPanelMain.add(jScrollPanePage, BorderLayout.CENTER);
        jPanelRootPanel.add(jPanelButtom, BorderLayout.SOUTH);
        jPanelButtom.add(jButtonCancel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
            GridBagConstraints.NONE, new Insets(10, 10, 10, 15), 0, 0));
        jPanelRootPanel.add(jScrollPane1, java.awt.BorderLayout.WEST);
        jScrollPane1.getViewport().add(tree);
    }

    // Overridden so we can exit when window is closed
    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            cancel();
        }
        super.processWindowEvent(e);
    }

    protected abstract void initializePages();

    public void showPageFirstPage() {
        if (pagesRoot.getChildCount() > 0) {
            tree.setSelectionRow(0);
        }
    }

    public AbstractItemDialogPage getCurrentPage() {
        Object object = null;
        try {
            object = jScrollPanePage.getViewport().getComponent(0);
        } catch (Exception ex) {
        }
        if (object instanceof AbstractItemDialogPage) {
            return (AbstractItemDialogPage) object;
        }
        return null;
    }

    private void rowslection(AbstractItemDialogPage page) {
        if (page != null) {
            if (currentPage != null) {
                currentPage.deselectPageAction();
            }
            currentPage = page;
            currentPage.selectPageAction();
            jScrollPanePage.setViewportView(currentPage);
        }
    }

    /**
     * iniTree
     */
    protected void iniTree() {

        // fill up tree
        Enumeration children = pagesRoot.children();
        while (children.hasMoreElements()) {
            PageProps[] subpages = null;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            Object object = node.getUserObject();
            if (object instanceof AbstractItemDialogPage) {
                subpages = ((AbstractItemDialogPage) object).getSubPages();
            }

            if (subpages != null) {
                for (int j = 0; j < subpages.length; j++) {
                    node.add(new DefaultMutableTreeNode(subpages[j]));
                }
            }
        }
        DefaultTreeModel model = new DefaultTreeModel(pagesRoot, false);
        tree.constructTree(model);
        tree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                if (e.getNewLeadSelectionPath() != null) {
                    DefaultMutableTreeNode object =
                        (DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent();
                    if (object.getUserObject() instanceof AbstractItemDialogPage) {
                        rowslection((AbstractItemDialogPage) object.getUserObject());
                    }
                }
            }
        });
        Dimension dim = tree.getPreferredSize().getSize();
        dim.width += 5;
        jScrollPane1.setPreferredSize(dim);
    }

    public void closeAllPages() {
        Enumeration children = pagesRoot.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode page = (DefaultMutableTreeNode) children.nextElement();
            Object object = page.getUserObject();
            if (object instanceof AbstractItemDialogPage) {
                try {
                    ((AbstractItemDialogPage) object).closeAdditionalWindow();
                } catch (Exception ex) {
                    continue;
                }
            }
        }
    }

    protected void resetAlltoDefault() {
        Enumeration children = pagesRoot.children();
        while (children.hasMoreElements()) {
            PageProps[] subpages = null;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
            Object object = node.getUserObject();
            if (object instanceof AbstractItemDialogPage) {
                try {
                    AbstractItemDialogPage page = ((AbstractItemDialogPage) object);
                    subpages = page.getSubPages();
                    if (subpages != null) {
                        for (int j = 0; j < subpages.length; j++) {
                            subpages[j].resetoDefaultValues();
                        }
                    }
                    page.resetoDefaultValues();
                } catch (Exception ex) {
                    continue;
                }
            }
        }
    }

    public abstract void cancel();

    public void expandNode(int position) {
        tree.expandRow(position);
    }

    public JPanel getJPanelButtom() {
        return jPanelButtom;
    }

    public JButton getJButtonCancel() {
        return jButtonCancel;
    }

}