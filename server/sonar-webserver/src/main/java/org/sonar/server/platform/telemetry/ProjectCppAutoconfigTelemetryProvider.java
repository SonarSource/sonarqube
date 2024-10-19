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
package org.sonar.server.platform.telemetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.property.PropertyQuery;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.TelemetryDataType;

public class ProjectCppAutoconfigTelemetryProvider implements TelemetryDataProvider<String> {

  private final DbClient dbClient;

  public ProjectCppAutoconfigTelemetryProvider(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public String getMetricKey() {
    return "project_cpp_config_type";
  }

  @Override
  public Dimension getDimension() {
    return Dimension.PROJECT;
  }

  @Override
  public Granularity getGranularity() {
    return Granularity.WEEKLY;
  }

  @Override
  public TelemetryDataType getType() {
    return TelemetryDataType.STRING;
  }

  @Override
  public Map<String, String> getValues() {
    Map<String, String> cppConfigTypePerProjectUuid = new HashMap<>();
    try (DbSession dbSession = dbClient.openSession(true)) {
      // In the future ideally languages should be defined in the codebase as enums, using strings is error-prone
      List<ProjectDto> cppProjects = dbClient.projectDao().selectProjectsByLanguage(dbSession, Set.of("cpp", "c"));
      for (ProjectDto cppProject : cppProjects) {
        CppConfigType cppConfigType = getCppConfigType(cppProject, dbSession);
        cppConfigTypePerProjectUuid.put(cppProject.getUuid(), cppConfigType.name());
      }
    }
    return cppConfigTypePerProjectUuid;
  }

  private CppConfigType getCppConfigType(ProjectDto project, DbSession dbSession) {
    List<PropertyDto> propertyDtos = dbClient.propertiesDao().selectByQuery(PropertyQuery
      .builder()
      .setEntityUuid(project.getUuid())
      .build(), dbSession);
    for (PropertyDto propertyDto : propertyDtos) {
      if (propertyDto.getKey().equals("sonar.cfamily.build-wrapper-output")) {
        return CppConfigType.BW_DEPRECATED;
      }
      if (propertyDto.getKey().equals("sonar.cfamily.compile-commands")) {
        return CppConfigType.COMPDB;
      }
    }
    return CppConfigType.AUTOCONFIG;
  }

  enum CppConfigType {
    BW_DEPRECATED, COMPDB, AUTOCONFIG
  }
}
