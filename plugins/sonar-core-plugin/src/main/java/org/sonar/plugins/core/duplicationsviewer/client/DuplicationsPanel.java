/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.duplicationsviewer.client;

import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.DefaultSourcePanel;
import org.sonar.gwt.ui.ExpandCollapseLink;
import org.sonar.gwt.ui.Loading;
import org.sonar.gwt.ui.SourcePanel;

import com.google.gwt.gen2.table.override.client.FlexTable;
import com.google.gwt.gen2.table.override.client.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public class DuplicationsPanel extends Composite {

  private final Panel panel;
  private Loading loading;

  public DuplicationsPanel(Resource resource) {
    panel = new VerticalPanel();
    loading = new Loading();
    panel.add(loading);
    initWidget(panel);
    setStyleName("gwt-DuplicationsPanel");
    getElement().setId("gwt-DuplicationsPanel");

    loadDuplications(resource);
  }

  public void loadDuplications(Resource resource) {
    ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.DUPLICATIONS_DATA);
    Sonar.getInstance().find(query, new DuplicationCallback());
  }

  private class DuplicationCallback extends AbstractCallback<Resource> {

    public DuplicationCallback() {
      super(loading);
    }

    @Override
    protected void doOnResponse(Resource resource) {
      loading.removeFromParent();
      String duplications = null;
      if (resource != null) {
        Measure data = resource.getMeasure(Metrics.DUPLICATIONS_DATA);
        if (data != null) {
          duplications = data.getData();
        }
      }
      if (duplications != null) {
        processDuplications(duplications, resource);
      }
    }

    private void processDuplications(String duplicationXMLData, Resource resource) {
      Document parsed = XMLParser.parse(duplicationXMLData);
      NodeList duplicationsXML = parsed.getElementsByTagName("duplication");
      
      FlexTable table = getDuplicationsTable();
      
      panel.add(table);
      int rowCounter = 1;
      for (int i = 0; i < duplicationsXML.getLength(); i++) {
        Element duplicationXML = (Element) duplicationsXML.item(i);
        String lines = duplicationXML.getAttribute("lines");
        String startLine = duplicationXML.getAttribute("start");
        String targetStartLine = duplicationXML.getAttribute("target-start");
        String targetResourceKey = duplicationXML.getAttribute("target-resource");
        renderDuplication(rowCounter, i, table, lines, startLine, targetStartLine, targetResourceKey, resource);
        rowCounter+=2;
      }
    }

    private FlexTable getDuplicationsTable() {
      FlexTable table = new FlexTable();
      table.setStylePrimaryName("duplicationsTable");
      table.setText(0, 0, "");
      table.setText(0, 1, "Nb lines");
      table.setText(0, 2, "From line");
      table.setText(0, 3, "File");
      table.setText(0, 4, "From line");
      
      table.getCellFormatter().getElement(0, 0).setId("expandCollapseCol");
      table.getCellFormatter().getElement(0, 1).setId("nbLineCol");
      table.getCellFormatter().getElement(0, 2).setId("lineFromCol");
      table.getCellFormatter().getElement(0, 3).setId("fileCol");
      
      setRowStyle(0, table, "header", false);
      return table;
    }

    private void renderDuplication(int row, int duplicationCounter, FlexTable table, String lines, String startLine, String targetStartLine, String targetResourceKey, final Resource resource) {
      String style = (duplicationCounter % 2 == 0) ? "odd" : "even";
      
      SourcePanel src = new DefaultSourcePanel(resource, new Integer(startLine), new Integer(lines));
      src.getElement().setId("source-panel-" + targetResourceKey.replace('.', '_'));
      src.setVisible(false);
      
      ExpandCollapseLink link = new ExpandCollapseLink(src);

      table.setWidget(row, 0, link);
      table.setText(row, 1, lines);
      table.setText(row, 2, startLine);
      if (targetResourceKey.equals(resource.getKey())) {
        targetResourceKey = "Same file";
      }
      if (targetResourceKey.contains(":")) {
        targetResourceKey = targetResourceKey.substring(targetResourceKey.lastIndexOf(':') + 1);
      }
      table.setText(row, 3, targetResourceKey);
      table.setText(row, 4, targetStartLine);
      setRowStyle(row, table, style, false);
      
      FlexCellFormatter frmt = (FlexCellFormatter)table.getCellFormatter();
      frmt.setColSpan(row + 1, 1, 4);
      table.setWidget(row + 1, 1, src);
      setRowStyle(row + 1, table, style, true);

    }

    private void setRowStyle(int row, FlexTable table, String style, boolean isPanelRow) {
      table.getCellFormatter().setStyleName(row, 0, style);
      table.getCellFormatter().setStyleName(row, 1, style);
      if (!isPanelRow) {
        table.getCellFormatter().setStyleName(row, 2, style);
        table.getCellFormatter().setStyleName(row, 3, style);
        table.getCellFormatter().setStyleName(row, 4, style);
      }
    }
  }
}
