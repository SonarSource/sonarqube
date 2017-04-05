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
package org.sonar.scanner.issue.ignore.pattern;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Settings;
import org.sonar.core.config.IssueExclusionProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;

public class IssueExclusionPatternInitializer extends AbstractPatternInitializer {

  private List<BlockIssuePattern> blockPatterns;
  private List<String> allFilePatterns;

  public IssueExclusionPatternInitializer(Settings settings) {
    super(settings);
    loadFileContentPatterns();
  }

  @Override
  protected String getMulticriteriaConfigurationKey() {
    return "sonar.issue.ignore" + ".multicriteria";
  }

  @Override
  public boolean hasConfiguredPatterns() {
    return hasFileContentPattern() || hasMulticriteriaPatterns();
  }

  private final void loadFileContentPatterns() {
    // Patterns Block
    blockPatterns = new ArrayList<>();
    String patternConf = StringUtils.defaultIfBlank(getSettings().getString(IssueExclusionProperties.PATTERNS_BLOCK_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = IssueExclusionProperties.PATTERNS_BLOCK_KEY + "." + id + ".";
      String beginBlockRegexp = getSettings().getString(propPrefix + IssueExclusionProperties.BEGIN_BLOCK_REGEXP);
      String endBlockRegexp = getSettings().getString(propPrefix + IssueExclusionProperties.END_BLOCK_REGEXP);
      String[] fields = new String[] {beginBlockRegexp, endBlockRegexp};
      PatternDecoder.checkDoubleRegexpLineConstraints(StringUtils.join(fields, ","), fields);
      BlockIssuePattern pattern = new BlockIssuePattern(nullToEmpty(beginBlockRegexp), nullToEmpty(endBlockRegexp));
      blockPatterns.add(pattern);
    }
    blockPatterns = Collections.unmodifiableList(blockPatterns);

    // Patterns All File
    allFilePatterns = new ArrayList<>();
    patternConf = StringUtils.defaultIfBlank(getSettings().getString(IssueExclusionProperties.PATTERNS_ALLFILE_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = IssueExclusionProperties.PATTERNS_ALLFILE_KEY + "." + id + ".";
      String allFileRegexp = getSettings().getString(propPrefix + IssueExclusionProperties.FILE_REGEXP);
      PatternDecoder.checkWholeFileRegexp(allFileRegexp);
      allFilePatterns.add(nullToEmpty(allFileRegexp));
    }
    allFilePatterns = Collections.unmodifiableList(allFilePatterns);
  }

  public List<BlockIssuePattern> getBlockPatterns() {
    return blockPatterns;
  }

  public List<String> getAllFilePatterns() {
    return allFilePatterns;
  }

  public boolean hasFileContentPattern() {
    return !(blockPatterns.isEmpty() && allFilePatterns.isEmpty());
  }
}
