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
package org.sonar.wsclient.system.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.system.Migration;
import org.sonar.wsclient.system.SystemClient;

import java.util.Collections;
import java.util.Map;

public class DefaultSystemClient implements SystemClient {

  private final HttpRequestFactory requestFactory;

  public DefaultSystemClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public Migration migrate() {
    String json = requestFactory.post("/api/server/setup", Collections.<String, Object>emptyMap());
    return jsonToMigration(json);
  }

  @Override
  public Migration migrate(long timeoutInMs, long rateInMs) {
    if (rateInMs >= timeoutInMs) {
      throw new IllegalArgumentException("Timeout must be greater than rate");
    }
    Migration migration = null;
    boolean running = true;
    long endAt = System.currentTimeMillis() + timeoutInMs;
    while (running && System.currentTimeMillis() < endAt) {
      migration = migrate();
      if (migration.status() == Migration.Status.MIGRATION_NEEDED ||
        migration.status() == Migration.Status.MIGRATION_RUNNING) {
        sleepQuietly(rateInMs);
      } else {
        running = false;
      }
    }
    return migration;
  }

  @Override
  public void restart() {
    requestFactory.post("/api/system/restart", Collections.<String, Object>emptyMap());
  }

  private void sleepQuietly(long rateInMs) {
    try {
      Thread.sleep(rateInMs);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Fail to sleep!", e);
    }
  }

  private Migration jsonToMigration(String json) {
    Map jsonRoot = (Map) JSONValue.parse(json);
    return new DefaultMigration(jsonRoot);
  }
}
