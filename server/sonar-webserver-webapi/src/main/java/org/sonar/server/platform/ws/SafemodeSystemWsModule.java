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
package org.sonar.server.platform.ws;

import org.sonar.core.platform.Module;
import org.sonar.server.common.platform.SafeModeLivenessCheckerImpl;
import org.sonar.server.monitoring.MonitoringWs;
import org.sonar.server.user.BearerPasscode;

public class SafemodeSystemWsModule extends Module {
  @Override
  protected void configureModule() {
    add(
      StatusAction.class,
      MigrateDbAction.class,
      DbMigrationStatusAction.class,

      HealthActionSupport.class,
      SafeModeHealthAction.class,
      SafeModeLivenessCheckerImpl.class,
      LivenessActionSupport.class,
      SafeModeLivenessAction.class,

      MonitoringWs.class,
      BearerPasscode.class,
      SafeModeMonitoringMetricAction.class,

      SystemWs.class);
  }
}
