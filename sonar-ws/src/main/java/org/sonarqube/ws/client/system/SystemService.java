/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client.system;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.System.HealthResponse;
import org.sonarqube.ws.System.StatusResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class SystemService extends BaseService {

  public SystemService(WsConnector wsConnector) {
    super(wsConnector, "api/system");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/change_log_level">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void changeLogLevel(ChangeLogLevelRequest request) {
    call(
      new PostRequest(path("change_log_level"))
        .setParam("level", request.getLevel())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/db_migration_status">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String dbMigrationStatus() {
    return call(
      new GetRequest(path("db_migration_status"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/health">Further information about this action online (including a response example)</a>
   * @since 6.6
   */
  public HealthResponse health() {
    return call(
      new GetRequest(path("health")),
      HealthResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/info">Further information about this action online (including a response example)</a>
   * @since 5.1
   */
  public String info() {
    return call(
      new GetRequest(path("info"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/logs">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String logs(LogsRequest request) {
    return call(
      new GetRequest(path("logs"))
        .setParam("process", request.getProcess())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/migrate_db">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String migrateDb() {
    return call(
      new PostRequest(path("migrate_db"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/ping">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public String ping() {
    return call(
      new GetRequest(path("ping"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/restart">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void restart() {
    call(
      new PostRequest(path("restart"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/status">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public StatusResponse status() {
    return call(
      new GetRequest(path("status")),
      StatusResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system/upgrades">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String upgrades() {
    return call(
      new GetRequest(path("upgrades"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
