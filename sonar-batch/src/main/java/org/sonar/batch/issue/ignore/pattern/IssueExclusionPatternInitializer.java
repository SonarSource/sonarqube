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

package org.sonar.batch.issue.ignore.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.core.config.IssueExclusionProperties;

import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;

public class IssueExclusionPatternInitializer extends AbstractPatternInitializer {

  private List<IssuePattern> blockPatterns;
  private List<IssuePattern> allFilePatterns;
  private PatternMatcher patternMatcher;

  public IssueExclusionPatternInitializer(Settings settings) {
    super(settings);
    patternMatcher = new PatternMatcher();
    loadFileContentPatterns();
  }

  @Override
  protected String getMulticriteriaConfigurationKey() {
    return "sonar.issue.ignore" + ".multicriteria";
  }

  public PatternMatcher getPatternMatcher() {
    return patternMatcher;
  }

  @Override
  public void initializePatternsForPath(String relativePath, String componentKey) {
    for (IssuePattern pattern : getMulticriteriaPatterns()) {
      if (pattern.matchResource(relativePath)) {
        getPatternMatcher().addPatternForComponent(componentKey, pattern);
      }
    }
  }

  @Override
  public boolean hasConfiguredPatterns() {
    return hasFileContentPattern() || hasMulticriteriaPatterns();
  }

  @VisibleForTesting
  protected final void loadFileContentPatterns() {
    // Patterns Block
    blockPatterns = Lists.newArrayList();
    String patternConf = StringUtils.defaultIfBlank(getSettings().getString(IssueExclusionProperties.PATTERNS_BLOCK_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = IssueExclusionProperties.PATTERNS_BLOCK_KEY + "." + id + ".";
      String beginBlockRegexp = getSettings().getString(propPrefix + IssueExclusionProperties.BEGIN_BLOCK_REGEXP);
      String endBlockRegexp = getSettings().getString(propPrefix + IssueExclusionProperties.END_BLOCK_REGEXP);
      String[] fields = new String[]{beginBlockRegexp, endBlockRegexp};
      PatternDecoder.checkDoubleRegexpLineConstraints(StringUtils.join(fields, ","), fields);
      IssuePattern pattern = new IssuePattern().setBeginBlockRegexp(nullToEmpty(beginBlockRegexp)).setEndBlockRegexp(nullToEmpty(endBlockRegexp));
      blockPatterns.add(pattern);
    }

    // Patterns All File
    allFilePatterns = Lists.newArrayList();
    patternConf = StringUtils.defaultIfBlank(getSettings().getString(IssueExclusionProperties.PATTERNS_ALLFILE_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = IssueExclusionProperties.PATTERNS_ALLFILE_KEY + "." + id + ".";
      String allFileRegexp = getSettings().getString(propPrefix + IssueExclusionProperties.FILE_REGEXP);
      PatternDecoder.checkWholeFileRegexp(allFileRegexp);
      IssuePattern pattern = new IssuePattern().setAllFileRegexp(nullToEmpty(allFileRegexp));
      allFilePatterns.add(pattern);
    }
  }

  public List<IssuePattern> getBlockPatterns() {
    return blockPatterns;
  }

  public List<IssuePattern> getAllFilePatterns() {
    return allFilePatterns;
  }

  public boolean hasFileContentPattern() {
    return !(blockPatterns.isEmpty() && allFilePatterns.isEmpty());
  }
}
