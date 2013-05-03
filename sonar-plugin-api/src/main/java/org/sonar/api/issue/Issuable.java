/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.api.issue;

import org.sonar.api.component.Perspective;
import org.sonar.api.rule.RuleKey;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @since 3.6
 */
public interface Issuable extends Perspective {

  interface IssueBuilder {
    IssueBuilder ruleKey(RuleKey ruleKey);

    IssueBuilder line(@Nullable Integer line);

    IssueBuilder description(String description);

    IssueBuilder severity(String severity);

    IssueBuilder effortToFix(@Nullable Double d);

    IssueBuilder manual(boolean b);

    IssueBuilder attribute(String key, @Nullable String value);

    Issue build();
  }

  IssueBuilder newIssueBuilder();

  /**
   * @return true if the new issue is registered, false if the related rule does not exist or is disabled in the Quality profile
   */
  boolean addIssue(Issue issue);

  Collection<Issue> issues();
}
