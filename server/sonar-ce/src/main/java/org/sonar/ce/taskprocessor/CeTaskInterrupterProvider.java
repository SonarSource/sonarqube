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
package org.sonar.ce.taskprocessor;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.CeTaskInterrupter;

import static com.google.common.base.Preconditions.checkState;

public class CeTaskInterrupterProvider extends ProviderAdapter {
  private static final String PROPERTY_CE_TASK_TIMEOUT = "sonar.ce.task.timeoutSeconds";

  private CeTaskInterrupter instance;

  public CeTaskInterrupter provide(Configuration configuration, CeWorkerController ceWorkerController, System2 system2) {
    if (instance == null) {
      instance = configuration.getLong(PROPERTY_CE_TASK_TIMEOUT)
        .filter(timeOutInSeconds -> {
          checkState(timeOutInSeconds >= 1, "The property '%s' must be a long value >= 1. Got '%s'", PROPERTY_CE_TASK_TIMEOUT, timeOutInSeconds);
          return true;
        })
        .map(timeOutInSeconds -> (CeTaskInterrupter) new TimeoutCeTaskInterrupter(timeOutInSeconds * 1_000L, ceWorkerController, system2))
        .orElseGet(SimpleCeTaskInterrupter::new);
    }
    return instance;
  }
}
