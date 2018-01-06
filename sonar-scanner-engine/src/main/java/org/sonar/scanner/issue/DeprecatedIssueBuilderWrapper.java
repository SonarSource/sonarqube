/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.issue;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
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

public class DeprecatedIssueBuilderWrapper implements Issuable.IssueBuilder {

  private final DefaultIssue newIssue;
  private final InputComponent primaryComponent;
  private TextRange primaryRange = null;
  private String primaryMessage = null;

  public DeprecatedIssueBuilderWrapper(InputComponent primaryComponent, DefaultIssue newIssue) {
    this.primaryComponent = primaryComponent;
    this.newIssue = newIssue;
  }

  @Override
  public IssueBuilder ruleKey(RuleKey ruleKey) {
    newIssue.forRule(ruleKey);
    return this;
  }

  @Override
  public IssueBuilder line(@Nullable Integer line) {
    Preconditions.checkState(newIssue.primaryLocation() == null, "Do not use line() and at() for the same issue");
    if (primaryComponent.isFile()) {
      if (line != null) {
        this.primaryRange = ((InputFile) primaryComponent).selectLine(line.intValue());
      }
      return this;
    } else {
      throw new IllegalArgumentException("Unable to set line for issues on project or directory");
    }
  }

  @Override
  public IssueBuilder message(String message) {
    Preconditions.checkState(newIssue.primaryLocation() == null, "Do not use message() and at() for the same issue");
    this.primaryMessage = message;
    return this;
  }

  @Override
  public NewIssueLocation newLocation() {
    return new DefaultIssueLocation();
  }

  @Override
  public IssueBuilder at(NewIssueLocation primaryLocation) {
    Preconditions.checkState(primaryMessage == null && primaryRange == null, "Do not use message() or line() and at() for the same issue");
    newIssue.at(primaryLocation);
    return this;
  }

  @Override
  public IssueBuilder addLocation(NewIssueLocation secondaryLocation) {
    newIssue.addLocation(secondaryLocation);
    return this;
  }

  @Override
  public IssueBuilder addFlow(Iterable<NewIssueLocation> flowLocations) {
    newIssue.addFlow(flowLocations);
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
    throw new UnsupportedOperationException("Not supported during sensor phase");
  }

  @Override
  public Issue build() {
    if (newIssue.primaryLocation() == null) {
      NewIssueLocation newLocation = newIssue.newLocation().on(primaryComponent);
      if (primaryMessage != null) {
        newLocation.message(primaryMessage);
      }
      if (primaryComponent.isFile() && primaryRange != null) {
        newLocation.at(primaryRange);
      }
      newIssue.at(newLocation);
    }
    return new DeprecatedIssueWrapper(newIssue);
  }

}
