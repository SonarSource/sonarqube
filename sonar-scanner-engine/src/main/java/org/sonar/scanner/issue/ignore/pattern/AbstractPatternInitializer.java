/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.issue.ignore.pattern;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;

import static com.google.common.base.MoreObjects.firstNonNull;

public abstract class AbstractPatternInitializer {
  private Configuration settings;
  private List<IssuePattern> multicriteriaPatterns;

  protected AbstractPatternInitializer(Configuration config) {
    this.settings = config;
    initPatterns();
  }

  protected Configuration getSettings() {
    return settings;
  }

  public List<IssuePattern> getMulticriteriaPatterns() {
    return multicriteriaPatterns;
  }

  public boolean hasConfiguredPatterns() {
    return hasMulticriteriaPatterns();
  }

  public boolean hasMulticriteriaPatterns() {
    return !multicriteriaPatterns.isEmpty();
  }

  @VisibleForTesting
  protected final void initPatterns() {
    // Patterns Multicriteria
    multicriteriaPatterns = new ArrayList<>();
    for (String id : settings.getStringArray(getMulticriteriaConfigurationKey())) {
      String propPrefix = getMulticriteriaConfigurationKey() + "." + id + ".";
      String filePathPattern = settings.get(propPrefix + "resourceKey").orElse(null);
      if (StringUtils.isBlank(filePathPattern)) {
        throw MessageException.of("Issue exclusions are misconfigured. File pattern is mandatory for each entry of '" + getMulticriteriaConfigurationKey() + "'");
      }
      String ruleKeyPattern = settings.get(propPrefix + "ruleKey").orElse(null);
      if (StringUtils.isBlank(ruleKeyPattern)) {
        throw MessageException.of("Issue exclusions are misconfigured. Rule key pattern is mandatory for each entry of '" + getMulticriteriaConfigurationKey() + "'");
      }
      IssuePattern pattern = new IssuePattern(firstNonNull(filePathPattern, "*"), firstNonNull(ruleKeyPattern, "*"));

      multicriteriaPatterns.add(pattern);
    }
  }

  protected abstract String getMulticriteriaConfigurationKey();
}
