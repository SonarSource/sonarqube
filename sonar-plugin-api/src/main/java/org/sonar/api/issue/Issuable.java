/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

package org.sonar.api.issue;

import org.sonar.api.component.Perspective;
import org.sonar.api.rule.RuleKey;

import java.util.Collection;

/**
 * @since 3.6
 */
public interface Issuable extends Perspective {

  interface IssueBuilder {
    IssueBuilder ruleKey(RuleKey ruleKey);

    IssueBuilder line(Integer line);

    IssueBuilder description(String description);

    IssueBuilder title(String title);

    IssueBuilder severity(String severity);

    IssueBuilder cost(Double cost);

    IssueBuilder manual(boolean b);

    Issue done();
  }

  IssueBuilder newIssue();

  Collection<Issue> issues();
}
