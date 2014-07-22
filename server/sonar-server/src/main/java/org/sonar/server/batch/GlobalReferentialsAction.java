/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.batch;

import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.GlobalReferentials;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;

import java.util.Map;

public class GlobalReferentialsAction implements RequestHandler {

  private final DbClient dbClient;
  private final Settings settings;

  public GlobalReferentialsAction(DbClient dbClient, Settings settings) {
    this.dbClient = dbClient;
    this.settings = settings;
  }

  void define(WebService.NewController controller) {
    controller.createAction("global")
      .setDescription("Return global referentials")
      .setSince("4.5")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession userSession = UserSession.get();
    boolean hasScanPerm = userSession.hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    boolean hasDryRunPerm = userSession.hasGlobalPermission(GlobalPermissions.DRY_RUN_EXECUTION);

    DbSession session = dbClient.openSession(false);
    try {
      GlobalReferentials ref = new GlobalReferentials();
      addMetrics(ref, session);
      addSettings(ref, hasScanPerm, hasDryRunPerm);

      response.stream().setMediaType(MimeTypes.JSON);
      IOUtils.write(ref.toJson(), response.stream().output());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void addMetrics(GlobalReferentials ref, DbSession session) {
    for (MetricDto metric : dbClient.metricDao().findEnabled(session)) {
      Boolean optimizedBestValue = metric.isOptimizedBestValue();
      ref.addMetric(
        new org.sonar.batch.protocol.input.Metric(metric.getId(), metric.getKey(),
          metric.getValueType(),
          metric.getDescription(),
          metric.getDirection(),
          metric.getName(),
          metric.isQualitative(),
          metric.isUserManaged(),
          metric.getWorstValue(),
          metric.getBestValue(),
          optimizedBestValue != null ? optimizedBestValue : false));
    }
  }

  private void addSettings(GlobalReferentials ref, boolean hasScanPerm, boolean hasDryRunPerm) {
    for (Map.Entry<String, String> entry : settings.getProperties().entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (isPropertyAllowed(key, hasScanPerm, hasDryRunPerm)) {
        ref.addGlobalSetting(key, value);
      }
    }
  }

  private boolean isPropertyAllowed(String key, boolean hasScanPerm, boolean hasDryRunPerm){
    return !key.contains(".secured") || hasScanPerm || (key.contains(".license") && hasDryRunPerm);
  }

}
