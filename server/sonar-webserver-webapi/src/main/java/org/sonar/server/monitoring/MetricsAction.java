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
package org.sonar.server.monitoring;

import org.sonar.api.server.ws.WebService;
import org.sonar.server.platform.ws.SafeModeMonitoringMetricAction;
import org.sonar.server.user.BearerPasscode;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.UserSession;

public class MetricsAction extends SafeModeMonitoringMetricAction {

  private final UserSession userSession;

  public MetricsAction(SystemPasscode systemPasscode, BearerPasscode bearerPasscode, UserSession userSession) {
    super(systemPasscode, bearerPasscode);
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("metrics")
      .setSince("9.3")
      .setDescription("""
        Return monitoring metrics in Prometheus format.\s
        Support content type 'text/plain' (default) and 'application/openmetrics-text'.
        this endpoint can be access using a Bearer token, that needs to be defined in sonar.properties with the 'sonar.web.systemPasscode' key.""")
      .setResponseExample(getClass().getResource("monitoring-metrics.txt"))
      .setHandler(this);

    isWebUpGauge.set(1D);
  }

  @Override
  public boolean isSystemAdmin() {
    return userSession.isSystemAdministrator();
  }

}
