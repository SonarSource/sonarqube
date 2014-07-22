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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.batch.protocol.input.GlobalReferentials;
import org.sonar.core.measure.db.MetricDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.plugins.MimeTypes;

public class GlobalReferentialsAction implements RequestHandler {

  private final DbClient dbClient;

  public GlobalReferentialsAction(DbClient dbClient) {
    this.dbClient = dbClient;
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
    // TODO check user permission

    DbSession session = dbClient.openSession(false);
    try {
      GlobalReferentials ref = new GlobalReferentials();
      for (MetricDto metric : dbClient.metricDao().findEnabled(session)) {
        Boolean optimizedBestValue = metric.isOptimizedBestValue();
        ref.metrics().add(
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

      response.stream().setMediaType(MimeTypes.JSON);
      IOUtils.write(ref.toJson(), response.stream().output());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
