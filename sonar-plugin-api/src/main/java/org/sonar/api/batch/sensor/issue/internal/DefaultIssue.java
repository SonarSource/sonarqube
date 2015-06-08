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
package org.sonar.api.batch.sensor.issue.internal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.internal.DefaultStorable;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.internal.Uuids;

public class DefaultIssue extends DefaultStorable implements Issue, NewIssue {

  private String key;
  private RuleKey ruleKey;
  private Double effortToFix;
  private Severity overriddenSeverity;
  private List<IssueLocation> locations = new ArrayList<>();
  private List<List<IssueLocation>> executionFlows = new ArrayList<>();

  public DefaultIssue() {
    super(null);
    this.key = Uuids.create();
  }

  public DefaultIssue(SensorStorage storage) {
    super(storage);
    this.key = Uuids.create();
  }

  @Override
  public DefaultIssue forRule(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public DefaultIssue effortToFix(@Nullable Double effortToFix) {
    this.effortToFix = effortToFix;
    return this;
  }

  @Override
  public DefaultIssue overrideSeverity(@Nullable Severity severity) {
    this.overriddenSeverity = severity;
    return this;
  }

  @Override
  public NewIssueLocation newLocation() {
    return new DefaultIssueLocation();
  }

  @Override
  public DefaultIssue addLocation(NewIssueLocation location) {
    locations.add((DefaultIssueLocation) location);
    return this;
  }

  @Override
  public DefaultIssue addExecutionFlow(NewIssueLocation... flow) {
    List<IssueLocation> flowAsList = new ArrayList<>();
    for (NewIssueLocation issueLocation : flow) {
      flowAsList.add((DefaultIssueLocation) issueLocation);
    }
    executionFlows.add(flowAsList);
    return null;
  }

  @Override
  public RuleKey ruleKey() {
    return this.ruleKey;
  }

  @Override
  public Severity overriddenSeverity() {
    return this.overriddenSeverity;
  }

  @Override
  public Double effortToFix() {
    return this.effortToFix;
  }

  public String key() {
    return this.key;
  }

  @Override
  public List<IssueLocation> locations() {
    return ImmutableList.copyOf(this.locations);
  }

  @Override
  public List<ExecutionFlow> executionFlows() {
    return Lists.transform(this.executionFlows, new Function<List<IssueLocation>, ExecutionFlow>() {
      @Override
      public ExecutionFlow apply(final List<IssueLocation> input) {
        return new ExecutionFlow() {
          @Override
          public List<IssueLocation> locations() {
            return ImmutableList.copyOf(input);
          }
        };
      }
    });
  }

  @Override
  public void doSave() {
    Preconditions.checkNotNull(this.ruleKey, "ruleKey is mandatory on issue");
    Preconditions.checkState(!Strings.isNullOrEmpty(key), "Fail to generate issue key");
    Preconditions.checkState(!locations.isEmpty(), "At least one location is mandatory on every issue");
    storage.store(this);
  }

  /**
   * For testing only.
   */
  public DefaultIssue withKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultIssue that = (DefaultIssue) o;
    return !key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

}
