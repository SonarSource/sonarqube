/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.component.es;

import com.google.common.collect.Maps;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newDeveloper;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class ProjectMeasuresResultSetIteratorTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();
  DbSession dbSession = dbTester.getSession();

  ComponentDbTester componentDbTester = new ComponentDbTester(dbTester);

  @Test
  public void return_one_project_measure() {
    ComponentDto project = newProjectDto().setKey("Project-Key").setName("Project Name");
    SnapshotDto analysis = componentDbTester.insertProjectAndSnapshot(project);

    Map<String, ProjectMeasuresDoc> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasuresDoc doc = docsById.get(project.uuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getId()).isEqualTo(project.uuid());
    assertThat(doc.getKey()).isEqualTo("Project-Key");
    assertThat(doc.getName()).isEqualTo("Project Name");
    assertThat(doc.getAnalysedAt()).isNotNull().isEqualTo(new Date(analysis.getCreatedAt()));
  }

  @Test
  public void return_many_project_measures() {
    componentDbTester.insertProjectAndSnapshot(newProjectDto());
    componentDbTester.insertProjectAndSnapshot(newProjectDto());
    componentDbTester.insertProjectAndSnapshot(newProjectDto());

    assertThat(createResultSetAndReturnDocsById()).hasSize(3);
  }

  @Test
  public void return_project_without_analysis() throws Exception {
    ComponentDto project = componentDbTester.insertComponent(newProjectDto());
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project).setLast(false));
    dbSession.commit();

    Map<String, ProjectMeasuresDoc> docsById = createResultSetAndReturnDocsById();

    assertThat(docsById).hasSize(1);
    ProjectMeasuresDoc doc = docsById.get(project.uuid());
    assertThat(doc.getAnalysedAt()).isNull();
  }

  @Test
  public void does_not_return_non_active_projects() throws Exception {
    // Disabled project
    componentDbTester.insertProjectAndSnapshot(newProjectDto().setEnabled(false));
    // Disabled project with analysis
    ComponentDto project = componentDbTester.insertComponent(newProjectDto().setEnabled(false));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project));

    // A view
    componentDbTester.insertProjectAndSnapshot(newView());

    // A developer
    componentDbTester.insertProjectAndSnapshot(newDeveloper("dev"));

    dbSession.commit();

    assertResultSetIsEmpty();
  }

  @Test
  public void return_only_docs_from_given_project() throws Exception {
    ComponentDto project = newProjectDto();
    SnapshotDto analysis = componentDbTester.insertProjectAndSnapshot(project);
    componentDbTester.insertProjectAndSnapshot(newProjectDto());
    componentDbTester.insertProjectAndSnapshot(newProjectDto());

    Map<String, ProjectMeasuresDoc> docsById = createResultSetAndReturnDocsById(0L, project.uuid());

    assertThat(docsById).hasSize(1);
    ProjectMeasuresDoc doc = docsById.get(project.uuid());
    assertThat(doc).isNotNull();
    assertThat(doc.getId()).isEqualTo(project.uuid());
    assertThat(doc.getKey()).isNotNull().isEqualTo(project.getKey());
    assertThat(doc.getName()).isNotNull().isEqualTo(project.name());
    assertThat(doc.getAnalysedAt()).isNotNull().isEqualTo(new Date(analysis.getCreatedAt()));
  }

  @Test
  public void return_only_docs_after_date() throws Exception {
    ComponentDto project1 = newProjectDto();
    dbClient.componentDao().insert(dbSession, project1);
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project1).setCreatedAt(1_000_000L));
    ComponentDto project2 = newProjectDto();
    dbClient.componentDao().insert(dbSession, project2);
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project2).setCreatedAt(2_000_000L));
    dbSession.commit();

    Map<String, ProjectMeasuresDoc> docsById = createResultSetAndReturnDocsById(1_500_000L, null);

    assertThat(docsById).hasSize(1);
    assertThat(docsById.get(project2.uuid())).isNotNull();
  }

  @Test
  public void return_nothing_on_unknown_project() throws Exception {
    componentDbTester.insertProjectAndSnapshot(newProjectDto());

    Map<String, ProjectMeasuresDoc> docsById = createResultSetAndReturnDocsById(0L, "UNKNOWN");

    assertThat(docsById).isEmpty();
  }

  private Map<String, ProjectMeasuresDoc> createResultSetAndReturnDocsById() {
    return createResultSetAndReturnDocsById(0L, null);
  }

  private Map<String, ProjectMeasuresDoc> createResultSetAndReturnDocsById(long date, @Nullable String projectUuid) {
    ProjectMeasuresResultSetIterator it = ProjectMeasuresResultSetIterator.create(dbTester.getDbClient(), dbTester.getSession(), date, projectUuid);
    Map<String, ProjectMeasuresDoc> docsById = Maps.uniqueIndex(it, ProjectMeasuresDoc::getId);
    it.close();
    return docsById;
  }

  private void assertResultSetIsEmpty() {
    assertThat(createResultSetAndReturnDocsById()).isEmpty();
  }

}
