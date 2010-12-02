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
package org.sonar.plugins.core.violationsviewer.client;

import org.sonar.gwt.Links;
import org.sonar.gwt.Utils;
import org.sonar.gwt.ui.Icons;
import org.sonar.gwt.ui.SourcePanel;
import org.sonar.wsclient.gwt.AbstractListCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.Violation;
import org.sonar.wsclient.services.ViolationQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViolationsPanel extends SourcePanel {
  private boolean expand = false;
  private List<Violation> violations;
  private Map<Integer, List<Violation>> filteredViolationsByLine = new HashMap<Integer, List<Violation>>();

  public ViolationsPanel(Resource resource, String filter) {
    super(resource);
    loadViolations(resource, filter);
  }

  protected void loadViolations(final Resource resource, final String filter) {
    Sonar.getInstance().findAll(ViolationQuery.createForResource(resource), new AbstractListCallback<Violation>() {

      @Override
      protected void doOnResponse(List<Violation> violations) {
        ViolationsPanel.this.violations = violations;
        filter(filter);
        setStarted();
      }
    });
  }

  public boolean isExpand() {
    return expand;
  }

  public void setExpand(boolean expand) {
    this.expand = expand;
  }

  public void filter(String filter) {
    filteredViolationsByLine.clear();
    for (Violation violation : violations) {
      if (filter == null || filter.equals("") || violation.getRuleKey().equals(filter) || violation.getPriority().equals(filter)) {
        Integer line = 0;
        if (violation.getLine() != null) {
          line = violation.getLine();
        }
        List<Violation> lineViolations = filteredViolationsByLine.get(line);
        if (lineViolations == null) {
          lineViolations = new ArrayList<Violation>();
          filteredViolationsByLine.put(line, lineViolations);
        }
        lineViolations.add(violation);
      }
    }
  }

  @Override
  public boolean shouldDecorateLine(int index) {
    if (expand) {
      return true;
    }
    for (int i = index - 5; i < index + 5; i++) {
      if (hasViolations(i)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected List<Row> decorateLine(int index, String source) {
    List<Row> rows = new ArrayList<Row>();
    List<Violation> lineViolations = filteredViolationsByLine.get(index);
    boolean hasViolations = lineViolations != null && !lineViolations.isEmpty();

    if (index > 0) {
      String style = (hasViolations ? "red" : "");
      Row row = new Row().setLineIndex(index, style).unsetValue().setSource(source, style);
      rows.add(row);
    }

    if (hasViolations) {
      for (Violation violation : lineViolations) {
        rows.add(new ViolationRow(violation));
      }
    }
    return rows;
  }

  public static class ViolationRow extends Row {
    private Violation violation;

    public ViolationRow(Violation violation) {
      this.violation = violation;
    }

    @Override
    public String getColumn1() {
      return "<div class=\"bigln\">&nbsp;</div>";
    }

    @Override
    public String getColumn2() {
      return "";
    }

    @Override
    public String getColumn3() {
      return "";
    }

    @Override
    public String getColumn4() {
      return "<div class=\"warn\">" + Icons.forPriority(violation.getPriority()).getHTML() + "</img> "
          + Utils.formatDate(violation.getCreatedAt())
          + " <a href=\"" + Links.urlForRule(violation.getRuleKey(), false)
          + "\" onclick=\"window.open(this.href,'rule','height=800,width=900,scrollbars=1,resizable=1');return false;\" title=\""
          + violation.getRuleKey() + "\"><b>"
          + Utils.escapeHtml(violation.getRuleName()) + "</b></a> : "
          + Utils.escapeHtml(violation.getMessage()) + "</div>";
    }
  }

  private boolean hasViolations(int lineIndex) {
    if (lineIndex < 0) {
      return false;
    }
    List<Violation> list = filteredViolationsByLine.get(lineIndex);
    return list != null && !list.isEmpty();
  }
}
