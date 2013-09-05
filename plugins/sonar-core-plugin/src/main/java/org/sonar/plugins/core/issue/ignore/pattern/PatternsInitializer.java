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
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.config.Settings;
import org.sonar.plugins.core.issue.ignore.Constants;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.nullToEmpty;

public class PatternsInitializer implements BatchExtension {

  private final Settings settings;

  private List<Pattern> multicriteriaPatterns;
  private List<Pattern> blockPatterns;
  private List<Pattern> allFilePatterns;
  private Map<String, Pattern> extraPatternByResource = Maps.newHashMap();

  public PatternsInitializer(Settings settings) {
    this.settings = settings;
    initPatterns();
  }

  public List<Pattern> getMulticriteriaPatterns() {
    return multicriteriaPatterns;
  }

  public List<Pattern> getBlockPatterns() {
    return blockPatterns;
  }

  public List<Pattern> getAllFilePatterns() {
    return allFilePatterns;
  }

  public Pattern getExtraPattern(String resource) {
    return extraPatternByResource.get(resource.substring(resource.lastIndexOf(":") + 1));
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
    String patternConf = StringUtils.defaultIfBlank(settings.getString(Constants.PATTERNS_MULTICRITERIA_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = Constants.PATTERNS_MULTICRITERIA_KEY + "." + id + ".";
      String resourceKeyPattern = settings.getString(propPrefix + Constants.RESOURCE_KEY);
      String ruleKeyPattern = settings.getString(propPrefix + Constants.RULE_KEY);
      Pattern pattern = new Pattern(firstNonNull(resourceKeyPattern, "*"), firstNonNull(ruleKeyPattern, "*"));
      String lineRange = settings.getString(propPrefix + Constants.LINE_RANGE_KEY);
      PatternDecoder.decodeRangeOfLines(pattern, firstNonNull(lineRange, "*"));
      multicriteriaPatterns.add(pattern);
    }

    // Patterns Block
    patternConf = StringUtils.defaultIfBlank(settings.getString(Constants.PATTERNS_BLOCK_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = Constants.PATTERNS_BLOCK_KEY + "." + id + ".";
      String beginBlockRegexp = settings.getString(propPrefix + Constants.BEGIN_BLOCK_REGEXP);
      String endBlockRegexp = settings.getString(propPrefix + Constants.END_BLOCK_REGEXP);
      Pattern pattern = new Pattern().setBeginBlockRegexp(nullToEmpty(beginBlockRegexp)).setEndBlockRegexp(nullToEmpty(endBlockRegexp));
      blockPatterns.add(pattern);
    }

    // Patterns All File
    patternConf = StringUtils.defaultIfBlank(settings.getString(Constants.PATTERNS_ALLFILE_KEY), "");
    for (String id : StringUtils.split(patternConf, ',')) {
      String propPrefix = Constants.PATTERNS_ALLFILE_KEY + "." + id + ".";
      String allFileRegexp = settings.getString(propPrefix + Constants.FILE_REGEXP);
      Pattern pattern = new Pattern().setAllFileRegexp(nullToEmpty(allFileRegexp));
      allFilePatterns.add(pattern);
    }
  }

  public void addPatternToExcludeResource(String resource) {
    extraPatternByResource.put(resource, new Pattern(resource, "*").setCheckLines(false));
  }

  public void addPatternToExcludeLines(String resource, Set<LineRange> lineRanges) {
    extraPatternByResource.put(resource, new Pattern(resource, "*", lineRanges));
  }

}
