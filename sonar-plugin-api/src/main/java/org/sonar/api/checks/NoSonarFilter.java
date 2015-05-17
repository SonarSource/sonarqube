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
package org.sonar.api.checks;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.api.resources.Resource;

import java.util.Map;
import java.util.Set;

/**
 * @since 2.1
 * @deprecated in 3.6. Replaced by {@link org.sonar.api.issue.NoSonarFilter}
 */
@Deprecated
public class NoSonarFilter implements org.sonar.api.issue.batch.IssueFilter {

  private final Map<String, Set<Integer>> noSonarLinesByKey = Maps.newHashMap();
  private SonarIndex sonarIndex;

  public NoSonarFilter(SonarIndex sonarIndex) {
    this.sonarIndex = sonarIndex;
  }

  public void addResource(Resource model, Set<Integer> noSonarLines) {
    if (model != null && noSonarLines != null) {
      // Reload resource to handle backward compatibility of resource keys
      Resource resource = sonarIndex.getResource(model);
      if (resource != null) {
        noSonarLinesByKey.put(resource.getEffectiveKey(), noSonarLines);
      }
    }
  }

  @Override
  public boolean accept(Issue issue, IssueFilterChain chain) {
    boolean accepted = true;
    if (issue.line() != null) {
      Set<Integer> noSonarLines = noSonarLinesByKey.get(issue.componentKey());
      accepted = noSonarLines == null || !noSonarLines.contains(issue.line());
      if (!accepted && StringUtils.containsIgnoreCase(issue.ruleKey().rule(), "nosonar")) {
        accepted = true;
      }
    }
    if (accepted) {
      accepted = chain.accept(issue);
    }
    return accepted;
  }
}
