/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.configuration;

import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays a warning in the logs if property "sonar.ce.workerCount" is defined as it has been replaced
 * by an internal property (see SONAR-9507).
 */
public class CeWorkerCountSettingWarning implements Startable {
  private static final String PROPERTY_SONAR_CE_WORKER_COUNT = "sonar.ce.workerCount";
  private static final Logger LOG = LoggerFactory.getLogger(CeWorkerCountSettingWarning.class);

  private final Configuration configuration;

  public CeWorkerCountSettingWarning(Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void start() {
    configuration.get(PROPERTY_SONAR_CE_WORKER_COUNT)
      .ifPresent(workerCount -> LOG.warn("Property {} is not supported anymore and will be ignored." +
        " Remove it from sonar.properties to remove this warning.",
        PROPERTY_SONAR_CE_WORKER_COUNT));
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
