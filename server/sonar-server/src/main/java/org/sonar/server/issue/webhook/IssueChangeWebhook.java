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
package org.sonar.server.issue.webhook;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.ws.SearchResponseData;

import static com.google.common.base.Preconditions.checkArgument;

public interface IssueChangeWebhook {
  /**
   * Will call webhooks once for any short living branch which has at least one issue in {@link SearchResponseData} and
   * if change described in {@link IssueChange} can alter the status of the short living branch.
   */
  void onChange(SearchResponseData searchResponseData, IssueChange issueChange, IssueChangeContext context);

  final class IssueChange {
    private final RuleType ruleType;
    private final String transitionKey;

    public IssueChange(RuleType ruleType) {
      this(ruleType, null);
    }

    public IssueChange(String transitionKey) {
      this(null, transitionKey);
    }

    IssueChange(@Nullable RuleType ruleType, @Nullable String transitionKey) {
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
  }
}
