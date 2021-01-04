/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DropLocalWebhooks extends DataChange {
  private static final Logger LOG = Loggers.get(DropLocalWebhooks.class);

  public DropLocalWebhooks(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("select w.uuid, w.name, w.url, w.project_uuid, p.name from webhooks w left join projects p on p.uuid = w.project_uuid");
    massUpdate.update("delete from webhooks where uuid = ?");
    massUpdate.execute((row, update) -> {
      try {
        String webhookName = row.getString(2);
        String webhookUrl = row.getString(3);
        URL url = new URL(webhookUrl);
        InetAddress address = InetAddress.getByName(url.getHost());
        if (address.isLoopbackAddress() || address.isAnyLocalAddress()) {
          boolean projectLevel = row.getString(4) != null;
          if (projectLevel) {
            String projectName = row.getString(5);
            LOG.warn("Webhook '{}' for project '{}' has been removed because it used an invalid, unsafe URL. Please recreate " +
              "this webhook with a valid URL or ask a project administrator to do it if it is still needed.", webhookName, projectName);
          } else {
            LOG.warn("Global webhook '{}' has been removed because it used an invalid, unsafe URL. Please recreate this webhook with a valid URL" +
              " if it is still needed.", webhookName);
          }

          update.setString(1, row.getString(1));
          return true;
        }
      } catch (MalformedURLException | UnknownHostException e) {
        return false;
      }

      return false;
    });
  }
}
