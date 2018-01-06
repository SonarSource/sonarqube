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
import org.sonar.api.scan.issue.filter.IssueFilterChain;

import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.issue.filter.IssueFilter;

/**
 * This filter removes the issues that are on line < 5
 * <p/>
 * Issue filters have been introduced in 3.6.
 */
public class IssueFilterBeforeLine5 implements IssueFilter {

  private final Settings settings;

  public IssueFilterBeforeLine5(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    if (issue.componentKey() == null) {
      throw new IllegalStateException("Issue component is not set");
    }
    if (issue.ruleKey() == null) {
      throw new IllegalStateException("Issue rule is not set");
    }

    boolean b = !settings.getBoolean("enableIssueFilters") || issue.line() == null || issue.line() >= 5;
    if (!b) {
      return false;
    }

    return chain.accept(issue);
  }
}
