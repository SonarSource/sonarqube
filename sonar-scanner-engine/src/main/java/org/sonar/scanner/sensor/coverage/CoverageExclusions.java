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
package org.sonar.scanner.sensor.coverage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Collection;
import java.util.Iterator;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.WildcardPattern;

public class CoverageExclusions implements Startable {

  private static final Logger LOG = LoggerFactory.getLogger(CoverageExclusions.class);

  private final Settings settings;
  private Collection<WildcardPattern> exclusionPatterns;

  public CoverageExclusions(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void start() {
    initPatterns();
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  /**
   * Thread safe
   */
  public boolean isExcluded(InputFile file) {
    boolean found = false;
    Iterator<WildcardPattern> iterator = exclusionPatterns.iterator();
    while (!found && iterator.hasNext()) {
      found = iterator.next().match(file.relativePath());
    }
    return found;
  }

  @VisibleForTesting
  final void initPatterns() {
    Builder<WildcardPattern> builder = ImmutableList.builder();
    for (String pattern : settings.getStringArray(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY)) {
      builder.add(WildcardPattern.create(pattern));
    }
    exclusionPatterns = builder.build();
    log("Excluded sources for coverage: ", exclusionPatterns);
  }

  private static void log(String title, Collection<WildcardPattern> patterns) {
    if (!patterns.isEmpty()) {
      LOG.info(title);
      for (WildcardPattern pattern : patterns) {
        LOG.info("  " + pattern);
      }
    }
  }
}
