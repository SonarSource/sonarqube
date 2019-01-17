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
import java.util.function.Predicate;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;
import static org.sonar.api.CoreProperties.CODE_PERIOD_VERSION_PROPERTY;
import static org.sonar.api.CoreProperties.PROJECT_VERSION_PROPERTY;

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
  private String codePeriodVersion;

  public ProjectInfo(Configuration settings, Clock clock) {
    this.settings = settings;
    this.clock = clock;
  }

  public Date getAnalysisDate() {
    return analysisDate;
  }

  public Optional<String> getProjectVersion() {
    return Optional.ofNullable(projectVersion);
  }

  public Optional<String> getCodePeriodVersion() {
    return Optional.ofNullable(codePeriodVersion);
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

  @Override
  public void start() {
    this.analysisDate = loadAnalysisDate();
    this.projectVersion = settings.get(PROJECT_VERSION_PROPERTY)
      .map(StringUtils::trimToNull)
      .filter(validateVersion("project"))
      .orElse(null);
    this.codePeriodVersion = settings.get(CODE_PERIOD_VERSION_PROPERTY)
      .map(StringUtils::trimToNull)
      .filter(validateVersion("codePeriod"))
      .orElse(projectVersion);
  }

  private static Predicate<String> validateVersion(String versionLabel) {
    return version -> {
      if (version.length() > 100) {
        throw MessageException.of(format("\"%s\" is not a valid %s version. " +
          "The maximum length is 100 characters.", version, versionLabel));
      }
      return true;
    };
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
