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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
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

import static java.lang.String.format;

public class DefaultIssue extends DefaultStorable implements Issue, NewIssue {

  private static final class ToFlow implements Function<List<IssueLocation>, Flow> {
    @Override
    public Flow apply(final List<IssueLocation> input) {
      return new Flow() {
        @Override
        public List<IssueLocation> locations() {
          return ImmutableList.copyOf(input);
        }
      };
    }
  }

  private RuleKey ruleKey;
  private Double effortToFix;
  private Severity overriddenSeverity;
  private IssueLocation primaryLocation;
  private List<List<IssueLocation>> flows = new ArrayList<>();

  public DefaultIssue() {
    super(null);
  }

  public DefaultIssue(SensorStorage storage) {
    super(storage);
  }

  @Override
  public DefaultIssue forRule(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public DefaultIssue effortToFix(@Nullable Double effortToFix) {
    Preconditions.checkArgument(effortToFix == null || effortToFix >= 0, format("Effort to fix must be greater than or equal 0 (got %s)", effortToFix));
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
  public DefaultIssue at(NewIssueLocation primaryLocation) {
    Preconditions.checkArgument(primaryLocation != null, "Cannot use a location that is null");
    Preconditions.checkState(this.primaryLocation == null, "at() already called");
    this.primaryLocation = (DefaultIssueLocation) primaryLocation;
    return this;
  }

  @Override
  public NewIssue addLocation(NewIssueLocation secondaryLocation) {
    flows.add(Arrays.asList((IssueLocation) secondaryLocation));
    return this;
  }

  @Override
  public DefaultIssue addFlow(Iterable<NewIssueLocation> locations) {
    List<IssueLocation> flowAsList = new ArrayList<>();
    for (NewIssueLocation issueLocation : locations) {
      flowAsList.add((DefaultIssueLocation) issueLocation);
    }
    flows.add(flowAsList);
    return this;
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

  @Override
  public IssueLocation primaryLocation() {
    return primaryLocation;
  }

  @Override
  public List<Flow> flows() {
    return Lists.transform(this.flows, new ToFlow());
  }

  @Override
  public void doSave() {
    Preconditions.checkNotNull(this.ruleKey, "ruleKey is mandatory on issue");
    Preconditions.checkState(primaryLocation != null, "Primary location is mandatory on every issue");
    storage.store(this);
  }

}
