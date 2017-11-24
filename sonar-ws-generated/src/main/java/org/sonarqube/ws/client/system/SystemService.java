/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
 * Get system details, and perform some management actions, such as restarting, and initiating a database migration (as part of a system upgrade).
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/system">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class SystemService extends BaseService {

  public SystemService(WsConnector wsConnector) {
    super(wsConnector, "api/system");
  }

  /**
   * Temporarily changes level of logs. New level is not persistent and is lost when restarting server. Requires system administration permission.
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
   * Display the database migration status of SonarQube.<br/>State values are:<ul><li>NO_MIGRATION: DB is up to date with current version of SonarQube.</li><li>NOT_SUPPORTED: Migration is not supported on embedded databases.</li><li>MIGRATION_RUNNING: DB migration is under go.</li><li>MIGRATION_SUCCEEDED: DB migration has run and has been successful.</li><li>MIGRATION_FAILED: DB migration has run and failed. SonarQube must be restarted in order to retry a DB migration (optionally after DB has been restored from backup).</li><li>MIGRATION_REQUIRED: DB migration is required.</li></ul>
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
   * Provide health status of SonarQube.<p>Require 'Administer System' permission or authentication with passcode</p><p>  <ul> <li>GREEN: SonarQube is fully operational</li> <li>YELLOW: SonarQube is usable, but it needs attention in order to be fully operational</li> <li>RED: SonarQube is not operational</li> </ul></p>
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
   * Get detailed information about system configuration.<br/>Requires 'Administer' permissions.<br/>Since 5.5, this web service becomes internal in order to more easily update result.
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
   * Get system logs in plain-text format. Requires system administration permission.
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
   * Migrate the database to match the current version of SonarQube.<br/>Sending a POST request to this URL starts the DB migration. It is strongly advised to <strong>make a database backup</strong> before invoking this WS.<br/>State values are:<ul><li>NO_MIGRATION: DB is up to date with current version of SonarQube.</li><li>NOT_SUPPORTED: Migration is not supported on embedded databases.</li><li>MIGRATION_RUNNING: DB migration is under go.</li><li>MIGRATION_SUCCEEDED: DB migration has run and has been successful.</li><li>MIGRATION_FAILED: DB migration has run and failed. SonarQube must be restarted in order to retry a DB migration (optionally after DB has been restored from backup).</li><li>MIGRATION_REQUIRED: DB migration is required.</li></ul>
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
   * Answers "pong" as plain-text
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
   * Restart server. Require 'Administer System' permission. Perform a full restart of the Web, Search and Compute Engine Servers processes.
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
   * Get state information about SonarQube.<p>status: the running status <ul> <li>STARTING: SonarQube Web Server is up and serving some Web Services (eg. api/system/status) but initialization is still ongoing</li> <li>UP: SonarQube instance is up and running</li> <li>DOWN: SonarQube instance is up but not running because migration has failed (refer to WS /api/system/migrate_db for details) or some other reason (check logs).</li> <li>RESTARTING: SonarQube instance is still up but a restart has been requested (refer to WS /api/system/restart for details).</li> <li>DB_MIGRATION_NEEDED: database migration is required. DB migration can be started using WS /api/system/migrate_db.</li> <li>DB_MIGRATION_RUNNING: DB migration is running (refer to WS /api/system/migrate_db for details)</li> </ul></p>
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
   * Lists available upgrades for the SonarQube instance (if any) and for each one, lists incompatible plugins and plugins requiring upgrade.<br/>Plugin information is retrieved from Update Center. Date and time at which Update Center was last refreshed is provided in the response.
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
