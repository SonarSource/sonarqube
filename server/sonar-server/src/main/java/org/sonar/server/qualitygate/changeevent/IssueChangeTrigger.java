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
package org.sonar.server.qualitygate.changeevent;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.issue.ws.SearchResponseData;

import static com.google.common.base.Preconditions.checkArgument;

public interface IssueChangeTrigger {
  /**
   * Will call webhooks once for any short living branch which has at least one issue in {@link SearchResponseData} and
   * if change described in {@link IssueChange} can alter the status of the short living branch.
   */
  void onChange(IssueChangeData issueChangeData, IssueChange issueChange, IssueChangeContext context);

  final class IssueChange {
    private final RuleType ruleType;
    private final String transitionKey;

    public IssueChange(RuleType ruleType) {
      this(ruleType, null);
    }

    public IssueChange(String transitionKey) {
      this(null, transitionKey);
    }

    public IssueChange(@Nullable RuleType ruleType, @Nullable String transitionKey) {
      checkArgument(ruleType != null || transitionKey != null, "At least one of ruleType and transitionKey must be non null");
      this.ruleType = ruleType;
      this.transitionKey = transitionKey;
    }

    public Optional<RuleType> getRuleType() {
      return Optional.ofNullable(ruleType);
    }

    public Optional<String> getTransitionKey() {
      return Optional.ofNullable(transitionKey);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      IssueChange that = (IssueChange) o;
      return ruleType == that.ruleType &&
        Objects.equals(transitionKey, that.transitionKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(ruleType, transitionKey);
    }

    @Override
    public String toString() {
      return "IssueChange{" +
        "ruleType=" + ruleType +
        ", transitionKey='" + transitionKey + '\'' +
        '}';
    }
  }

  final class IssueChangeData {
    private final List<DefaultIssue> issues;
    private final List<ComponentDto> components;

    public IssueChangeData(List<DefaultIssue> issues, List<ComponentDto> components) {
      this.issues = ImmutableList.copyOf(issues);
      this.components = ImmutableList.copyOf(components);
    }

    public List<DefaultIssue> getIssues() {
      return issues;
    }

    public List<ComponentDto> getComponents() {
      return components;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      IssueChangeData that = (IssueChangeData) o;
      return Objects.equals(issues, that.issues) &&
        Objects.equals(components, that.components);
    }

    @Override
    public int hashCode() {
      return Objects.hash(issues, components);
    }

    @Override
    public String toString() {
      return "IssueChangeData{" +
        "issues=" + issues +
        ", components=" + components +
        '}';
    }
  }
}
