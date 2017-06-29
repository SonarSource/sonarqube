/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.scan.report;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.scanner.issue.tracking.TrackedIssue;

public class IssuesReport {

  public static final int TOO_MANY_ISSUES_THRESHOLD = 1000;
  private String title;
  private Date date;
  private boolean noFile;
  private final ReportSummary summary = new ReportSummary();
  private final Map<InputComponent, ResourceReport> resourceReportsByResource = Maps.newLinkedHashMap();

  public ReportSummary getSummary() {
    return summary;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public boolean isNoFile() {
    return noFile;
  }

  public void setNoFile(boolean noFile) {
    this.noFile = noFile;
  }

  public Map<InputComponent, ResourceReport> getResourceReportsByResource() {
    return resourceReportsByResource;
  }

  public List<ResourceReport> getResourceReports() {
    return new ArrayList<>(resourceReportsByResource.values());
  }

  public List<InputComponent> getResourcesWithReport() {
    return new ArrayList<>(resourceReportsByResource.keySet());
  }

  public void addIssueOnResource(InputComponent resource, TrackedIssue issue, Rule rule, RulePriority severity) {
    addResource(resource);
    getSummary().addIssue(issue, rule, severity);
    resourceReportsByResource.get(resource).addIssue(issue, rule, severity);
  }

  public void addResolvedIssueOnResource(InputComponent resource, Rule rule, RulePriority severity) {
    addResource(resource);
    getSummary().addResolvedIssue(rule, severity);
    resourceReportsByResource.get(resource).addResolvedIssue(rule, severity);
  }

  private void addResource(InputComponent componnet) {
    if (!resourceReportsByResource.containsKey(componnet)) {
      resourceReportsByResource.put(componnet, new ResourceReport(componnet));
    }
  }

}
