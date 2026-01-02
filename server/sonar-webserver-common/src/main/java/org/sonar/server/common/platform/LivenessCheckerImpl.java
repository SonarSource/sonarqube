/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.platform;

import javax.annotation.Nullable;
import org.sonar.server.common.health.CeStatusNodeCheck;
import org.sonar.server.common.health.DbConnectionNodeCheck;
import org.sonar.server.common.health.EsStatusNodeCheck;
import org.sonar.server.common.health.WebServerStatusNodeCheck;
import org.sonar.server.health.Health;

public class LivenessCheckerImpl implements LivenessChecker {

  private final DbConnectionNodeCheck dbConnectionNodeCheck;
  private final CeStatusNodeCheck ceStatusNodeCheck;
  @Nullable
  private final EsStatusNodeCheck esStatusNodeCheck;
  private final WebServerStatusNodeCheck webServerStatusNodeCheck;

  public LivenessCheckerImpl(DbConnectionNodeCheck dbConnectionNodeCheck,
    WebServerStatusNodeCheck webServerStatusNodeCheck, CeStatusNodeCheck ceStatusNodeCheck, @Nullable EsStatusNodeCheck esStatusNodeCheck) {
    this.dbConnectionNodeCheck = dbConnectionNodeCheck;
    this.webServerStatusNodeCheck = webServerStatusNodeCheck;
    this.ceStatusNodeCheck = ceStatusNodeCheck;
    this.esStatusNodeCheck = esStatusNodeCheck;
  }

  public boolean liveness() {

    if (Health.Status.GREEN != dbConnectionNodeCheck.check().getStatus()) {
      return false;
    }

    if (Health.Status.GREEN != webServerStatusNodeCheck.check().getStatus()) {
      return false;
    }

    if (Health.Status.GREEN != ceStatusNodeCheck.check().getStatus()) {
      return false;
    }

    if (esStatusNodeCheck != null && Health.Status.RED == esStatusNodeCheck.check().getStatus()) {
      return false;
    }

    return true;
  }
}
