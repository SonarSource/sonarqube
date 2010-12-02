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
package org.sonar.plugins.core.testdetailsviewer.client;

import com.google.gwt.gen2.table.override.client.FlexTable;
import com.google.gwt.gen2.table.override.client.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.ExpandCollapseLink;
import org.sonar.gwt.ui.Loading;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public class TestsPanel extends Composite {

  private final Panel panel;
  private Loading loading;

  public TestsPanel(Resource resource) {
    panel = new VerticalPanel();
    loading = new Loading();
    panel.add(loading);
    initWidget(panel);
    setStyleName("gwt-TestDetailsPanel");
    getElement().setId("gwt-TestDetailsPanel");
    loadData(resource);
  }

  private void loadData(Resource resource) {
    ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.TEST_DATA);
    Sonar.getInstance().find(query, new TestDetailsMeasureHandler());
  }

  private class TestDetailsMeasureHandler extends AbstractCallback<Resource> {

    public TestDetailsMeasureHandler() {
      super(loading);
    }

    @Override
    protected void doOnResponse(Resource resource) {
      loading.removeFromParent();
      if (resource != null) {
        Measure measure = resource.getMeasure(Metrics.TEST_DATA);
        processTestDetails(measure.getData());
      }
    }

    private void processTestDetails(String testXMLData) {
      Document parsed = XMLParser.parse(testXMLData);
      NodeList testcasesXML = parsed.getElementsByTagName("testcase");

      FlexTable table = new FlexTable();
      table.setStylePrimaryName("detailsTable");
      table.setText(0, 0, "");
      table.setText(0, 1, "");
      table.setText(0, 2, "Duration");
      table.setText(0, 3, "Unit test name");
      table.getCellFormatter().getElement(0, 1).setId("iCol");
      table.getCellFormatter().getElement(0, 2).setId("dCol");
      setRowStyle(0, table, "header", false);

      int rowCounter = 1;
      for (int i = 0; i < testcasesXML.getLength(); i++) {
        Element testcaseXML = (Element) testcasesXML.item(i);
        String time = testcaseXML.getAttribute("time");
        String name = testcaseXML.getAttribute("name");
        String status = testcaseXML.getAttribute("status");
        Element error = getFirstElement("error", testcaseXML);
        Element failure = getFirstElement("failure", testcaseXML);
        Element stackTrace = status.equals("error") ? error : failure;
        renderTestDetails(rowCounter, i, status, stackTrace, name, time, table);
        rowCounter += 2;
      }
      panel.add(table);
    }

    private Element getFirstElement(String elementName, Element node) {
      NodeList elements = node.getElementsByTagName(elementName);
      return elements.getLength() > 0 ? (Element) elements.item(0) : null;
    }

    private void renderTestDetails(int row, int testCounter, String testCaseStatus, Element stackTrace, String name, String timeMS, FlexTable table) {

      HTML icon = new HTML("&nbsp;");
      icon.setStyleName(testCaseStatus);
      table.setWidget(row, 1, icon);
      table.setText(row, 2, timeMS + " ms");

      table.setText(row, 3, name);
      String style = (testCounter % 2 == 0) ? "odd" : "even";
      setRowStyle(row, table, style, false);

      if (stackTrace != null) {
        Panel stackPanel = new SimplePanel();
        stackPanel.setStyleName("stackPanel");
        stackPanel.getElement().setId("stack-panel" + name);
        stackPanel.setVisible(false);
        fillStackPanel(stackPanel, stackTrace);

        FlexCellFormatter frmt = (FlexCellFormatter) table.getCellFormatter();
        frmt.setColSpan(row + 1, 1, 3);
        table.setWidget(row + 1, 1, stackPanel);
        table.setWidget(row, 0, new ExpandCollapseLink(stackPanel));
        table.getCellFormatter().getElement(row, 0).setId("expandCollapseCol");
        setRowStyle(row + 1, table, style, true);
      }
    }

    private void setRowStyle(int row, FlexTable table, String style, boolean isPanelRow) {
      table.getCellFormatter().setStyleName(row, 0, style);
      table.getCellFormatter().setStyleName(row, 1, style);
      if (!isPanelRow) {
        table.getCellFormatter().setStyleName(row, 2, style);
        table.getCellFormatter().setStyleName(row, 3, style);
      }
      table.getCellFormatter().getElement(row, 0).setId("noLinkExpandCollapseCol");
    }
  }

  private void fillStackPanel(Panel p, Element stackElement) {
    String message = stackElement.getAttribute("message");
    if (message.length() > 0) {
      p.getElement().setInnerHTML(escapeHtml(message) + "<br/>" + stackLineBreaks(stackElement.getFirstChild().getNodeValue()));
    } else {
      p.getElement().setInnerHTML(stackLineBreaks(stackElement.getFirstChild().getNodeValue()));
    }
  }

  private String escapeHtml(String maybeHtml) {
    com.google.gwt.dom.client.Element div = DOM.createDiv();
    div.setInnerText(maybeHtml);
    return div.getInnerHTML();
  }

  private String stackLineBreaks(String s) {
    StringBuilder stack = new StringBuilder(256);
    for (String el : s.split("\n")) {
      stack.append(el.trim()).append("<br/>");
    }
    return stack.toString();
  }
}