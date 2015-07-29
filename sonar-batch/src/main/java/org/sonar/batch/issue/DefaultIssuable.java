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
package org.sonar.batch.issue;

import java.util.Collections;
import java.util.List;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.batch.index.BatchComponent;

/**
 * @since 3.6
 */
public class DefaultIssuable implements Issuable {

  private final BatchComponent component;
  private final SensorContext sensorContext;

  DefaultIssuable(BatchComponent component, SensorContext sensorContext) {
    this.component = component;
    this.sensorContext = sensorContext;
  }

  @Override
  public IssueBuilder newIssueBuilder() {
    DefaultIssue newIssue = (DefaultIssue) sensorContext.newIssue();
    return new DeprecatedIssueBuilderWrapper(component.inputComponent(), newIssue);
  }

  @Override
  public boolean addIssue(Issue issue) {
    ((DeprecatedIssueWrapper) issue).wrapped().save();
    return true;
  }

  @Override
  public List<Issue> resolvedIssues() {
    return Collections.emptyList();
  }

  @Override
  public List<Issue> issues() {
    return Collections.emptyList();
  }

}
