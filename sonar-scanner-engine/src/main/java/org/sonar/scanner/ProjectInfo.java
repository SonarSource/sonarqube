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
package org.sonar.scanner;

import java.time.Clock;
import java.util.Date;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;

/**
 * @since 6.3
 *
 * Immutable after {@link #start()}
 */
public class ProjectInfo implements Startable {
  private final Clock clock;
  private Configuration settings;

  private Date analysisDate;
  private String projectVersion;

  public ProjectInfo(Configuration settings, Clock clock) {
    this.settings = settings;
    this.clock = clock;
  }

  public Date analysisDate() {
    return analysisDate;
  }

  @CheckForNull
  public String projectVersion() {
    return projectVersion;
  }

  private Date loadAnalysisDate() {
    Optional<String> value = settings.get(CoreProperties.PROJECT_DATE_PROPERTY);
    if (!value.isPresent()) {
      return Date.from(clock.instant());
    }
    try {
      // sonar.projectDate may have been specified as a time
      return DateUtils.parseDateTime(value.get());
    } catch (RuntimeException e) {
      // this is probably just a date
    }
    try {
      // sonar.projectDate may have been specified as a date
      return DateUtils.parseDate(value.get());
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Illegal value for '" + CoreProperties.PROJECT_DATE_PROPERTY + "'", e);
    }
  }

  @CheckForNull
  private String loadProjectVersion() {
    return settings.get(CoreProperties.PROJECT_VERSION_PROPERTY)
      .filter(version -> {
        if (version.length() > 100) {
          throw MessageException.of(format("\"%s\" is not a valid project version. " +
            "The maximum length for version numbers is 100 characters.", version));
        }
        return true;
      })
      .orElse(null);
  }

  @Override
  public void start() {
    this.analysisDate = loadAnalysisDate();
    this.projectVersion = loadProjectVersion();
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
