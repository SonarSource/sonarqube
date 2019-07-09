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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.core.config.IssueExclusionProperties;

public class IssueExclusionPatternInitializer extends AbstractPatternInitializer {

  public static final String CONFIG_KEY = IssueExclusionProperties.EXCLUSION_KEY_PREFIX + ".multicriteria";
  private List<BlockIssuePattern> blockPatterns;
  private List<String> allFilePatterns;

  public IssueExclusionPatternInitializer(Configuration settings) {
    super(settings);
    loadFileContentPatterns();
  }

  @Override
  protected String getMulticriteriaConfigurationKey() {
    return CONFIG_KEY;
  }

  @Override
  public boolean hasConfiguredPatterns() {
    return hasFileContentPattern() || hasMulticriteriaPatterns();
  }

  private final void loadFileContentPatterns() {
    // Patterns Block
    blockPatterns = new ArrayList<>();
    for (String id : getSettings().getStringArray(IssueExclusionProperties.PATTERNS_BLOCK_KEY)) {
      String propPrefix = IssueExclusionProperties.PATTERNS_BLOCK_KEY + "." + id + ".";
      String beginBlockRegexp = getSettings().get(propPrefix + IssueExclusionProperties.BEGIN_BLOCK_REGEXP).orElse(null);
      if (StringUtils.isBlank(beginBlockRegexp)) {
        throw MessageException.of("Issue exclusions are misconfigured. Start block regexp is mandatory for each entry of '" + IssueExclusionProperties.PATTERNS_BLOCK_KEY + "'");
      }
      String endBlockRegexp = getSettings().get(propPrefix + IssueExclusionProperties.END_BLOCK_REGEXP).orElse(null);
      // As per configuration help, missing second field means: from start regexp to EOF
      BlockIssuePattern pattern = new BlockIssuePattern(nullToEmpty(beginBlockRegexp), nullToEmpty(endBlockRegexp));
      blockPatterns.add(pattern);
    }
    blockPatterns = Collections.unmodifiableList(blockPatterns);

    // Patterns All File
    allFilePatterns = new ArrayList<>();
    for (String id : getSettings().getStringArray(IssueExclusionProperties.PATTERNS_ALLFILE_KEY)) {
      String propPrefix = IssueExclusionProperties.PATTERNS_ALLFILE_KEY + "." + id + ".";
      String allFileRegexp = getSettings().get(propPrefix + IssueExclusionProperties.FILE_REGEXP).orElse(null);
      if (StringUtils.isBlank(allFileRegexp)) {
        throw MessageException.of("Issue exclusions are misconfigured. Remove blank entries from '" + IssueExclusionProperties.PATTERNS_ALLFILE_KEY + "'");
      }
      allFilePatterns.add(nullToEmpty(allFileRegexp));
    }
    allFilePatterns = Collections.unmodifiableList(allFilePatterns);
  }

  private static String nullToEmpty(@Nullable String str) {
    if (str == null) {
      return "";
    }
    return str;
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
