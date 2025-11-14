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
package org.sonar.ce.task.projectexport.component;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.slf4j.LoggerFactory;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.DumpWriter;
import org.sonar.ce.task.projectexport.steps.ProjectHolder;
import org.sonar.ce.task.projectexport.steps.StreamWriter;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.sonar.db.DatabaseUtils.getString;

public class ExportComponentsStep implements ComputationStep {

  // Results are ordered by ascending id so that any parent is located
  // before its children.
  private static final String QUERY = "select" +
    " p.uuid, p.qualifier, p.uuid_path, p.kee, p.name," +
    " p.description, p.scope, p.language, p.long_name, p.path," +
    " p.deprecated_kee, p.branch_uuid" +
    " from components p" +
    " join components pp on pp.uuid = p.branch_uuid" +
    " join project_branches pb on pb.uuid = pp.uuid" +
    " where pb.project_uuid=? and pb.branch_type = 'BRANCH' and pb.exclude_from_purge=? and p.enabled=?";
  private final DbClient dbClient;
  private final ProjectHolder projectHolder;
  private final MutableComponentRepository componentRepository;
  private final DumpWriter dumpWriter;

  public ExportComponentsStep(DbClient dbClient, ProjectHolder projectHolder, MutableComponentRepository componentRepository, DumpWriter dumpWriter) {
    this.dbClient = dbClient;
    this.projectHolder = projectHolder;
    this.componentRepository = componentRepository;
    this.dumpWriter = dumpWriter;
  }

  @Override
  public void execute(Context context) {
    long ref = 1;
    long count = 0L;
    try (
      StreamWriter<ProjectDump.Component> output = dumpWriter.newStreamWriter(DumpElement.COMPONENTS);
      DbSession dbSession = dbClient.openSession(false);
      PreparedStatement stmt = createSelectStatement(dbSession);
      ResultSet rs = stmt.executeQuery()) {
      ProjectDump.Component.Builder componentBuilder = ProjectDump.Component.newBuilder();
      while (rs.next()) {
        String uuid = getString(rs, 1);
        String qualifier = getString(rs, 2);
        String uuidPath = getString(rs, 3);
        componentBuilder.clear();
        componentRepository.register(ref, uuid, ComponentQualifiers.FILE.equals(qualifier));
        ProjectDump.Component component = componentBuilder
          .setRef(ref)
          .setUuid(uuid)
          .setUuidPath(uuidPath)
          .setKey(getString(rs, 4))
          .setName(defaultString(getString(rs, 5)))
          .setDescription(defaultString(getString(rs, 6)))
          .setScope(getString(rs, 7))
          .setQualifier(qualifier)
          .setLanguage(defaultString(getString(rs, 8)))
          .setLongName(defaultString(getString(rs, 9)))
          .setPath(defaultString(getString(rs, 10)))
          .setDeprecatedKey(defaultString(getString(rs, 11)))
          .setProjectUuid(getString(rs, 12))
          .build();
        output.write(component);
        ref++;
        count++;
      }
      LoggerFactory.getLogger(getClass()).debug("{} components exported", count);
    } catch (Exception e) {
      throw new IllegalStateException(format("Component Export failed after processing %d components successfully", count), e);
    }
  }

  private PreparedStatement createSelectStatement(DbSession dbSession) throws SQLException {
    PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(dbSession, QUERY);
    try {
      stmt.setString(1, projectHolder.projectDto().getUuid());
      stmt.setBoolean(2, true);
      stmt.setBoolean(3, true);
      return stmt;
    } catch (Exception t) {
      DatabaseUtils.closeQuietly(stmt);
      throw t;
    }
  }

  @Override
  public String getDescription() {
    return "Export components";
  }
}
