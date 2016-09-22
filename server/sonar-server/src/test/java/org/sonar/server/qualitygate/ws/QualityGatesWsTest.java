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
package org.sonar.server.qualitygate.ws;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import org.sonar.db.qualitygate.QualityGateConditionDto;
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
      new ListAction(qGates), new ShowAction(qGates), new SearchAction(projectFinder),
      new CreateAction(null, null, null), new CopyAction(qGates), new DestroyAction(qGates), new RenameAction(qGates),
      new SetAsDefaultAction(qGates), new UnsetDefaultAction(qGates),
      new CreateConditionAction(null, null, null), new UpdateConditionAction(null, null, null), new DeleteConditionAction(qGates),
      selectAction, new DeselectAction(qGates, mock(DbClient.class), mock(ComponentFinder.class)), new AppAction(null, null)));
  }

  @Test
  public void define_ws() {
    Controller controller = tester.controller("api/qualitygates");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/qualitygates");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(15);

    Action list = controller.action("list");
    assertThat(list).isNotNull();
    assertThat(list.handler()).isNotNull();
    assertThat(list.since()).isEqualTo("4.3");
    assertThat(list.isPost()).isFalse();
    assertThat(list.isInternal()).isFalse();

    Action show = controller.action("show");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("4.3");
    assertThat(show.isPost()).isFalse();
    assertThat(show.param("id")).isNotNull();
    assertThat(show.isInternal()).isFalse();

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

    Action rename = controller.action("rename");
    assertThat(rename).isNotNull();
    assertThat(rename.handler()).isNotNull();
    assertThat(rename.since()).isEqualTo("4.3");
    assertThat(rename.isPost()).isTrue();
    assertThat(rename.param("id")).isNotNull();
    assertThat(rename.param("name")).isNotNull();
    assertThat(rename.isInternal()).isFalse();

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

    Action deleteCondition = controller.action("delete_condition");
    assertThat(deleteCondition).isNotNull();
    assertThat(deleteCondition.handler()).isNotNull();
    assertThat(deleteCondition.since()).isEqualTo("4.3");
    assertThat(deleteCondition.isPost()).isTrue();
    assertThat(deleteCondition.param("id")).isNotNull();
    assertThat(deleteCondition.isInternal()).isFalse();

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
  public void rename_nominal() throws Exception {
    Long id = 42L;
    String name = "New QG";
    when(qGates.rename(id, name)).thenReturn(new QualityGateDto().setId(id).setName(name));
    tester.newPostRequest("api/qualitygates", "rename").setParam("id", id.toString()).setParam("name", name).execute()
      .assertJson("{\"id\":42,\"name\":\"New QG\"}");
    ;
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
  public void list_nominal() throws Exception {
    when(qGates.list()).thenReturn(Lists.newArrayList(
      new QualityGateDto().setId(42L).setName("Golden"),
      new QualityGateDto().setId(43L).setName("Star"),
      new QualityGateDto().setId(666L).setName("Ninth")));
    tester.newGetRequest("api/qualitygates", "list").execute().assertJson(
      "{\"qualitygates\":[{\"id\":42,\"name\":\"Golden\"},{\"id\":43,\"name\":\"Star\"},{\"id\":666,\"name\":\"Ninth\"}]}");
  }

  @Test
  public void list_with_default() throws Exception {
    QualityGateDto defaultQgate = new QualityGateDto().setId(42L).setName("Golden");
    when(qGates.list()).thenReturn(Lists.newArrayList(
      defaultQgate,
      new QualityGateDto().setId(43L).setName("Star"),
      new QualityGateDto().setId(666L).setName("Ninth")));
    when(qGates.getDefault()).thenReturn(defaultQgate);
    tester.newGetRequest("api/qualitygates", "list").execute().assertJson(
      "{\"qualitygates\":[{\"id\":42,\"name\":\"Golden\"},{\"id\":43,\"name\":\"Star\"},{\"id\":666,\"name\":\"Ninth\"}],\"default\":42}");
  }

  @Test
  public void show_empty() throws Exception {
    long gateId = 12345L;
    when(qGates.get(gateId)).thenReturn(new QualityGateDto().setId(gateId).setName("Golden"));
    tester.newGetRequest("api/qualitygates", "show").setParam("id", Long.toString(gateId)).execute().assertJson(
      "{\"id\":12345,\"name\":\"Golden\"}");
  }

  @Test
  public void show_by_id_nominal() throws Exception {
    long gateId = 12345L;
    when(qGates.get(gateId)).thenReturn(new QualityGateDto().setId(gateId).setName("Golden"));
    when(qGates.listConditions(gateId)).thenReturn(ImmutableList.of(
      new QualityGateConditionDto().setId(1L).setMetricKey("ncloc").setOperator("GT").setErrorThreshold("10000"),
      new QualityGateConditionDto().setId(2L).setMetricKey("new_coverage").setOperator("LT").setWarningThreshold("90").setPeriod(3)));
    tester.newGetRequest("api/qualitygates", "show").setParam("id", Long.toString(gateId)).execute().assertJson(
      "{\"id\":12345,\"name\":\"Golden\",\"conditions\":["
        + "{\"id\":1,\"metric\":\"ncloc\",\"op\":\"GT\",\"error\":\"10000\"},"
        + "{\"id\":2,\"metric\":\"new_coverage\",\"op\":\"LT\",\"warning\":\"90\",\"period\":3}"
        + "]}");
  }

  @Test
  public void show_by_name_nominal() throws Exception {
    long qGateId = 12345L;
    String gateName = "Golden";
    when(qGates.get(gateName)).thenReturn(new QualityGateDto().setId(qGateId).setName(gateName));
    when(qGates.listConditions(qGateId)).thenReturn(ImmutableList.of(
      new QualityGateConditionDto().setId(1L).setMetricKey("ncloc").setOperator("GT").setErrorThreshold("10000"),
      new QualityGateConditionDto().setId(2L).setMetricKey("new_coverage").setOperator("LT").setWarningThreshold("90").setPeriod(3)));
    tester.newGetRequest("api/qualitygates", "show").setParam("name", gateName).execute().assertJson(
      "{\"id\":12345,\"name\":\"Golden\",\"conditions\":["
        + "{\"id\":1,\"metric\":\"ncloc\",\"op\":\"GT\",\"error\":\"10000\"},"
        + "{\"id\":2,\"metric\":\"new_coverage\",\"op\":\"LT\",\"warning\":\"90\",\"period\":3}"
        + "]}");
  }

  @Test(expected = BadRequestException.class)
  public void show_without_parameters() throws Exception {
    tester.newGetRequest("api/qualitygates", "show").execute();
  }

  @Test(expected = BadRequestException.class)
  public void show_with_both_parameters() throws Exception {
    tester.newGetRequest("api/qualitygates", "show").setParam("id", "12345").setParam("name", "Polop").execute();
  }

  @Test
  public void delete_condition_nominal() throws Exception {
    long condId = 12345L;
    tester.newPostRequest("api/qualitygates", "delete_condition")
      .setParam("id", Long.toString(condId))
      .execute()
      .assertNoContent();
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
