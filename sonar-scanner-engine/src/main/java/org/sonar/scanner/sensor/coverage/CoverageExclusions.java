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

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.concurrent.Immutable;

import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.WildcardPattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

@Immutable
public class CoverageExclusions implements Startable {
  private static final Logger LOG = LoggerFactory.getLogger(CoverageExclusions.class);

  private Collection<WildcardPattern> exclusionPatterns;

  public CoverageExclusions(Configuration settings) {
    Builder<WildcardPattern> builder = ImmutableList.builder();
    for (String pattern : settings.getStringArray(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY)) {
      builder.add(WildcardPattern.create(pattern));
    }
    exclusionPatterns = builder.build();
  }

  @Override
  public void start() {
    log("Excluded sources for coverage: ", exclusionPatterns);
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  public boolean isExcluded(InputFile file) {
    boolean found = false;
    Iterator<WildcardPattern> iterator = exclusionPatterns.iterator();
    while (!found && iterator.hasNext()) {
      found = iterator.next().match(file.relativePath());
    }
    return found;
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
