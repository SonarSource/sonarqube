/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.elasticsearch.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WsTester;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualitygate.QualityGates;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QualityGatesWsTest {

  @Mock
  private QualityGates qGates;

  WsTester tester;

  @Before
  public void setUp() {
    tester = new WsTester(new QualityGatesWs(qGates));
  }

  @Test
  public void define_ws() throws Exception {
    WebService.Controller controller = tester.controller("api/qualitygates");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/qualitygates");
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(10);

    WebService.Action list = controller.action("list");
    assertThat(list).isNotNull();
    assertThat(list.handler()).isNotNull();
    assertThat(list.since()).isEqualTo("4.3");
    assertThat(list.isPost()).isFalse();
    assertThat(list.isPrivate()).isFalse();

    WebService.Action show = controller.action("show");
    assertThat(show).isNotNull();
    assertThat(show.handler()).isNotNull();
    assertThat(show.since()).isEqualTo("4.3");
    assertThat(show.isPost()).isFalse();
    assertThat(show.param("id")).isNotNull();
    assertThat(show.isPrivate()).isFalse();

    WebService.Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.handler()).isNotNull();
    assertThat(create.since()).isEqualTo("4.3");
    assertThat(create.isPost()).isTrue();
    assertThat(create.param("name")).isNotNull();
    assertThat(create.isPrivate()).isFalse();

    WebService.Action destroy = controller.action("destroy");
    assertThat(destroy).isNotNull();
    assertThat(destroy.handler()).isNotNull();
    assertThat(destroy.since()).isEqualTo("4.3");
    assertThat(destroy.isPost()).isTrue();
    assertThat(destroy.param("id")).isNotNull();
    assertThat(destroy.isPrivate()).isFalse();

    WebService.Action rename = controller.action("rename");
    assertThat(rename).isNotNull();
    assertThat(rename.handler()).isNotNull();
    assertThat(rename.since()).isEqualTo("4.3");
    assertThat(rename.isPost()).isTrue();
    assertThat(rename.param("id")).isNotNull();
    assertThat(rename.param("name")).isNotNull();
    assertThat(rename.isPrivate()).isFalse();

    WebService.Action setDefault = controller.action("set_as_default");
    assertThat(setDefault).isNotNull();
    assertThat(setDefault.handler()).isNotNull();
    assertThat(setDefault.since()).isEqualTo("4.3");
    assertThat(setDefault.isPost()).isTrue();
    assertThat(setDefault.param("id")).isNotNull();
    assertThat(setDefault.isPrivate()).isFalse();

    WebService.Action unsetDefault = controller.action("unset_default");
    assertThat(unsetDefault).isNotNull();
    assertThat(unsetDefault.handler()).isNotNull();
    assertThat(unsetDefault.since()).isEqualTo("4.3");
    assertThat(unsetDefault.isPost()).isTrue();
    assertThat(unsetDefault.isPrivate()).isFalse();

    WebService.Action createCondition = controller.action("create_condition");
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
    assertThat(createCondition.isPrivate()).isFalse();

    WebService.Action updateCondition = controller.action("update_condition");
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
    assertThat(updateCondition.isPrivate()).isFalse();

    WebService.Action deleteCondition = controller.action("delete_condition");
    assertThat(deleteCondition).isNotNull();
    assertThat(deleteCondition.handler()).isNotNull();
    assertThat(deleteCondition.since()).isEqualTo("4.3");
    assertThat(deleteCondition.isPost()).isTrue();
    assertThat(deleteCondition.param("id")).isNotNull();
    assertThat(deleteCondition.isPrivate()).isFalse();

  }

  @Test
  public void create_nominal() throws Exception {
    String name = "New QG";
    when(qGates.create(name)).thenReturn(new QualityGateDto().setId(42L).setName(name));
    tester.newRequest("create").setParam("name", name).execute()
      .assertJson("{'id':42,'name':'New QG'}");
  }

  @Test(expected = IllegalArgumentException.class)
  public void create_with_missing_name() throws Exception {
    tester.newRequest("create").execute();
  }

  @Test(expected = BadRequestException.class)
  public void create_with_duplicate_name() throws Exception {
    String name = "New QG";
    when(qGates.create(name)).thenThrow(BadRequestException.of("Name is already used"));
    tester.newRequest("create").setParam("name", name).execute();
  }

  @Test
  public void rename_nominal() throws Exception {
    Long id = 42L;
    String name = "New QG";
    when(qGates.rename(id, name)).thenReturn(new QualityGateDto().setId(id).setName(name));
    tester.newRequest("rename").setParam("id", id.toString()).setParam("name", name).execute()
      .assertNoContent();
  }

  @Test
  public void set_as_default_nominal() throws Exception {
    Long id = 42L;
    tester.newRequest("set_as_default").setParam("id", id.toString()).execute()
      .assertNoContent();
    verify(qGates).setDefault(id);
  }

  @Test
  public void unset_default_nominal() throws Exception {
    tester.newRequest("unset_default").execute()
      .assertNoContent();
    verify(qGates).setDefault(null);
  }

  @Test
  public void destroy_nominal() throws Exception {
    Long id = 42L;
    tester.newRequest("destroy").setParam("id", id.toString()).execute()
      .assertNoContent();
  }

  @Test(expected = IllegalArgumentException.class)
  public void destroy_without_id() throws Exception {
    tester.newRequest("destroy").execute();
  }

  @Test(expected = BadRequestException.class)
  public void destroy_with_invalid_id() throws Exception {
    tester.newRequest("destroy").setParam("id", "polop").execute();
  }

  @Test
  public void list_nominal() throws Exception {
    when(qGates.list()).thenReturn(Lists.newArrayList(
      new QualityGateDto().setId(42L).setName("Golden"),
      new QualityGateDto().setId(43L).setName("Star"),
      new QualityGateDto().setId(666L).setName("Ninth")
    ));
    tester.newRequest("list").execute().assertJson(
        "{'qualitygates':[{'id':42,'name':'Golden'},{'id':43,'name':'Star'},{'id':666,'name':'Ninth'}]}");
  }

  @Test
  public void list_with_default() throws Exception {
    QualityGateDto defaultQgate = new QualityGateDto().setId(42L).setName("Golden");
    when(qGates.list()).thenReturn(Lists.newArrayList(
      defaultQgate,
      new QualityGateDto().setId(43L).setName("Star"),
      new QualityGateDto().setId(666L).setName("Ninth")
    ));
    when(qGates.getDefault()).thenReturn(defaultQgate);
    tester.newRequest("list").execute().assertJson(
        "{'qualitygates':[{'id':42,'name':'Golden'},{'id':43,'name':'Star'},{'id':666,'name':'Ninth'}],'default':42}");
  }

  @Test
  public void show_empty() throws Exception {
    long gateId = 12345L;
    when(qGates.get(gateId)).thenReturn(new QualityGateDto().setId(gateId).setName("Golden"));
    tester.newRequest("show").setParam("id", Long.toString(gateId)).execute().assertJson(
      "{'id':12345,'name':'Golden'}");
  }

  @Test
  public void show_nominal() throws Exception {
    long gateId = 12345L;
    when(qGates.get(gateId)).thenReturn(new QualityGateDto().setId(gateId).setName("Golden"));
    when(qGates.listConditions(gateId)).thenReturn(ImmutableList.of(
        new QualityGateConditionDto().setId(1L).setMetricKey("ncloc").setOperator("GT").setErrorThreshold("10000"),
        new QualityGateConditionDto().setId(2L).setMetricKey("new_coverage").setOperator("LT").setWarningThreshold("90").setPeriod(3)
    ));
    tester.newRequest("show").setParam("id", Long.toString(gateId)).execute().assertJson(
      "{'id':12345,'name':'Golden','conditions':["
        + "{'id':1,'metric':'ncloc','op':'GT','error':'10000'},"
        + "{'id':2,'metric':'new_coverage','op':'LT','warning':'90','error':'80','period':3}"
    + "]}");
  }

  @Test
  public void create_condition_nominal() throws Exception {
    long qGateId = 42L;
    String metricKey = "coverage";
    String operator = "LT";
    String warningThreshold = "80";
    String errorThreshold = "75";
    when(qGates.createCondition(qGateId, metricKey, operator, warningThreshold, errorThreshold, null))
      .thenReturn(new QualityGateConditionDto().setId(12345L).setQualityGateId(qGateId).setMetricId(10).setMetricKey(metricKey)
        .setOperator(operator).setWarningThreshold(warningThreshold).setErrorThreshold(errorThreshold));
    tester.newRequest("create_condition")
      .setParam("gateId", Long.toString(qGateId))
      .setParam("metric", metricKey)
      .setParam("op", operator)
      .setParam("warning", warningThreshold)
      .setParam("error", errorThreshold)
      .execute()
      .assertJson("{'id':12345,'metric':'coverage','op':'LT','warning':'80','error':'75'}");
  }

  @Test
  public void update_condition_nominal() throws Exception {
    long condId = 12345L;
    String metricKey = "coverage";
    String operator = "LT";
    String warningThreshold = "80";
    String errorThreshold = "75";
    when(qGates.updateCondition(condId, metricKey, operator, warningThreshold, errorThreshold, null))
      .thenReturn(new QualityGateConditionDto().setId(condId).setMetricId(10).setMetricKey(metricKey)
        .setOperator(operator).setWarningThreshold(warningThreshold).setErrorThreshold(errorThreshold));
    tester.newRequest("update_condition")
      .setParam("id", Long.toString(condId))
      .setParam("metric", metricKey)
      .setParam("op", operator)
      .setParam("warning", warningThreshold)
      .setParam("error", errorThreshold)
      .execute()
      .assertJson("{'id':12345,'metric':'coverage','op':'LT','warning':'80','error':'75'}");
  }

  @Test
  public void delete_condition_nominal() throws Exception {
    long condId = 12345L;
    tester.newRequest("delete_condition")
      .setParam("id", Long.toString(condId))
      .execute()
      .assertNoContent();
  }
}
