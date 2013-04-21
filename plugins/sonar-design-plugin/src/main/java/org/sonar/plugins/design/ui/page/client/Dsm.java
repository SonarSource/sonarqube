/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.design.ui.page.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.sonar.gwt.Links;
import org.sonar.gwt.ui.Icons;

import java.util.LinkedList;
import java.util.List;

public class Dsm extends Composite {

  /* STYLES */
  public static final String DSM = "dsm";

  public static final String HEADER = "htable";
  public static final String HEADER_TITLE = "ht";
  public static final String HEADER_SELECTED_SUFFIX = "s";
  public static final String HEADER_INDICATOR = "hi";
  public static final String HEADER_HIGHER_INDICATOR_SUFFIX = "h";
  public static final String HEADER_LOWER_INDICATOR_SUFFIX = "l";

  public static final String GRID = "gtable";
  public static final String GRID_CELL_BOTTOM_LEFT = "cbl";
  public static final String GRID_CELL_TOP_RIGHT = "ctr";
  public static final String GRID_CELL_DIAGONAL = "cd";
  public static final String GRID_CELL_SELECTION1_SUFFIX = "s1";
  public static final String GRID_CELL_SELECTION2_SUFFIX = "s2";
  public static final String GRID_CELL_COMB1_SUFFIX = "c1";
  public static final String GRID_CELL_COMB2_SUFFIX = "c2";
  public static final String[] GRID_SUFFIXES = {GRID_CELL_SELECTION1_SUFFIX, GRID_CELL_SELECTION2_SUFFIX, GRID_CELL_COMB1_SUFFIX, GRID_CELL_COMB2_SUFFIX};


  private VerticalPanel dsm = new VerticalPanel();
  private DsmData.Rows data;
  private Label[][] cells;
  private Label[] titles;
  private Label[] indicators;
  private List<Label> highlightedCells = new LinkedList<Label>();

  public Dsm() {
    dsm.setStylePrimaryName(DSM);
    initWidget(dsm);
  }

  private Widget createLegend() {
    Dictionary l10n = Dictionary.getDictionary("l10n");
    HorizontalPanel legend = new HorizontalPanel();
    legend.getElement().setId("dsmlegend");
    legend.add(new HTML("<div class='square gray'> </div>"));
    legend.add(new Label(l10n.get("design.legend.dependencies")));
    legend.add(new HTML("<div class='space'></div>"));
    legend.add(new HTML("<div class='square red'> </div> "));
    legend.add(new Label(l10n.get("design.legend.cycles")));
    legend.add(new HTML(" <div class='space'></div> "));
    legend.add(new HTML("<div class='square green'></div> "));
    legend.add(new Label(l10n.get("design.legend.uses")));
    legend.add(new HTML("<div class='square blue'></div> "));
    legend.add(new Label(l10n.get("design.legend.uses")));
    legend.add(new HTML(" <div class='square yellow'></div>"));
    return legend;
  }

  public void displayNoData() {
    dsm.clear();
    dsm.add(new Label(Dictionary.getDictionary("l10n").get("noData")));
  }

  public void display(DsmData.Rows data) {
    if (data == null) {
      displayNoData();
      
    } else {
      this.data = data;
      dsm.clear();
      dsm.add(createHelp());
      dsm.add(createLegend());
      HorizontalPanel matrix = new HorizontalPanel();
      matrix.add(createRowHeader());
      matrix.add(createGrid());
      dsm.add(matrix);
    }
  }

  private Widget createHelp() {
    HorizontalPanel help = new HorizontalPanel();
    help.getElement().setId("dsmhelp");
    Dictionary l10n = Dictionary.getDictionary("l10n");
    Anchor link = new Anchor(l10n.get("design.help"), "http://docs.codehaus.org/x/QQFhC", "docsonar");
    help.add(Icons.get().help().createImage());
    help.add(link);
    return help;
  }

  private Grid createRowHeader() {
    Grid header = new Grid(data.size(), 2);
    header.setCellPadding(0);
    header.setCellSpacing(0);
    header.setStylePrimaryName(HEADER);

    titles = new Label[data.size()];
    indicators = new Label[data.size()];
    for (int indexRow = 0; indexRow < data.size(); indexRow++) {
      DsmData.Row row = data.get(indexRow);

      HTML title = buildRowTitle(indexRow, row);
      titles[indexRow] = title;
      header.setWidget(indexRow, 0, title);

      Label indicator = buildLabel("", HEADER_INDICATOR);
      header.setWidget(indexRow, 1, indicator);
      indicators[indexRow] = indicator;
    }
    return header;
  }

  private Grid createGrid() {
    int rowsCount = data.size();
    Grid grid = new Grid(rowsCount, rowsCount);
    grid.setCellPadding(0);
    grid.setCellSpacing(0);
    grid.setStylePrimaryName(GRID);

    return loadGridCells(grid, data);
  }

  private Grid loadGridCells(Grid grid, DsmData.Rows data) {
    int size = data.size();
    cells = new Label[size][size];
    for (int row = 0; row < size; row++) {
      DsmData.Row resource = data.get(row);
      for (int col = 0; col < resource.size(); col++) {
        Label cell = createGridCell(row, col, resource.getWeight(col));
        grid.setWidget(row, col, cell);
        cells[row][col] = cell;
      }
    }
    return grid;
  }


  /* ---------------- ACTIONS -------------------- */

  public void onCellClicked(int row, int col) {
    cancelHighlighting();

    highlightTitle(row);
    highlightTitle(col);
    highlightIndicator(row);
    highlightIndicator(col);

    for (int i = 0; i < cells.length; i++) {
      for (int j = 0; j < cells.length; j++) {
        Label cell = cells[i][j];
        if (i == row && j == col) {
          highlightCell(cell, GRID_CELL_SELECTION1_SUFFIX);

        } else if (j == row && i == col) {
          // opposite
          highlightCell(cell, GRID_CELL_SELECTION1_SUFFIX);

        } else if (j == col || i == col) {
          highlightCell(cell, GRID_CELL_COMB1_SUFFIX);

        } else if (i == row || j == row) {
          highlightCell(cell, GRID_CELL_COMB2_SUFFIX);
        }
      }
    }
  }

  private void displayDependencyInfo(int row, int col) {
    DsmData.Cell cell = data.get(row).getCell(col);
    DependencyInfo.getInstance().showOrPopup(cell.getDependencyId());
  }

  public void onTitleClicked(int row) {
    cancelHighlighting();
    highlightTitle(row);
    highlightIndicator(row);

    // highlight row
    for (int col = 0; col < cells[row].length; col++) {
      highlightCell(cells[row][col], GRID_CELL_SELECTION2_SUFFIX);
      if (col < row && hasWeight(cells[row][col])) {
        highlightIndicator(col, true);
      }
    }

    // highlight column
    for (int i = 0; i < cells.length; i++) {
      if (i != row) {
        highlightCell(cells[i][row], GRID_CELL_SELECTION2_SUFFIX);
        if (i > row && hasWeight(cells[i][row])) {
          highlightIndicator(i, false);
        }
      }
    }
  }

  private boolean hasWeight(Label label) {
    return label.getText().length() > 0;
  }


  /*--------- EFFECTS ----------*/
  private void cancelHighlighting() {
    cancelGridHighlighting();
    cancelIndicatorsHighlighting();
    cancelTitlesHighlighting();
  }

  private void cancelGridHighlighting() {
    for (Label cell : highlightedCells) {
      for (String suffix : GRID_SUFFIXES) {
        cell.removeStyleDependentName(suffix);
      }
    }
    highlightedCells.clear();
  }

  private void highlightCell(Label cell, String style) {
    cell.addStyleDependentName(style);
    highlightedCells.add(cell);
  }

  private void highlightTitle(int row) {
    titles[row].addStyleDependentName(HEADER_SELECTED_SUFFIX);
  }

  private void cancelTitlesHighlighting() {
    for (Label title : titles) {
      title.removeStyleDependentName(HEADER_SELECTED_SUFFIX);
    }
  }

  private void cancelIndicatorsHighlighting() {
    for (Label indicator : indicators) {
      indicator.removeStyleDependentName(HEADER_HIGHER_INDICATOR_SUFFIX);
      indicator.removeStyleDependentName(HEADER_LOWER_INDICATOR_SUFFIX);
      indicator.removeStyleDependentName(HEADER_SELECTED_SUFFIX);
    }
  }

  private void highlightIndicator(int row) {
    indicators[row].addStyleDependentName(HEADER_SELECTED_SUFFIX);
  }

  private void highlightIndicator(int row, boolean higher) {
    indicators[row].addStyleDependentName(higher ? HEADER_HIGHER_INDICATOR_SUFFIX : HEADER_LOWER_INDICATOR_SUFFIX);
  }


  /* ---------- COMPONENTS ------------ */
  private Label createGridCell(final int row, final int col, final int weight) {
    Label cell;
    if (row == col) {
      cell = createDiagonalCell(row);

    } else {
      cell = createNonDiagonalCell(row, col, weight);
    }
    return cell;
  }

  private Label createNonDiagonalCell(final int row, final int col, int weight) {
    Label cell;
    cell = buildCell(row, col, weight, (col > row ? GRID_CELL_TOP_RIGHT : GRID_CELL_BOTTOM_LEFT));

    if (weight > 0) {
      String tooltip = data.get(col).getName() + " -> " + data.get(row).getName() + " (" + weight + "). " + Dictionary.getDictionary("l10n").get("design.cellTooltip");
      cell.setTitle(tooltip);
    }
    return cell;
  }

  private Label createDiagonalCell(final int row) {
    Label cell;
    cell = buildLabel("-", GRID_CELL_DIAGONAL);
    cell.addClickHandler(new ClickHandler() {
      public void onClick(final ClickEvent event) {
        onTitleClicked(row);
      }
    });
    return cell;
  }

  private HTML buildRowTitle(final int indexRow, final DsmData.Row row) {
    HTML title = new HTML(Icons.forQualifier(row.getQualifier()).getHTML() + " " + row.getName()) {
      {
        addDomHandler(new DoubleClickHandler() {
          public void onDoubleClick(DoubleClickEvent pEvent) {
            if (row.getId() != null) {
              if (!"FIL".equals(row.getQualifier()) && !"CLA".equals(row.getQualifier())) {
                Window.Location.assign(Links.urlForResourcePage(row.getId(), DesignPage.GWT_ID, null));
              } else {
                Links.openMeasurePopup(row.getId(), null);
              }
            }
          }
        }, DoubleClickEvent.getType());
      }
    };
    title.setStylePrimaryName(HEADER_TITLE);
    title.setTitle(Dictionary.getDictionary("l10n").get("design.rowTooltip"));
    final int finalIndexRow = indexRow;
    title.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        onTitleClicked(finalIndexRow);
      }
    });
    return title;
  }

  private static Label buildLabel(String text, String primaryStyle) {
    Label label = new Label(text);
    label.setStylePrimaryName(primaryStyle);
    return label;
  }

  private Label buildCell(final int row, final int col, int weight, String primaryStyle) {
    String text = "";
    if (weight > 0) {
      text = "<span>" + Integer.toString(weight) + "</span>";
    }

    HTML cell = new HTML(text) {
      {
        addDomHandler(new DoubleClickHandler() {
          public void onDoubleClick(DoubleClickEvent pEvent) {
            displayDependencyInfo(row, col);
          }
        }, DoubleClickEvent.getType());
      }
    };
    cell.addClickHandler(new ClickHandler() {
      public void onClick(final ClickEvent event) {
        onCellClicked(row, col);
      }
    });
    cell.setStylePrimaryName(primaryStyle);
    return cell;
  }

}
