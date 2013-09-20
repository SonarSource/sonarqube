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

package org.sonar.plugins.core.issue.ignore.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.plugins.core.issue.ignore.IgnoreIssuesConfiguration;

import java.util.List;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;

public class PatternsInitializer implements BatchExtension {

  private final Settings settings;

  private List<IssuePattern> multicriteriaPatterns;
  private List<IssuePattern> blockPatterns;
  private List<IssuePattern> allFilePatterns;

  public PatternsInitializer(Settings settings) {
    this.settings = settings;
    initPatterns();
  }

  public List<IssuePattern> getMulticriteriaPatterns() {
    return multicriteriaPatterns;
  }

  public List<IssuePattern> getBlockPatterns() {
    return blockPatterns;
  }

  public List<IssuePattern> getAllFilePatterns() {
    return allFilePatterns;
  }

  public boolean hasFileContentPattern() {
    return ! (blockPatterns.isEmpty() && allFilePatterns.isEmpty());
  }

  public boolean hasMulticriteriaPatterns() {
    return ! multicriteriaPatterns.isEmpty();
  }

  public boolean hasConfiguredPatterns() {
    return hasFileContentPattern() || hasMulticriteriaPatterns();
  }

  @VisibleForTesting
  protected final void initPatterns() {
    multicriteriaPatterns = Lists.newArrayList();
    blockPatterns = Lists.newArrayList();
    allFilePatterns = Lists.newArrayList();

    loadPatternsFromNewProperties();
  }

  private void loadPatternsFromNewProperties() {
    // Patterns Multicriteria
    String patternConf = StringUtils.defaultIfBlank(settings.getString(IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = IgnoreIssuesConfiguration.PATTERNS_MULTICRITERIA_KEY + "." + id + ".";
      String resourceKeyPattern = settings.getString(propPrefix + IgnoreIssuesConfiguration.RESOURCE_KEY);
      String ruleKeyPattern = settings.getString(propPrefix + IgnoreIssuesConfiguration.RULE_KEY);
      String lineRange = settings.getString(propPrefix + IgnoreIssuesConfiguration.LINE_RANGE_KEY);
      String[] fields = new String[] { resourceKeyPattern, ruleKeyPattern, lineRange };
      PatternDecoder.checkRegularLineConstraints(StringUtils.join(fields, ","), fields);
      IssuePattern pattern = new IssuePattern(firstNonNull(resourceKeyPattern, "*"), firstNonNull(ruleKeyPattern, "*"));
      PatternDecoder.decodeRangeOfLines(pattern, firstNonNull(lineRange, "*"));
      multicriteriaPatterns.add(pattern);
    }

    // Patterns Block
    patternConf = StringUtils.defaultIfBlank(settings.getString(IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = IgnoreIssuesConfiguration.PATTERNS_BLOCK_KEY + "." + id + ".";
      String beginBlockRegexp = settings.getString(propPrefix + IgnoreIssuesConfiguration.BEGIN_BLOCK_REGEXP);
      String endBlockRegexp = settings.getString(propPrefix + IgnoreIssuesConfiguration.END_BLOCK_REGEXP);
      String[] fields = new String[] { beginBlockRegexp, endBlockRegexp };
      PatternDecoder.checkDoubleRegexpLineConstraints(StringUtils.join(fields, ","), fields);
      IssuePattern pattern = new IssuePattern().setBeginBlockRegexp(nullToEmpty(beginBlockRegexp)).setEndBlockRegexp(nullToEmpty(endBlockRegexp));
      blockPatterns.add(pattern);
    }

    // Patterns All File
    patternConf = StringUtils.defaultIfBlank(settings.getString(IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = IgnoreIssuesConfiguration.PATTERNS_ALLFILE_KEY + "." + id + ".";
      String allFileRegexp = settings.getString(propPrefix + IgnoreIssuesConfiguration.FILE_REGEXP);
      PatternDecoder.checkWholeFileRegexp(allFileRegexp);
      IssuePattern pattern = new IssuePattern().setAllFileRegexp(nullToEmpty(allFileRegexp));
      allFilePatterns.add(pattern);
    }
  }
}
