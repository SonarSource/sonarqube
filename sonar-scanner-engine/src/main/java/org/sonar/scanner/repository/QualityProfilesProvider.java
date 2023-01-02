/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.repository;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ScannerProperties;
import org.sonar.scanner.rule.QualityProfiles;
import org.springframework.context.annotation.Bean;

public class QualityProfilesProvider {
  private static final Logger LOG = Loggers.get(QualityProfilesProvider.class);
  private static final String LOG_MSG = "Load quality profiles";

  @Bean("QualityProfiles")
  public QualityProfiles provide(QualityProfileLoader loader, ScannerProperties props) {
    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    QualityProfiles profiles = new QualityProfiles(loader.load(props.getProjectKey()));
    profiler.stopInfo();
    return profiles;
  }

}
