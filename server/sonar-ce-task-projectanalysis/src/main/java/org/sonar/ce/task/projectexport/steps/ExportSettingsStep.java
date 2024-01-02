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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import java.util.Set;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectexport.component.ComponentRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectExportMapper;
import org.sonar.db.property.PropertyDto;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultString;

public class ExportSettingsStep implements ComputationStep {

  /**
   * These properties are not exported as values depend on environment data that are not
   * exported within the dump (Quality Gate, users).
   */
  private static final Set<String> IGNORED_KEYS = Set.of("sonar.issues.defaultAssigneeLogin");

  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final ComponentRepository componentRepository;
  private final DumpWriter dumpWriter;

  public ExportSettingsStep(DbClient dbClient, ProjectHolder projectHolder, ComponentRepository componentRepository, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.componentRepository = componentRepository;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long count = 0L;
    try (
      StreamWriter<ProjectDump.Setting> output = dumpWriter.newStreamWriter(DumpElement.SETTINGS);
      DbSession dbSession = dbClient.openSession(false)) {

      final ProjectDump.Setting.Builder builder = ProjectDump.Setting.newBuilder();
      final List<PropertyDto> properties = dbSession.getMapper(ProjectExportMapper.class).selectPropertiesForExport(projectHolder.projectDto().getUuid())
        .stream()
        .filter(dto -> dto.getComponentUuid() != null)
        .filter(dto -> !IGNORED_KEYS.contains(dto.getKey()))
        .toList();
      for (PropertyDto property : properties) {
        builder.clear()
          .setKey(property.getKey())
          .setValue(defaultString(property.getValue()));

        if (property.getComponentUuid() != null) {
          builder.setComponentRef(componentRepository.getRef(property.getComponentUuid()));
        }
        output.write(builder.build());
        ++count;
      }

      Loggers.get(getClass()).debug("{} settings exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("Settings Export failed after processing %d settings successfully", count), e);
    }
  }

  @Override
  public String getDescription() {
    return "Export settings";
  }
}
