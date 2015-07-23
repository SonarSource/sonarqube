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

import java.util.ArrayList;
import java.util.List;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issuable.IssueBuilder;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.index.BatchComponent;

public class DeprecatedIssueBuilderWrapper implements Issuable.IssueBuilder {

  private final DefaultIssue newIssue;
  private final BatchComponent primaryComponent;
  private TextRange primaryRange = null;
  private String primaryMessage = null;
  private List<NewIssueLocation> locations = new ArrayList<>();

  public DeprecatedIssueBuilderWrapper(BatchComponent primaryComponent, DefaultIssue newIssue) {
    this.primaryComponent = primaryComponent;
    this.newIssue = newIssue;
  }

  @Override
  public IssueBuilder ruleKey(RuleKey ruleKey) {
    newIssue.forRule(ruleKey);
    return this;
  }

  @Override
  public IssueBuilder line(Integer line) {
    if (primaryComponent.isFile()) {
      this.primaryRange = ((InputFile) primaryComponent.inputPath()).selectLine(line);
      return this;
    } else {
      throw new IllegalArgumentException("Unable to set line for issues on project or directory");
    }
  }

  @Override
  public IssueBuilder message(String message) {
    this.primaryMessage = message;
    return this;
  }

  @Override
  public NewIssueLocation newLocation() {
    return new DefaultIssueLocation();
  }

  @Override
  public IssueBuilder addLocation(NewIssueLocation location) {
    locations.add(location);
    return this;
  }

  @Override
  public IssueBuilder addExecutionFlow(NewIssueLocation... flow) {
    newIssue.addExecutionFlow(flow);
    return this;
  }

  @Override
  public IssueBuilder severity(String severity) {
    newIssue.overrideSeverity(Severity.valueOf(severity));
    return this;
  }

  @Override
  public IssueBuilder reporter(String reporter) {
    throw new UnsupportedOperationException("Not supported during sensor phase");
  }

  @Override
  public IssueBuilder effortToFix(Double d) {
    newIssue.effortToFix(d);
    return this;
  }

  @Override
  public IssueBuilder attribute(String key, String value) {
    throw new UnsupportedOperationException("Unused");
  }

  @Override
  public Issue build() {
    if (primaryMessage != null || primaryRange != null || locations.isEmpty()) {
      NewIssueLocation newLocation = newIssue.newLocation().message(primaryMessage);
      if (primaryComponent.isProjectOrModule()) {
        newLocation.onProject();
      } else if (primaryComponent.isFile()) {
        newLocation.onFile((InputFile) primaryComponent.inputPath());
        if (primaryRange != null) {
          newLocation.at(primaryRange);
        }
      } else if (primaryComponent.isDir()) {
        newLocation.onDir((InputDir) primaryComponent.inputPath());
      }
      newIssue.addLocation(newLocation);
    }
    for (NewIssueLocation issueLocation : locations) {
      newIssue.addLocation(issueLocation);
    }

    return new DeprecatedIssueWrapper(newIssue);
  }

}
