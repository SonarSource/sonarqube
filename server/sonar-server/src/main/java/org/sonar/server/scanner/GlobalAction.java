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

package org.sonar.server.scanner;

import org.apache.commons.io.IOUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.GlobalRepositories;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;

public class GlobalAction implements ScannerWsAction {

  private final DbClient dbClient;
  private final PropertiesDao propertiesDao;
  private final UserSession userSession;

  public GlobalAction(DbClient dbClient, PropertiesDao propertiesDao, UserSession userSession) {
    this.dbClient = dbClient;
    this.propertiesDao = propertiesDao;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("global")
      .setDescription("Return metrics and global properties")
      .setSince("4.5")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    boolean hasScanPerm = userSession.hasGlobalPermission(GlobalPermissions.SCAN_EXECUTION);
    boolean hasPreviewPerm = userSession.hasGlobalPermission(GlobalPermissions.PREVIEW_EXECUTION);
    if (!hasPreviewPerm && !hasScanPerm) {
      throw new ForbiddenException(Messages.NO_PERMISSION);
    }

    DbSession session = dbClient.openSession(false);
    try {
      GlobalRepositories ref = new GlobalRepositories();
      addMetrics(ref, session);
      addSettings(ref, hasScanPerm, hasPreviewPerm, session);

      response.stream().setMediaType(MimeTypes.JSON);
      IOUtils.write(ref.toJson(), response.stream().output());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void addMetrics(GlobalRepositories ref, DbSession session) {
    for (MetricDto metric : dbClient.metricDao().selectEnabled(session)) {
      ref.addMetric(
        new org.sonar.batch.protocol.input.Metric(metric.getId(), metric.getKey(),
          metric.getValueType(),
          metric.getDescription(),
          metric.getDirection(),
          metric.getKey(),
          metric.isQualitative(),
          metric.isUserManaged(),
          metric.getWorstValue(),
          metric.getBestValue(),
          metric.isOptimizedBestValue()));
    }
  }

  private void addSettings(GlobalRepositories ref, boolean hasScanPerm, boolean hasPreviewPerm, DbSession session) {
    for (PropertyDto propertyDto : propertiesDao.selectGlobalProperties(session)) {
      String key = propertyDto.getKey();
      String value = propertyDto.getValue();

      if (isPropertyAllowed(key, hasScanPerm, hasPreviewPerm)) {
        ref.addGlobalSetting(key, value);
      }
    }
  }

  private static boolean isPropertyAllowed(String key, boolean hasScanPerm, boolean hasPreviewPerm) {
    return !key.contains(".secured") || hasScanPerm || (key.contains(".license") && hasPreviewPerm);
  }

}
