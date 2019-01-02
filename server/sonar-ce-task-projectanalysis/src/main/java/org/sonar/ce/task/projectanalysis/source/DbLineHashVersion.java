/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source;

import java.util.HashMap;
import java.util.Map;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.LineHashVersion;
import org.sonar.ce.task.projectanalysis.component.Component;

public class DbLineHashVersion {
  private final Map<Component, LineHashVersion> lineHashVersionPerComponent = new HashMap<>();
  private final DbClient dbClient;

  public DbLineHashVersion(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Reads from DB the version of line hashes for a component and returns if it was generated taking into account the ranges of significant code.
   * The response is cached.
   * Returns false if the component is not in the DB.
   */
  public boolean hasLineHashesWithSignificantCode(Component component) {
    return lineHashVersionPerComponent.computeIfAbsent(component, this::compute) == LineHashVersion.WITH_SIGNIFICANT_CODE;
  }

  private LineHashVersion compute(Component component) {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.fileSourceDao().selectLineHashesVersion(session, component.getUuid());
    }
  }
}
