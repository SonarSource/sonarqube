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
package org.sonar.plugins.jacoco.itcoverage.viewer.client;

import org.sonar.gwt.ui.SourcePanel;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copied from org.sonar.plugins.core.coverageviewer.client.CoveragePanel
 */
public class CoveragePanel extends SourcePanel {

  private Map<Integer, Integer> hitsByLine = new HashMap<Integer, Integer>();
  private Map<Integer, Integer> conditionsByLine = new HashMap<Integer, Integer>();
  private Map<Integer, Integer> coveredConditionsByLine = new HashMap<Integer, Integer>();
  private Map<Integer, String> branchCoverageByLine = new HashMap<Integer, String>();

  public CoveragePanel(Resource resource) {
    super(resource);
    loadCoverageHits(resource);
  }

  private void loadCoverageHits(Resource resource) {
    ResourceQuery query = ResourceQuery.createForResource(resource, Metrics.IT_COVERAGE_LINE_HITS_DATA, Metrics.IT_BRANCH_COVERAGE_HITS_DATA, Metrics.IT_CONDITIONS_BY_LINE, Metrics.IT_COVERED_CONDITIONS_BY_LINE);
    Sonar.getInstance().find(query, new AbstractCallback<Resource>() {

      @Override
      protected void doOnResponse(Resource resource) {
        handleLineHits(resource);
        handleLineConditions(resource);
        handleDeprecatedBranchCoverage(resource);
        setStarted();
      }
    });
  }

  private void handleLineHits(Resource resource) {
    parseDataMap(resource, Metrics.IT_COVERAGE_LINE_HITS_DATA, hitsByLine);
  }

  private void handleLineConditions(Resource resource) {
    parseDataMap(resource, Metrics.IT_CONDITIONS_BY_LINE, conditionsByLine);
    parseDataMap(resource, Metrics.IT_COVERED_CONDITIONS_BY_LINE, coveredConditionsByLine);
  }

  private void parseDataMap(Resource resource, String metric, Map<Integer, Integer> map) {
    if (resource == null || resource.getMeasure(metric) == null) {
      return;
    }

    map.clear();
    String data = resource.getMeasure(metric).getData();
    for (String lineWithValue : data.split(";")) {
      String[] elt = lineWithValue.split("=");
      if (elt != null && elt.length == 2) {
        map.put(Integer.parseInt(elt[0]), Integer.parseInt(elt[1]));
      }
    }
  }

  private void handleDeprecatedBranchCoverage(Resource resource) {
    if (resource == null || resource.getMeasure(Metrics.IT_BRANCH_COVERAGE_HITS_DATA) == null) {
      return;
    }

    branchCoverageByLine.clear();
    String data = resource.getMeasure(Metrics.IT_BRANCH_COVERAGE_HITS_DATA).getData();
    for (String lineWithValue : data.split(";")) {
      String[] elt = lineWithValue.split("=");
      if (elt != null && elt.length == 2) {
        branchCoverageByLine.put(Integer.parseInt(elt[0]), elt[1]);
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

    Integer hits = hitsByLine.get(index);
    Integer conditions = conditionsByLine.get(index);
    Integer coveredConditions = coveredConditionsByLine.get(index);
    String branchCoverage = branchCoverageByLine.get(index);
    if (branchCoverage == null && conditions != null && coveredConditions != null) {
      branchCoverage = String.valueOf(conditions - coveredConditions) + "/" + String.valueOf(conditions);
    }

    boolean hasLineCoverage = (hits != null);
    boolean hasBranchCoverage = (branchCoverage != null);
    boolean lineIsCovered = (hasLineCoverage && hits > 0);
    boolean branchIsCovered = ("100%".equals(branchCoverage) || (conditions != null && coveredConditions != null && coveredConditions == conditions));

    row.setSource(source, "");
    row.setValue("&nbsp;", "");
    row.setValue2("&nbsp;", "");

    if (lineIsCovered) {
      if (branchIsCovered) {
        row.setValue(String.valueOf(hits), "green");
        row.setValue2(branchCoverage, "green");

      } else if (hasBranchCoverage) {
        row.setValue(String.valueOf(hits), "orange");
        row.setValue2(branchCoverage, "orange");
        row.setSource(source, "orange");
      } else {
        row.setValue(String.valueOf(hits), "green");
      }
    } else if (hasLineCoverage) {
      row.setValue(String.valueOf(hits), "red");
      row.setSource(source, "red");
      if (hasBranchCoverage) {
        row.setValue2(branchCoverage, "red");
      } else {
        row.setValue2("&nbsp;", "red");
      }
    }
    return Arrays.asList(row);
  }
}
