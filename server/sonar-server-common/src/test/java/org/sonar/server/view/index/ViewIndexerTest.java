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
package org.sonar.server.view.index;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.EsTester;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.server.view.index.ViewIndexDefinition.TYPE_VIEW;

public class ViewIndexerTest {

  @Rule
  public TestRule safeguardTimeout = new DisableOnDebug(Timeout.seconds(60));
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public EsTester es = EsTester.create();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ViewIndexer underTest = new ViewIndexer(dbClient, es.client());

  @Test
  public void index_nothing() {
    underTest.indexOnStartup(emptySet());
    assertThat(es.countDocuments(TYPE_VIEW)).isEqualTo(0L);
  }

  @Test
  public void index_on_startup() {
    // simple view
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto view1 = db.components().insertView();
    db.components().insertSnapshot(view1, t -> t.setLast(true));
    db.components().insertComponent(newProjectCopy(project1, view1));
    // view with subview
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentDto view2 = db.components().insertView();
    db.components().insertSnapshot(view2, t -> t.setLast(true));
    db.components().insertComponent(newProjectCopy(project2, view2));
    ComponentDto subView = db.components().insertComponent(newSubView(view2));
    db.components().insertComponent(newProjectCopy(project3, subView));
    // view without project
    ComponentDto view3 = db.components().insertView();
    db.components().insertSnapshot(view3, t -> t.setLast(true));

    underTest.indexOnStartup(emptySet());

    List<ViewDoc> docs = es.getDocuments(TYPE_VIEW, ViewDoc.class);
    assertThat(docs).hasSize(4);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, ViewDoc::uuid);

    assertThat(viewsByUuid.get(view1.uuid()).projects()).containsOnly(project1.uuid());
    assertThat(viewsByUuid.get(view2.uuid()).projects()).containsOnly(project2.uuid(), project3.uuid());
    assertThat(viewsByUuid.get(subView.uuid()).projects()).containsOnly(project3.uuid());
    assertThat(viewsByUuid.get(view3.uuid()).projects()).isEmpty();
  }

  @Test
  public void index_root_view() {
    // simple view
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto view1 = db.components().insertView();
    db.components().insertSnapshot(view1, t -> t.setLast(true));
    db.components().insertComponent(newProjectCopy(project1, view1));
    // view with subview
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentDto view2 = db.components().insertView();
    db.components().insertSnapshot(view2, t -> t.setLast(true));
    db.components().insertComponent(newProjectCopy(project2, view2));
    ComponentDto subView = db.components().insertComponent(newSubView(view2));
    db.components().insertComponent(newProjectCopy(project3, subView));
    // view without project
    ComponentDto view3 = db.components().insertView();
    db.components().insertSnapshot(view3, t -> t.setLast(true));

    underTest.index(view2.uuid());

    List<ViewDoc> docs = es.getDocuments(TYPE_VIEW, ViewDoc.class);
    assertThat(docs).hasSize(2);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, ViewDoc::uuid);

    assertThat(viewsByUuid.get(view2.uuid()).projects()).containsOnly(project2.uuid(), project3.uuid());
    assertThat(viewsByUuid.get(subView.uuid()).projects()).containsOnly(project3.uuid());
  }

  @Test
  public void index_view_doc() {
    underTest.index(new ViewDoc().setUuid("EFGH").setProjects(newArrayList("KLMN", "JKLM")));

    List<ViewDoc> result = es.getDocuments(TYPE_VIEW, ViewDoc.class);

    assertThat(result).hasSize(1);
    ViewDoc view = result.get(0);
    assertThat(view.uuid()).isEqualTo("EFGH");
    assertThat(view.projects()).containsOnly("KLMN", "JKLM");
  }

  @Test
  public void index_application() {
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy("PC1", project, application));

    underTest.index(application.uuid());
    List<ViewDoc> result = es.getDocuments(TYPE_VIEW, ViewDoc.class);

    assertThat(result).hasSize(1);
    ViewDoc resultApp = result.get(0);
    assertThat(resultApp.uuid()).isEqualTo(application.uuid());
    assertThat(resultApp.projects()).containsExactlyInAnyOrder(project.uuid());
  }

  @Test
  public void index_application_on_startup() {
    ComponentDto application = db.components().insertApplication(db.getDefaultOrganization());
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertComponent(newProjectCopy("PC1", project, application));

    underTest.indexOnStartup(emptySet());
    List<ViewDoc> result = es.getDocuments(TYPE_VIEW, ViewDoc.class);

    assertThat(result).hasSize(1);
    ViewDoc resultApp = result.get(0);
    assertThat(resultApp.uuid()).isEqualTo(application.uuid());
    assertThat(resultApp.projects()).containsExactlyInAnyOrder(project.uuid());
  }

  @Test
  public void index_application_branch() {
    ComponentDto application = db.components().insertMainBranch(c -> c.setQualifier(APP).setDbKey("app"));
    ComponentDto applicationBranch1 = db.components().insertProjectBranch(application, a -> a.setKey("app-branch1"));
    ComponentDto applicationBranch2 = db.components().insertProjectBranch(application, a -> a.setKey("app-branch2"));
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setDbKey("prj1"));
    ComponentDto project1Branch = db.components().insertProjectBranch(project1);
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setDbKey("prj2"));
    ComponentDto project2Branch = db.components().insertProjectBranch(project2);
    ComponentDto project3 = db.components().insertPrivateProject(p -> p.setDbKey("prj3"));
    ComponentDto project3Branch = db.components().insertProjectBranch(project3);
    db.components().insertComponent(newProjectCopy(project1Branch, applicationBranch1));
    db.components().insertComponent(newProjectCopy(project2Branch, applicationBranch1));
    // Technical project of applicationBranch2 should be ignored
    db.components().insertComponent(newProjectCopy(project3Branch, applicationBranch2));

    underTest.index(applicationBranch1.uuid());

    List<ViewDoc> result = es.getDocuments(TYPE_VIEW, ViewDoc.class);
    assertThat(result)
      .extracting(ViewDoc::uuid, ViewDoc::projects)
      .containsExactlyInAnyOrder(
        tuple(applicationBranch1.uuid(), asList(project1Branch.uuid(), project2Branch.uuid())));
  }

  @Test
  public void delete_should_delete_the_view() {
    ViewDoc view1 = new ViewDoc().setUuid("UUID1").setProjects(asList("P1"));
    ViewDoc view2 = new ViewDoc().setUuid("UUID2").setProjects(asList("P2", "P3", "P4"));
    ViewDoc view3 = new ViewDoc().setUuid("UUID3").setProjects(asList("P2", "P3", "P4"));
    es.putDocuments(TYPE_VIEW, view1);
    es.putDocuments(TYPE_VIEW, view2);
    es.putDocuments(TYPE_VIEW, view3);

    assertThat(es.getDocumentFieldValues(TYPE_VIEW, ViewIndexDefinition.FIELD_UUID))
      .containsOnly(view1.uuid(), view2.uuid(), view3.uuid());

    underTest.delete(dbSession, asList(view1.uuid(), view2.uuid()));

    assertThat(es.getDocumentFieldValues(TYPE_VIEW, ViewIndexDefinition.FIELD_UUID))
      .containsOnly(view3.uuid());
  }

}
