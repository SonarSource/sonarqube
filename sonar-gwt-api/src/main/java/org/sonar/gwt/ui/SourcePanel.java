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
package org.sonar.gwt.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.widgetideas.table.client.PreloadedTable;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.Source;
import org.sonar.wsclient.services.SourceQuery;

import java.util.*;

public abstract class SourcePanel extends Composite implements ClickHandler {

  private static final int MAX_LINES_BY_BLOCK = 3000;

  private final Panel panel = new VerticalPanel();
  private PreloadedTable rowsTable;
  private Source source;
  private final Loading loading = new Loading();
  private int from = 0;
  private int length = 0;

  private boolean started = false;
  private Resource resource;
  private boolean hasNoSources = false;

  // pagination
  private Button moreButton = null;
  private int currentRow = 0;
  private int offset = 0;
  private boolean firstPage = true;
  private boolean previousLineIsDecorated = true;
  private boolean firstDecoratedLine = true;

  public SourcePanel(Resource resource) {
    this(resource, 0, 0);
  }

  public Resource getResource() {
    return resource;
  }

  public SourcePanel(Resource resource, int from, int length) {
    this.from = from;
    this.length = length;
    this.resource = resource;

    panel.add(loading);
    panel.getElement().setId("sourcePanel");
    initWidget(panel);
    setStyleName("gwt-SourcePanel");

    loadSources();
  }

  public void onClick(ClickEvent clickEvent) {
    if (clickEvent.getSource() == moreButton) {
      hideMoreButton();
      displaySource();
    }
  }

  public Button getMoreButton() {
    if (moreButton == null) {
      moreButton = new Button("More");
      moreButton.getElement().setId("more_source");
      moreButton.addClickHandler(this);
    }
    return moreButton;
  }

  public void showMoreButton() {
    hideMoreButton();
    panel.add(getMoreButton());
  }

  public void hideMoreButton() {
    getMoreButton().removeFromParent();
  }


  public void refresh() {
    if (!hasNoSources) {
      panel.clear();
      panel.add(loading);

      rowsTable = null;
      currentRow = 0;
      offset = 0;
      firstPage = true;
      previousLineIsDecorated = true;
      firstDecoratedLine = true;
      displaySource();
    }
  }

  private void loadSources() {
    Sonar.getInstance().find(SourceQuery.create(resource.getId().toString())
        .setLinesFromLine(from, length)
        .setHighlightedSyntax(true), new AbstractCallback<Source>(loading) {

      @Override
      protected void doOnResponse(Source result) {
        source = result;
        displaySource();
      }

      @Override
      protected void doOnError(int errorCode, String errorMessage) {
        if (errorCode == 404) {
          panel.add(new HTML("<p style=\"padding: 5px\">No sources</p>"));
          hasNoSources = true;
          loading.removeFromParent();

        } else if (errorCode == 401) {
          panel.add(new HTML("<p style=\"padding: 5px\">You're not authorized to view source code</p>"));
          hasNoSources = true;
          loading.removeFromParent();

        } else {
          super.onError(errorCode, errorMessage);
        }
      }

    });
  }

  protected void setStarted() {
    started = true;
    displaySource();
  }

  private void displaySource() {
    if (started && source != null) {
      createRowsTable();

      int displayedLines = 0;
      SortedMap<Integer, String> lines = source.getLinesById().subMap(offset, offset + source.getLinesById().lastKey() + 1);
      Iterator<Map.Entry<Integer, String>> linesById = lines.entrySet().iterator();

      while (displayedLines < MAX_LINES_BY_BLOCK && linesById.hasNext()) {
        Map.Entry<Integer, String> entry = linesById.next();
        Integer lineIndex = entry.getKey();
        if (shouldDecorateLine(lineIndex)) {
          if (!previousLineIsDecorated && !firstDecoratedLine) {
            setRowHtml(0, "<div class='src' style='background-color: #fff;height: 3em; border-top: 1px dashed silver;border-bottom: 1px dashed silver;'> </div>");
            setRowHtml(1, " ");
            setRowHtml(2, " ");
            setRowHtml(3, "<div class='src' style='background-color: #fff;height: 3em; border-top: 1px dashed silver;border-bottom: 1px dashed silver;'> </div>");
            currentRow++;
          }

          List<Row> rows = decorateLine(lineIndex, entry.getValue());
          if (rows != null) {
            for (Row row : rows) {
              setRowHtml(0, row.getColumn1());
              setRowHtml(1, row.getColumn2());
              setRowHtml(2, row.getColumn3());
              setRowHtml(3, row.getColumn4());
              currentRow++;
            }
            previousLineIsDecorated = true;
            firstDecoratedLine = false;
          }
          displayedLines++;

        } else {
          previousLineIsDecorated = false;
        }
        offset++;
      }

      if (firstPage) {
        panel.clear();
        panel.add(rowsTable);
        firstPage = false;
      }

      if (offset <= source.getLinesById().lastKey()) {
        showMoreButton();
      } else {
        hideMoreButton();
      }
    }
  }

  private void setRowHtml(int colNum, String html) {
    if (firstPage) {
      rowsTable.setPendingHTML(currentRow, colNum, html);
    } else {
      rowsTable.setHTML(currentRow, colNum, html);
    }
  }

  private void createRowsTable() {
    if (rowsTable == null) {
      rowsTable = new PreloadedTable();
      rowsTable.setStyleName("sources code");

      offset = source.getLinesById().firstKey();
      if (shouldDecorateLine(0)) {
        List<Row> rows = decorateLine(0, null);
        if (rows != null) {
          for (Row row : rows) {
            rowsTable.setPendingHTML(currentRow, 0, row.getColumn1());
            rowsTable.setPendingHTML(currentRow, 1, row.getColumn2());
            rowsTable.setPendingHTML(currentRow, 2, row.getColumn3());
            rowsTable.setPendingHTML(currentRow, 3, row.getColumn4());
            currentRow++;
          }
        }
      }
    }
  }

  protected boolean shouldDecorateLine(int index) {
    return true;
  }

  protected List<Row> decorateLine(int index, String source) {
    if (index > 0) {
      return Arrays.asList(new Row(index, source));
    }
    return null;
  }

  public static class Row {
    protected String column1;
    protected String column2;
    protected String column3;
    protected String column4;

    public Row(String column1, String column2, String column3) {
      this.column1 = column1;
      this.column2 = column2;
      this.column3 = column3;
      this.column4 = "";
    }

    public Row(String column1, String column2, String column3, String column4) {
      this.column1 = column1;
      this.column2 = column2;
      this.column3 = column3;
      this.column4 = column4;
    }

    public Row(int lineIndex, String source) {
      setLineIndex(lineIndex, "");
      unsetValue();
      setSource(source, "");
    }

    public Row() {
    }

    public Row setLineIndex(int index, String style) {
      column1 = "<div class='ln " + style + "'>" + index + "</div>";
      return this;
    }

    public Row setValue(String value, String style) {
      column2 = "<div class='val " + style + "'>" + value + "</div>";
      return this;
    }

    public Row setValue2(String value, String style) {
      column3 = "<div class='val " + style + "'>" + value + "</div>";
      return this;
    }

    public Row unsetValue() {
      column2 = "";
      column3 = "";
      return this;
    }

    public Row setSource(String source, String style) {
      column4 = "<div class='src " + style + "'><pre>" + source + "</pre></div>";
      return this;
    }

    public String getColumn1() {
      return column1;
    }

    public void setColumn1(String column1) {
      this.column1 = column1;
    }

    public String getColumn2() {
      return column2;
    }

    public void setColumn2(String column2) {
      this.column2 = column2;
    }

    public String getColumn3() {
      return column3;
    }

    public void setColumn3(String column3) {
      this.column3 = column3;
    }

    public String getColumn4() {
      return column4;
    }

    public void setColumn4(String column4) {
      this.column4 = column4;
    }
  }
}
