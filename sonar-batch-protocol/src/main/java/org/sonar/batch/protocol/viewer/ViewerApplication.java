/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.protocol.viewer;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Component;
import org.sonar.batch.protocol.output.BatchReport.Metadata;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.FileStructure.Domain;
import org.sonar.core.util.CloseableIterator;

public class ViewerApplication {

  private JFrame frame;
  private BatchReportReader reader;
  private Metadata metadata;
  private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
  private JTree componentTree;
  private JSplitPane splitPane;
  private JTabbedPane tabbedPane;
  private JScrollPane treeScrollPane;
  private JScrollPane componentDetailsTab;
  private JScrollPane highlightingTab;
  private JEditorPane componentEditor;
  private JEditorPane highlightingEditor;
  private JScrollPane sourceTab;
  private JEditorPane sourceEditor;
  private JScrollPane coverageTab;
  private JEditorPane coverageEditor;
  private TextLineNumber textLineNumber;
  private JScrollPane duplicationTab;
  private JEditorPane duplicationEditor;

  /**
   * Create the application.
   */
  public ViewerApplication() {
    initialize();
  }

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          ViewerApplication window = new ViewerApplication();
          window.frame.setVisible(true);

          window.loadReport();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

    });
  }

  private void loadReport() {
    final JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("Choose scanner report directory");
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setFileHidingEnabled(false);
    fc.setApproveButtonText("Open scanner report");
    int returnVal = fc.showOpenDialog(frame);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      try {
        loadReport(file);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        exit();
      }
    } else {
      exit();
    }

  }

  private void exit() {
    frame.setVisible(false);
    frame.dispose();
  }

  private void loadReport(File file) {
    reader = new BatchReportReader(file);
    metadata = reader.readMetadata();
    updateTitle();
    loadComponents();
  }

  private void loadComponents() {
    int rootComponentRef = metadata.getRootComponentRef();
    Component component = reader.readComponent(rootComponentRef);
    DefaultMutableTreeNode project = createNode(component);
    loadChildren(component, project);
    getComponentTree().setModel(new DefaultTreeModel(project));
  }

  private static DefaultMutableTreeNode createNode(Component component) {
    return new DefaultMutableTreeNode(component) {
      @Override
      public String toString() {
        return getNodeName((Component) getUserObject());
      }
    };
  }

  private static String getNodeName(Component component) {
    switch (component.getType()) {
      case PROJECT:
      case MODULE:
        return component.getName();
      case DIRECTORY:
      case FILE:
        return component.getPath();
      default:
        throw new IllegalArgumentException("Unknow component type: " + component.getType());
    }
  }

  private void loadChildren(Component parentComponent, DefaultMutableTreeNode parentNode) {
    for (int ref : parentComponent.getChildRefList()) {
      Component child = reader.readComponent(ref);
      DefaultMutableTreeNode childNode = createNode(child);
      parentNode.add(childNode);
      loadChildren(child, childNode);
    }

  }

  private void updateTitle() {
    frame.setTitle(metadata.getProjectKey() + (metadata.hasBranch() ? (" (" + metadata.getBranch() + ")") : "") + " " + sdf.format(new Date(metadata.getAnalysisDate())));
  }

  private void updateDetails(Component component) {
    componentEditor.setText(component.toString());
    updateHighlighting(component);
    updateSource(component);
    updateCoverage(component);
    updateDuplications(component);
  }

  private void updateDuplications(Component component) {
    duplicationEditor.setText("");
    if (reader.hasCoverage(component.getRef())) {
      try (CloseableIterator<BatchReport.Duplication> it = reader.readComponentDuplications(component.getRef())) {
        while (it.hasNext()) {
          BatchReport.Duplication dup = it.next();
          duplicationEditor.getDocument().insertString(duplicationEditor.getDocument().getEndPosition().getOffset(), dup.toString() + "\n", null);
        }
      } catch (Exception e) {
        throw new IllegalStateException("Can't read duplications for " + getNodeName(component), e);
      }
    }
  }

  private void updateCoverage(Component component) {
    coverageEditor.setText("");
    try (CloseableIterator<BatchReport.Coverage> it = reader.readComponentCoverage(component.getRef())) {
      while (it.hasNext()) {
        BatchReport.Coverage coverage = it.next();
        coverageEditor.getDocument().insertString(coverageEditor.getDocument().getEndPosition().getOffset(), coverage.toString() + "\n", null);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read code coverage for " + getNodeName(component), e);
    }
  }

  private void updateSource(Component component) {
    File sourceFile = reader.getFileStructure().fileFor(Domain.SOURCE, component.getRef());
    sourceEditor.setText("");

    if (sourceFile.exists()) {
      try (Scanner s = new Scanner(sourceFile, StandardCharsets.UTF_8.name()).useDelimiter("\\Z")) {
        if (s.hasNext()) {
          sourceEditor.setText(s.next());
        }
      } catch (IOException ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        sourceEditor.setText(errors.toString());
      }
    }
  }

  private void updateHighlighting(Component component) {
    highlightingEditor.setText("");
    try (CloseableIterator<BatchReport.SyntaxHighlighting> it = reader.readComponentSyntaxHighlighting(component.getRef())) {
      while (it.hasNext()) {
        BatchReport.SyntaxHighlighting rule = it.next();
        highlightingEditor.getDocument().insertString(highlightingEditor.getDocument().getEndPosition().getOffset(), rule.toString() + "\n", null);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read syntax highlighting for " + getNodeName(component), e);
    }
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    try {
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (Exception e) {
      // If Nimbus is not available, you can set the GUI to another look and feel.
    }
    frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    splitPane = new JSplitPane();
    frame.getContentPane().add(splitPane, BorderLayout.CENTER);

    tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    tabbedPane.setPreferredSize(new Dimension(500, 7));
    splitPane.setRightComponent(tabbedPane);

    componentDetailsTab = new JScrollPane();
    tabbedPane.addTab("Component details", null, componentDetailsTab, null);

    componentEditor = new JEditorPane();
    componentDetailsTab.setViewportView(componentEditor);

    sourceTab = new JScrollPane();
    tabbedPane.addTab("Source", null, sourceTab, null);

    sourceEditor = createSourceEditor();
    sourceEditor.setEditable(false);
    sourceTab.setViewportView(sourceEditor);

    textLineNumber = createTextLineNumber();
    sourceTab.setRowHeaderView(textLineNumber);

    highlightingTab = new JScrollPane();
    tabbedPane.addTab("Highlighting", null, highlightingTab, null);

    highlightingEditor = new JEditorPane();
    highlightingTab.setViewportView(highlightingEditor);

    coverageTab = new JScrollPane();
    tabbedPane.addTab("Coverage", null, coverageTab, null);

    coverageEditor = new JEditorPane();
    coverageTab.setViewportView(coverageEditor);

    duplicationTab = new JScrollPane();
    tabbedPane.addTab("Duplications", null, duplicationTab, null);

    duplicationEditor = new JEditorPane();
    duplicationTab.setViewportView(duplicationEditor);

    treeScrollPane = new JScrollPane();
    treeScrollPane.setPreferredSize(new Dimension(200, 400));
    splitPane.setLeftComponent(treeScrollPane);

    componentTree = new JTree();
    componentTree.setModel(new DefaultTreeModel(
      new DefaultMutableTreeNode("empty") {
        {
        }
      }));
    treeScrollPane.setViewportView(componentTree);
    componentTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    componentTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) componentTree.getLastSelectedPathComponent();

        if (node == null) {
          // Nothing is selected.
          return;
        }

        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        updateDetails((Component) node.getUserObject());
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

      }

    });
    frame.pack();
  }

  public JTree getComponentTree() {
    return componentTree;
  }

  /**
   * @wbp.factory
   */
  public static JPanel createComponentPanel() {
    JPanel panel = new JPanel();
    return panel;
  }

  protected JEditorPane getComponentEditor() {
    return componentEditor;
  }

  /**
   * @wbp.factory
   */
  public static JEditorPane createSourceEditor() {
    JEditorPane editorPane = new JEditorPane();
    return editorPane;
  }

  /**
   * @wbp.factory
   */
  public TextLineNumber createTextLineNumber() {
    TextLineNumber textLineNumber = new TextLineNumber(sourceEditor);
    return textLineNumber;
  }
}
