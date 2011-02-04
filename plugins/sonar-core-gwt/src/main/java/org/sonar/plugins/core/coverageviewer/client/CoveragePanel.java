/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.core.coverageviewer.client;

import org.sonar.gwt.Metrics;
import org.sonar.gwt.ui.SourcePanel;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoveragePanel extends SourcePanel {

  private Map<Integer, String> hitsByLine = new HashMap<Integer, String>();
  private Map<Integer, String> branchHitsByLine = new HashMap<Integer, String>();


  public CoveragePanel(Resource resource) {
    super(resource);
    loadCoverageHits(resource);
  }

  private void loadCoverageHits(Resource resource) {
    ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.COVERAGE_LINE_HITS_DATA, Metrics.BRANCH_COVERAGE_HITS_DATA);
    Sonar.getInstance().find(query, new AbstractCallback<Resource>() {

      @Override
      protected void doOnResponse(Resource resource) {
        handleResponse(resource, Metrics.COVERAGE_LINE_HITS_DATA, hitsByLine);
        handleResponse(resource, Metrics.BRANCH_COVERAGE_HITS_DATA, branchHitsByLine);
        setStarted();
      }
    });
  }

  private void handleResponse(Resource resource, String metric, Map<Integer, String> values) {
    if (resource==null || resource.getMeasure(metric)==null) {
      return;
    }

    values.clear();
    String linesValue = resource.getMeasure(metric).getData();
    String[] lineWithValueArray;
    if (linesValue.contains(",")) {
      // deprecated - format before 1.9
      lineWithValueArray = linesValue.split(",");
    } else {
      lineWithValueArray = linesValue.split(";");
    }
    for (String lineWithValue : lineWithValueArray) {
      String[] elt = lineWithValue.split("=");
      if (elt != null && elt.length == 2) {
        values.put(Integer.parseInt(elt[0]), elt[1]);
      }
    }
  }


  @Override
  protected boolean shouldDecorateLine(int index) {
    return index > 0;
  }

  @Override
  protected List<Row> decorateLine(int index, String source) {
    Row row = new Row().setLineIndex(index, "");

    String hits = hitsByLine.get(index);
    String branchHits = branchHitsByLine.get(index);
    boolean hasLineCoverage = (null != hits);
    boolean hasBranchCoverage = (null != branchHits);
    boolean lineIsCovered = (hasLineCoverage && Integer.parseInt(hits) > 0);
    boolean branchIsCovered = (hasBranchCoverage && "100%".equals(branchHits));

    row.setSource(source, "");
    row.setValue("&nbsp;", "");
    row.setValue2("&nbsp;", "");

    if (lineIsCovered) {
      if (branchIsCovered) {
        row.setValue(hits, "green");
        row.setValue2(branchHits, "green");
      } else if (hasBranchCoverage) {
        row.setValue(hits, "orange");
        row.setValue2(branchHits, "orange");
        row.setSource(source, "orange");
      } else {
        row.setValue(hits, "green");
      }
    } else if (hasLineCoverage) {
      row.setValue(hits, "red");
      row.setSource(source, "red");
      if (hasBranchCoverage) {
        row.setValue2(branchHits, "red");
      } else {
        row.setValue2("&nbsp;", "red");
      }
    }
    return Arrays.asList(row);
  }
}
