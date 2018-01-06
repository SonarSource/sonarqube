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
package org.sonar.api.issue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;

/**
 * Issue filter used to ignore issues created on lines commented with the tag "NOSONAR".
 * <br>
 * Plugins, via {@link ScannerSide}s, must feed this filter by registering the
 * lines that contain "NOSONAR". Note that filters are disabled for the issues reported by
 * end-users from UI or web services.
 *
 * @since 3.6
 */
public class NoSonarFilter implements IssueFilter {

  private final Map<String, Set<Integer>> noSonarLinesByResource = new HashMap<>();

  /**
   * @deprecated since 5.0 use {@link #noSonarInFile(InputFile, Set)}
   */
  @Deprecated
  public NoSonarFilter addComponent(String componentKey, Set<Integer> noSonarLines) {
    noSonarLinesByResource.put(componentKey, noSonarLines);
    return this;
  }

  /**
   * Register lines in a file that contains the NOSONAR flag.
   * @param inputFile
   * @param noSonarLines Line number starts at 1 in a file
   * @since 5.0
   */
  public NoSonarFilter noSonarInFile(InputFile inputFile, Set<Integer> noSonarLines) {
    noSonarLinesByResource.put(((DefaultInputFile) inputFile).key(), noSonarLines);
    return this;
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    boolean accepted = true;
    if (issue.line() != null) {
      Set<Integer> noSonarLines = noSonarLinesByResource.get(issue.componentKey());
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
