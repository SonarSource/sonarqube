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
package org.sonar.server.qualitygate.ws;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService.Action;
import org.sonar.api.server.ws.WebService.Controller;
import org.sonar.db.DbClient;
import org.sonar.db.qualitygate.ProjectQgateAssociation;
import org.sonar.db.qualitygate.ProjectQgateAssociationQuery;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualitygate.QgateProjectFinder;
import org.sonar.server.qualitygate.QgateProjectFinder.Association;
import org.sonar.server.qualitygate.QualityGates;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO split testcases in action tests
@RunWith(MockitoJUnitRunner.class)
public class QualityGatesWsTest {

  @Mock
  private QualityGates qGates;

  @Mock
  private QgateProjectFinder projectFinder;

  @Mock
  private AppAction appHandler;

  WsTester tester;

  @Before
  public void setUp() {
    SelectAction selectAction = new SelectAction(mock(DbClient.class), mock(UserSessionRule.class), mock(ComponentFinder.class));

    tester = new WsTester(new QualityGatesWs(
      new SearchAction(projectFinder),
      new CreateAction(null, null, null, null),
      new CopyAction(qGates),
      new DestroyAction(qGates),
      new SetAsDefaultAction(qGates), new UnsetDefaultAction(qGates),
      new CreateConditionAction(null, null, null, null),
      new UpdateConditionAction(null, null, null, null),
      new DeleteConditionAction(null, null, null),
      selectAction,
      new DeselectAction(qGates, mock(DbClient.class), mock(ComponentFinder.class)),
      new AppAction(null, null, null)));
  }

  @Test
  public void define_ws() {
    Controller controller = tester.controller("api/qualitygates");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/qualitygates");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(12);

    Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.handler()).isNotNull();
    assertThat(create.since()).isEqualTo("4.3");
    assertThat(create.isPost()).isTrue();
    assertThat(create.param("name")).isNotNull();
    assertThat(create.isInternal()).isFalse();

    Action copy = controller.action("copy");
    assertThat(copy).isNotNull();
    assertThat(copy.handler()).isNotNull();
    assertThat(copy.since()).isEqualTo("4.3");
    assertThat(copy.isPost()).isTrue();
    assertThat(copy.param("id")).isNotNull();
    assertThat(copy.param("name")).isNotNull();
    assertThat(copy.isInternal()).isFalse();

    Action destroy = controller.action("destroy");
    assertThat(destroy).isNotNull();
    assertThat(destroy.handler()).isNotNull();
    assertThat(destroy.since()).isEqualTo("4.3");
    assertThat(destroy.isPost()).isTrue();
    assertThat(destroy.param("id")).isNotNull();
    assertThat(destroy.isInternal()).isFalse();

    Action setDefault = controller.action("set_as_default");
    assertThat(setDefault).isNotNull();
    assertThat(setDefault.handler()).isNotNull();
    assertThat(setDefault.since()).isEqualTo("4.3");
    assertThat(setDefault.isPost()).isTrue();
    assertThat(setDefault.param("id")).isNotNull();
    assertThat(setDefault.isInternal()).isFalse();

    Action unsetDefault = controller.action("unset_default");
    assertThat(unsetDefault).isNotNull();
    assertThat(unsetDefault.handler()).isNotNull();
    assertThat(unsetDefault.since()).isEqualTo("4.3");
    assertThat(unsetDefault.isPost()).isTrue();
    assertThat(unsetDefault.isInternal()).isFalse();

    Action createCondition = controller.action("create_condition");
    assertThat(createCondition).isNotNull();
    assertThat(createCondition.handler()).isNotNull();
    assertThat(createCondition.since()).isEqualTo("4.3");
    assertThat(createCondition.isPost()).isTrue();
    assertThat(createCondition.param("gateId")).isNotNull();
    assertThat(createCondition.param("metric")).isNotNull();
    assertThat(createCondition.param("op")).isNotNull();
    assertThat(createCondition.param("warning")).isNotNull();
    assertThat(createCondition.param("error")).isNotNull();
    assertThat(createCondition.param("period")).isNotNull();
    assertThat(createCondition.isInternal()).isFalse();

    Action updateCondition = controller.action("update_condition");
    assertThat(updateCondition).isNotNull();
    assertThat(updateCondition.handler()).isNotNull();
    assertThat(updateCondition.since()).isEqualTo("4.3");
    assertThat(updateCondition.isPost()).isTrue();
    assertThat(updateCondition.param("id")).isNotNull();
    assertThat(updateCondition.param("metric")).isNotNull();
    assertThat(updateCondition.param("op")).isNotNull();
    assertThat(updateCondition.param("warning")).isNotNull();
    assertThat(updateCondition.param("error")).isNotNull();
    assertThat(updateCondition.param("period")).isNotNull();
    assertThat(updateCondition.isInternal()).isFalse();

    Action appInit = controller.action("app");
    assertThat(appInit.isInternal()).isTrue();
  }

  @Test
  public void copy_nominal() throws Exception {
    String name = "Copied QG";
    when(qGates.copy(24L, name)).thenReturn(new QualityGateDto().setId(42L).setName(name));
    tester.newPostRequest("api/qualitygates", "copy").setParam("id", "24").setParam("name", name).execute()
      .assertJson("{\"id\":42,\"name\":\"Copied QG\"}");
  }

  @Test
  public void set_as_default_nominal() throws Exception {
    Long id = 42L;
    tester.newPostRequest("api/qualitygates", "set_as_default").setParam("id", id.toString()).execute()
      .assertNoContent();
    verify(qGates).setDefault(id);
  }

  @Test
  public void unset_default_nominal() throws Exception {
    tester.newPostRequest("api/qualitygates", "unset_default").execute()
      .assertNoContent();
    verify(qGates).setDefault(null);
  }

  @Test
  public void destroy_nominal() throws Exception {
    Long id = 42L;
    tester.newPostRequest("api/qualitygates", "destroy").setParam("id", id.toString()).execute()
      .assertNoContent();
  }

  @Test(expected = IllegalArgumentException.class)
  public void destroy_without_id() throws Exception {
    tester.newPostRequest("api/qualitygates", "destroy").execute();
  }

  @Test(expected = BadRequestException.class)
  public void destroy_with_invalid_id() throws Exception {
    tester.newPostRequest("api/qualitygates", "destroy").setParam("id", "polop").execute();
  }

  @Test
  public void search_with_query() throws Exception {
    long gateId = 12345L;
    Association assoc = mock(Association.class);
    when(assoc.hasMoreResults()).thenReturn(true);
    List<ProjectQgateAssociation> projects = ImmutableList.of(
      new ProjectQgateAssociation().setId(42L).setName("Project One").setMember(false),
      new ProjectQgateAssociation().setId(24L).setName("Project Two").setMember(true));
    when(assoc.projects()).thenReturn(projects);
    when(projectFinder.find(any(ProjectQgateAssociationQuery.class))).thenReturn(assoc);

    tester.newGetRequest("api/qualitygates", "search")
      .setParam("gateId", Long.toString(gateId))
      .setParam("query", "Project")
      .execute()
      .assertJson("{\"more\":true,\"results\":["
        + "{\"id\":42,\"name\":\"Project One\",\"selected\":false},"
        + "{\"id\":24,\"name\":\"Project Two\",\"selected\":true}"
        + "]}");
    ArgumentCaptor<ProjectQgateAssociationQuery> queryCaptor = ArgumentCaptor.forClass(ProjectQgateAssociationQuery.class);
    verify(projectFinder).find(queryCaptor.capture());
    ProjectQgateAssociationQuery query = queryCaptor.getValue();
    assertThat(query.membership()).isEqualTo(ProjectQgateAssociationQuery.ANY);
  }

  @Test
  public void search_nominal() throws Exception {
    long gateId = 12345L;
    Association assoc = mock(Association.class);
    when(assoc.hasMoreResults()).thenReturn(true);
    List<ProjectQgateAssociation> projects = ImmutableList.of(
      new ProjectQgateAssociation().setId(24L).setName("Project Two").setMember(true));
    when(assoc.projects()).thenReturn(projects);
    when(projectFinder.find(any(ProjectQgateAssociationQuery.class))).thenReturn(assoc);

    tester.newGetRequest("api/qualitygates", "search")
      .setParam("gateId", Long.toString(gateId))
      .execute()
      .assertJson("{\"more\":true,\"results\":["
        + "{\"id\":24,\"name\":\"Project Two\",\"selected\":true}"
        + "]}");
    ArgumentCaptor<ProjectQgateAssociationQuery> queryCaptor = ArgumentCaptor.forClass(ProjectQgateAssociationQuery.class);
    verify(projectFinder).find(queryCaptor.capture());
    ProjectQgateAssociationQuery query = queryCaptor.getValue();
    assertThat(query.membership()).isEqualTo(ProjectQgateAssociationQuery.IN);
  }
}
