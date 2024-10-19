/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue.filter;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.core.issue.DefaultIssue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.core.config.IssueExclusionProperties.PATTERNS_MULTICRITERIA_EXCLUSION_KEY;
import static org.sonar.core.config.IssueExclusionProperties.PATTERNS_MULTICRITERIA_INCLUSION_KEY;
import static org.sonar.core.config.IssueExclusionProperties.RESOURCE_KEY;
import static org.sonar.core.config.IssueExclusionProperties.RULE_KEY;

@ComputeEngineSide
public class IssueFilter {

  private static final Logger LOG = LoggerFactory.getLogger(IssueFilter.class);

  private final List<IssuePattern> exclusionPatterns;
  private final List<IssuePattern> inclusionPatterns;

  public IssueFilter(ConfigurationRepository configRepository) {
    Configuration config = configRepository.getConfiguration();
    this.exclusionPatterns = loadPatterns(PATTERNS_MULTICRITERIA_EXCLUSION_KEY, config);
    this.inclusionPatterns = loadPatterns(PATTERNS_MULTICRITERIA_INCLUSION_KEY, config);
  }

  public boolean accept(DefaultIssue issue, Component component) {
    if (component.getType() != FILE || (exclusionPatterns.isEmpty() && inclusionPatterns.isEmpty())) {
      return true;
    }
    if (isExclude(issue, component)) {
      return false;
    }
    return isInclude(issue, component);
  }

  private boolean isExclude(DefaultIssue issue, Component file) {
    IssuePattern matchingPattern = null;
    Iterator<IssuePattern> patternIterator = exclusionPatterns.iterator();
    while (matchingPattern == null && patternIterator.hasNext()) {
      IssuePattern nextPattern = patternIterator.next();
      if (nextPattern.match(issue, file)) {
        matchingPattern = nextPattern;
      }
    }
    if (matchingPattern != null) {
      LOG.debug("Issue {} ignored by exclusion pattern {}", issue, matchingPattern);
      return true;
    }
    return false;
  }

  private boolean isInclude(DefaultIssue issue, Component file) {
    boolean atLeastOneRuleMatched = false;
    boolean atLeastOnePatternFullyMatched = false;
    IssuePattern matchingPattern = null;

    for (IssuePattern pattern : inclusionPatterns) {
      if (pattern.getRulePattern().match(issue.ruleKey().toString())) {
        atLeastOneRuleMatched = true;
        String filePath = file.getName();
        if (filePath != null && pattern.getComponentPattern().match(filePath)) {
          atLeastOnePatternFullyMatched = true;
          matchingPattern = pattern;
        }
      }
    }

    if (atLeastOneRuleMatched) {
      if (atLeastOnePatternFullyMatched) {
        LOG.debug("Issue {} enforced by pattern {}", issue, matchingPattern);
      }
      return atLeastOnePatternFullyMatched;
    } else {
      return true;
    }
  }

  private static List<IssuePattern> loadPatterns(String propertyKey, Configuration settings) {
    List<IssuePattern> patterns = new ArrayList<>();
    String patternConf = settings.get(propertyKey).orElse("");
    for (String id : Splitter.on(",").omitEmptyStrings().split(patternConf)) {
      String propPrefix = propertyKey + "." + id + ".";
      String componentPathPattern = settings.get(propPrefix + RESOURCE_KEY).orElse(null);
      checkArgument(!isNullOrEmpty(componentPathPattern), format("File path pattern cannot be empty. Please check '%s' settings", propertyKey));
      String ruleKeyPattern = settings.get(propPrefix + RULE_KEY).orElse(null);
      checkArgument(!isNullOrEmpty(ruleKeyPattern), format("Rule key pattern cannot be empty. Please check '%s' settings", propertyKey));
      patterns.add(new IssuePattern(componentPathPattern, ruleKeyPattern));
    }
    return patterns;
  }

}
