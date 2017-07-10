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
package org.sonar.scanner;

import java.util.Date;
import java.util.Optional;

import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.System2;

/**
 * @since 6.3
 * 
 * Immutable after {@link #start()}
 */
@ScannerSide
public class ProjectAnalysisInfo implements Startable {
  private final System2 system2;
  private Configuration settings;

  private Date analysisDate;
  private String analysisVersion;

  public ProjectAnalysisInfo(Configuration settings, System2 system2) {
    this.settings = settings;
    this.system2 = system2;
  }

  public Date analysisDate() {
    return analysisDate;
  }

  public String analysisVersion() {
    return analysisVersion;
  }

  private Date loadAnalysisDate() {
    Optional<String> value = settings.get(CoreProperties.PROJECT_DATE_PROPERTY);
    if (!value.isPresent()) {
      return new Date(system2.now());
    }
    Date date;
    try {
      // sonar.projectDate may have been specified as a time
      return DateUtils.parseDateTime(value.get());
    } catch (SonarException e) {
      // this is probably just a date
      return DateUtils.parseDate(value.get());
    }
  }

  private String loadAnalysisVersion() {
    return settings.get(CoreProperties.PROJECT_VERSION_PROPERTY).orElse(null);
  }

  @Override
  public void start() {
    this.analysisDate = loadAnalysisDate();
    this.analysisVersion = loadAnalysisVersion();
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
