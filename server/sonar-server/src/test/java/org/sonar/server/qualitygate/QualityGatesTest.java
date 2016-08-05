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
package org.sonar.server.qualitygate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserSession;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newProjectDto;

@RunWith(MockitoJUnitRunner.class)
public class QualityGatesTest {

  static final long QUALITY_GATE_ID = 42L;
  static final int METRIC_ID = 10;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  DbSession dbSession = mock(DbSession.class);
  DbClient dbClient = mock(DbClient.class);
  QualityGateDao dao = mock(QualityGateDao.class);
  QualityGateConditionDao conditionDao = mock(QualityGateConditionDao.class);
  PropertiesDao propertiesDao = mock(PropertiesDao.class);
  ComponentDao componentDao = mock(ComponentDao.class);
  MetricFinder metricFinder = mock(MetricFinder.class);
  Settings settings = new Settings();

  QualityGates underTest;

  static final String PROJECT_KEY = "SonarQube";
  static final String PROJECT_UUID = Uuids.UUID_EXAMPLE_01;

  UserSession authorizedProfileAdminUserSession = new MockUserSession("gaudol").setName("Olivier").setGlobalPermissions(GlobalPermissions.QUALITY_GATE_ADMIN);
  UserSession authorizedProjectAdminUserSession = new MockUserSession("gaudol").setName("Olivier").addProjectUuidPermissions(UserRole.ADMIN, PROJECT_UUID);
  UserSession unauthorizedUserSession = new MockUserSession("polop").setName("Polop");
  UserSession unauthenticatedUserSession = new AnonymousMockUserSession();

  @Before
  public void initialize() {
    settings.clear();

    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.qualityGateDao()).thenReturn(dao);
    when(dbClient.gateConditionDao()).thenReturn(conditionDao);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.componentDao()).thenReturn(componentDao);

    when(componentDao.selectOrFailById(eq(dbSession), anyLong())).thenReturn(newProjectDto(PROJECT_UUID).setId(1L).setKey(PROJECT_KEY));

    underTest = new QualityGates(dbClient, metricFinder, userSessionRule, settings);

    userSessionRule.set(authorizedProfileAdminUserSession);
  }

  @Test
  public void should_list_qgates() {
    List<QualityGateDto> allQgates = Lists.newArrayList(new QualityGateDto().setName("Gate One"), new QualityGateDto().setName("Gate Two"));
    when(dao.selectAll()).thenReturn(allQgates);
    assertThat(underTest.list()).isEqualTo(allQgates);
  }

  @Test
  public void should_create_qgate() {
    String name = "SG-1";
    QualityGateDto sg1 = underTest.create(name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectByName(name);
    verify(dao).insert(sg1);
    assertThat(underTest.currentUserHasWritePermission()).isTrue();
  }

  @Test(expected = ForbiddenException.class)
  public void should_fail_create_on_anonymous() {
    userSessionRule.set(unauthenticatedUserSession);
    assertThat(underTest.currentUserHasWritePermission()).isFalse();
    underTest.create("polop");
  }

  @Test(expected = ForbiddenException.class)
  public void should_fail_create_on_missing_permission() {
    userSessionRule.set(unauthorizedUserSession);
    underTest.create("polop");
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_on_empty_name() {
    underTest.create("");
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_on_duplicate_name() {
    String name = "SG-1";
    when(dao.selectByName(name)).thenReturn(new QualityGateDto().setName(name).setId(QUALITY_GATE_ID));
    underTest.create(name);
  }

  @Test
  public void should_get_qgate_by_id() {
    long id = QUALITY_GATE_ID;
    final String name = "Golden";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectById(id)).thenReturn(existing);
    assertThat(underTest.get(id)).isEqualTo(existing);
    verify(dao).selectById(id);
  }

  @Test
  public void should_get_qgate_by_name() {
    long id = QUALITY_GATE_ID;
    final String name = "Golden";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectByName(name)).thenReturn(existing);
    assertThat(underTest.get(name)).isEqualTo(existing);
    verify(dao).selectByName(name);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_to_find_qgate_by_name() {
    underTest.get("Does not exist");
  }

  @Test
  public void should_rename_qgate() {
    long id = QUALITY_GATE_ID;
    String name = "SG-1";
    QualityGateDto existing = new QualityGateDto().setId(id).setName("Golden");
    when(dao.selectById(id)).thenReturn(existing);
    QualityGateDto sg1 = underTest.rename(id, name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectById(id);
    verify(dao).selectByName(name);
    verify(dao).update(sg1);
  }

  @Test
  public void should_allow_rename_with_same_name() {
    long id = QUALITY_GATE_ID;
    String name = "SG-1";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectById(id)).thenReturn(existing);
    QualityGateDto sg1 = underTest.rename(id, name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectById(id);
    verify(dao).selectByName(name);
    verify(dao).update(sg1);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_rename_on_inexistent_qgate() {
    underTest.rename(QUALITY_GATE_ID, "Unknown");
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_rename_on_duplicate_name() {
    long id = QUALITY_GATE_ID;
    String name = "SG-1";
    QualityGateDto existing = new QualityGateDto().setId(id).setName("Golden");
    when(dao.selectById(id)).thenReturn(existing);
    when(dao.selectByName(name)).thenReturn(new QualityGateDto().setId(666L).setName(name));
    underTest.rename(id, name);
  }

  @Test
  public void should_select_default_qgate_and_update_settings() {
    long defaultId = QUALITY_GATE_ID;
    String defaultName = "Default Name";
    when(dao.selectById(defaultId)).thenReturn(new QualityGateDto().setId(defaultId).setName(defaultName));

    underTest.setDefault(defaultId);

    verify(dao).selectById(defaultId);
    ArgumentCaptor<PropertyDto> propertyCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).insertProperty(propertyCaptor.capture());

    assertThat(propertyCaptor.getValue().getKey()).isEqualTo("sonar.qualitygate");
    assertThat(propertyCaptor.getValue().getValue()).isEqualTo("42");
    assertThat(settings.getLong("sonar.qualitygate")).isEqualTo(42);
  }

  @Test
  public void should_unset_default_qgate_and_unset_in_settings() {
    settings.setProperty("sonar.qualitygate", 42l);

    underTest.setDefault(null);

    verify(propertiesDao).deleteGlobalProperty("sonar.qualitygate");
    assertThat(settings.hasKey("sonar.qualitygate")).isFalse();
  }

  @Test
  public void should_delete_qgate() {
    long idToDelete = QUALITY_GATE_ID;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(idToDelete)).thenReturn(toDelete);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest.delete(idToDelete);
    verify(dao).selectById(idToDelete);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", dbSession);
    verify(dao).delete(toDelete, dbSession);
  }

  @Test
  public void should_delete_qgate_if_non_default() {
    long idToDelete = QUALITY_GATE_ID;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(idToDelete)).thenReturn(toDelete);
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue("666"));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest.delete(idToDelete);
    verify(dao).selectById(idToDelete);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", dbSession);
    verify(dao).delete(toDelete, dbSession);
  }

  @Test
  public void should_delete_qgate_even_if_default() {
    long idToDelete = QUALITY_GATE_ID;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(idToDelete)).thenReturn(toDelete);
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue("42"));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest.delete(idToDelete);
    verify(dao).selectById(idToDelete);
    verify(propertiesDao).deleteGlobalProperty("sonar.qualitygate", dbSession);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", dbSession);
    verify(dao).delete(toDelete, dbSession);
  }

  @Test
  public void should_return_default_qgate_if_set() {
    String defaultName = "Sonar way";
    long defaultId = QUALITY_GATE_ID;
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue(Long.toString(defaultId)));
    QualityGateDto defaultQgate = new QualityGateDto().setId(defaultId).setName(defaultName);
    when(dao.selectById(defaultId)).thenReturn(defaultQgate);
    assertThat(underTest.getDefault()).isEqualTo(defaultQgate);
  }

  @Test
  public void should_return_null_default_qgate_if_unset() {
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue(""));
    assertThat(underTest.getDefault()).isNull();
  }

  @Test
  public void should_create_warning_condition_without_period() {
    long qGateId = QUALITY_GATE_ID;
    String metricKey = "coverage";
    String operator = "LT";
    String warningThreshold = "90";
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    int metricId = 10;
    Metric coverage = Mockito.spy(CoreMetrics.COVERAGE);
    when(coverage.getId()).thenReturn(metricId);
    when(metricFinder.findByKey(metricKey)).thenReturn(coverage);

    QualityGateConditionDto newCondition = underTest.createCondition(qGateId, metricKey, operator, warningThreshold, null, null);
    assertThat(newCondition.getQualityGateId()).isEqualTo(qGateId);
    assertThat(newCondition.getMetricId()).isEqualTo((long) metricId);
    assertThat(newCondition.getOperator()).isEqualTo(operator);
    assertThat(newCondition.getWarningThreshold()).isEqualTo(warningThreshold);
    assertThat(newCondition.getErrorThreshold()).isNull();
    assertThat(newCondition.getPeriod()).isNull();
    verify(conditionDao).insert(newCondition);
  }

  @Test
  public void should_create_error_condition_with_period() {
    long qGateId = QUALITY_GATE_ID;
    String metricKey = "new_coverage";
    String operator = "LT";
    String errorThreshold = "80";
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    int metricId = 10;
    Metric newCoverage = Mockito.spy(CoreMetrics.NEW_COVERAGE);
    when(newCoverage.getId()).thenReturn(metricId);
    when(metricFinder.findByKey(metricKey)).thenReturn(newCoverage);
    int period = 1;

    QualityGateConditionDto newCondition = underTest.createCondition(qGateId, metricKey, operator, null, errorThreshold, period);
    assertThat(newCondition.getQualityGateId()).isEqualTo(qGateId);
    assertThat(newCondition.getMetricId()).isEqualTo((long) metricId);
    assertThat(newCondition.getMetricKey()).isEqualTo(metricKey);
    assertThat(newCondition.getOperator()).isEqualTo(operator);
    assertThat(newCondition.getWarningThreshold()).isNull();
    assertThat(newCondition.getErrorThreshold()).isEqualTo(errorThreshold);
    assertThat(newCondition.getPeriod()).isEqualTo(period);
    verify(conditionDao).insert(newCondition);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_create_condition_on_missing_metric() {
    long qGateId = QUALITY_GATE_ID;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "new_coverage", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_alert_metric() {
    long qGateId = QUALITY_GATE_ID;
    when(metricFinder.findByKey(anyString())).thenReturn(CoreMetrics.ALERT_STATUS);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "EQ", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_non_data_metric() {
    long qGateId = QUALITY_GATE_ID;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.DATA);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_hidden_metric() {
    long qGateId = QUALITY_GATE_ID;
    final Metric metric = mock(Metric.class);
    when(metric.isHidden()).thenReturn(true);
    when(metric.getType()).thenReturn(ValueType.INT);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_rating_metric() {
    long qGateId = QUALITY_GATE_ID;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.RATING);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_unallowed_operator() {
    long qGateId = QUALITY_GATE_ID;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_missing_thresholds() {
    long qGateId = QUALITY_GATE_ID;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "EQ", null, null, 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_missing_period() {
    long qGateId = QUALITY_GATE_ID;
    final Metric metric = mock(Metric.class);
    when(metric.getKey()).thenReturn("new_coverage");
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "EQ", null, "90", null);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_invalid_period() {
    long qGateId = QUALITY_GATE_ID;
    final Metric metric = mock(Metric.class);
    when(metric.getKey()).thenReturn("new_coverage");
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.createCondition(qGateId, "alert_status", "EQ", null, "90", 6);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_already_exist() throws Exception {
    String metricKey = "coverage";
    addMetric(metricKey, "Coverage");
    when(dao.selectById(QUALITY_GATE_ID)).thenReturn(new QualityGateDto().setId(QUALITY_GATE_ID));
    when(conditionDao.selectForQualityGate(QUALITY_GATE_ID)).thenReturn(
      singletonList(new QualityGateConditionDto().setMetricKey(metricKey).setMetricId(METRIC_ID)));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'Coverage' already exists.");
    underTest.createCondition(QUALITY_GATE_ID, metricKey, "LT", "90", null, null);
  }

  @Test
  public void fail_to_create_condition_when_condition_on_same_metric_and_period_already_exist() throws Exception {
    String metricKey = "new_coverage";
    addMetric(metricKey, "New Coverage");
    when(dao.selectById(QUALITY_GATE_ID)).thenReturn(new QualityGateDto().setId(QUALITY_GATE_ID));

    when(conditionDao.selectForQualityGate(QUALITY_GATE_ID)).thenReturn(
      singletonList(new QualityGateConditionDto().setMetricKey(metricKey).setMetricId(METRIC_ID).setPeriod(1)));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'New Coverage' over leak period already exists.");
    underTest.createCondition(QUALITY_GATE_ID, metricKey, "LT", "90", null, 1);
  }

  @Test
  public void should_update_condition() {
    String metricKey = "new_coverage";
    String operator = "LT";
    String errorThreshold = "80";
    addMetric(metricKey, "New Coverage");

    QualityGateConditionDto condition = insertQualityGateConditionDto(newCondition(metricKey, METRIC_ID));
    when(conditionDao.selectForQualityGate(QUALITY_GATE_ID)).thenReturn(singletonList(condition));

    assertThat(underTest.updateCondition(condition.getId(), metricKey, operator, null, errorThreshold, 1)).isEqualTo(condition);
    verify(conditionDao).update(condition);
  }

  @Test
  public void fail_to_update_condition_when_condition_on_same_metric_already_exist() throws Exception {
    String metricKey = "coverage";
    addMetric(metricKey, "Coverage");
    when(dao.selectById(QUALITY_GATE_ID)).thenReturn(new QualityGateDto().setId(QUALITY_GATE_ID));

    QualityGateConditionDto conditionNotOnLeakPeriod = insertQualityGateConditionDto(newCondition(metricKey, METRIC_ID)).setPeriod(0);
    QualityGateConditionDto conditionOnLeakPeriod = insertQualityGateConditionDto(newCondition(metricKey, METRIC_ID)).setPeriod(1);
    when(conditionDao.selectForQualityGate(QUALITY_GATE_ID)).thenReturn(asList(conditionNotOnLeakPeriod, conditionOnLeakPeriod));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Condition on metric 'Coverage' over leak period already exists.");

    // Update condition not on leak period to be on leak period => will fail as this condition already exist
    underTest.updateCondition(conditionNotOnLeakPeriod.getId(), metricKey, "LT", null, "60", 1);
  }

  @Test
  public void should_list_conditions() {
    long qGateId = QUALITY_GATE_ID;
    long metric1Id = 1L;
    String metric1Key = "polop";
    long metric2Id = 2L;
    String metric2Key = "palap";
    QualityGateConditionDto cond1 = new QualityGateConditionDto().setMetricId(metric1Id);
    QualityGateConditionDto cond2 = new QualityGateConditionDto().setMetricId(metric2Id);
    Collection<QualityGateConditionDto> conditions = ImmutableList.of(cond1, cond2);
    when(conditionDao.selectForQualityGate(qGateId)).thenReturn(conditions);
    Metric metric1 = mock(Metric.class);
    when(metric1.getKey()).thenReturn(metric1Key);
    when(metricFinder.findById((int) metric1Id)).thenReturn(metric1);
    Metric metric2 = mock(Metric.class);
    when(metric2.getKey()).thenReturn(metric2Key);
    when(metricFinder.findById((int) metric2Id)).thenReturn(metric2);
    assertThat(underTest.listConditions(qGateId)).isEqualTo(conditions);
    Iterator<QualityGateConditionDto> iterator = conditions.iterator();
    assertThat(iterator.next().getMetricKey()).isEqualTo(metric1Key);
    assertThat(iterator.next().getMetricKey()).isEqualTo(metric2Key);
  }

  @Test(expected = IllegalStateException.class)
  public void should_do_a_sanity_check_when_listing_conditions() {
    long qGateId = QUALITY_GATE_ID;
    long metric1Id = 1L;
    String metric1Key = "polop";
    long metric2Id = 2L;
    QualityGateConditionDto cond1 = new QualityGateConditionDto().setMetricId(metric1Id);
    QualityGateConditionDto cond2 = new QualityGateConditionDto().setMetricId(metric2Id);
    Collection<QualityGateConditionDto> conditions = ImmutableList.of(cond1, cond2);
    when(conditionDao.selectForQualityGate(qGateId)).thenReturn(conditions);
    Metric metric1 = mock(Metric.class);
    when(metric1.getKey()).thenReturn(metric1Key);
    when(metricFinder.findById((int) metric1Id)).thenReturn(metric1);
    underTest.listConditions(qGateId);
  }

  @Test
  public void should_delete_condition() {
    long idToDelete = QUALITY_GATE_ID;
    QualityGateConditionDto toDelete = new QualityGateConditionDto().setId(idToDelete);
    when(conditionDao.selectById(idToDelete)).thenReturn(toDelete);
    underTest.deleteCondition(idToDelete);
    verify(conditionDao).selectById(idToDelete);
    verify(conditionDao).delete(toDelete);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_delete_condition() {
    underTest.deleteCondition(QUALITY_GATE_ID);
  }

  @Test
  public void should_associate_project() {
    Long qGateId = QUALITY_GATE_ID;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.associateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    ArgumentCaptor<PropertyDto> propertyCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).insertProperty(propertyCaptor.capture());
    PropertyDto property = propertyCaptor.getValue();
    assertThat(property.getKey()).isEqualTo("sonar.qualitygate");
    assertThat(property.getResourceId()).isEqualTo(projectId);
    assertThat(property.getValue()).isEqualTo("42");
  }

  @Test
  public void associate_project_with_project_admin_permission() {
    userSessionRule.set(authorizedProjectAdminUserSession);

    Long qGateId = QUALITY_GATE_ID;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.associateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    ArgumentCaptor<PropertyDto> propertyCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).insertProperty(propertyCaptor.capture());
    PropertyDto property = propertyCaptor.getValue();
    assertThat(property.getKey()).isEqualTo("sonar.qualitygate");
    assertThat(property.getResourceId()).isEqualTo(projectId);
    assertThat(property.getValue()).isEqualTo("42");
  }

  @Test
  public void should_dissociate_project() {
    Long qGateId = QUALITY_GATE_ID;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.dissociateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    verify(propertiesDao).deleteProjectProperty("sonar.qualitygate", projectId);
  }

  @Test
  public void dissociate_project_with_project_admin_permission() {
    userSessionRule.set(authorizedProjectAdminUserSession);

    Long qGateId = QUALITY_GATE_ID;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    underTest.dissociateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    verify(propertiesDao).deleteProjectProperty("sonar.qualitygate", projectId);
  }

  @Test
  public void should_copy_qgate() {
    String name = "Atlantis";
    long sourceId = QUALITY_GATE_ID;
    final long destId = 43L;
    long metric1Id = 1L;
    long metric2Id = 2L;
    QualityGateConditionDto cond1 = new QualityGateConditionDto().setMetricId(metric1Id);
    QualityGateConditionDto cond2 = new QualityGateConditionDto().setMetricId(metric2Id);
    Collection<QualityGateConditionDto> conditions = ImmutableList.of(cond1, cond2);

    when(dao.selectById(sourceId)).thenReturn(new QualityGateDto().setId(sourceId).setName("SG-1"));
    Mockito.doAnswer(invocation -> {
      ((QualityGateDto) invocation.getArguments()[1]).setId(destId);
      return null;
    }).when(dao).insert(eq(dbSession), any(QualityGateDto.class));
    when(conditionDao.selectForQualityGate(anyLong(), eq(dbSession))).thenReturn(conditions);
    QualityGateDto atlantis = underTest.copy(sourceId, name);
    assertThat(atlantis.getName()).isEqualTo(name);
    verify(dao).selectByName(name);
    verify(dao).insert(dbSession, atlantis);
    verify(conditionDao).selectForQualityGate(anyLong(), eq(dbSession));
    verify(conditionDao, times(conditions.size())).insert(any(QualityGateConditionDto.class), eq(dbSession));
  }

  @Test
  public void should_list_gate_metrics() {
    Metric dataMetric = mock(Metric.class);
    when(dataMetric.isDataType()).thenReturn(true);
    Metric hiddenMetric = mock(Metric.class);
    when(hiddenMetric.isHidden()).thenReturn(true);
    Metric nullHiddenMetric = mock(Metric.class);
    when(nullHiddenMetric.isHidden()).thenReturn(null);
    Metric alertMetric = CoreMetrics.ALERT_STATUS;
    Metric ratingMetric = mock(Metric.class);
    when(ratingMetric.getType()).thenReturn(ValueType.RATING);
    Metric classicMetric = mock(Metric.class);
    when(classicMetric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findAll()).thenReturn(ImmutableList.of(
      dataMetric, hiddenMetric, nullHiddenMetric, alertMetric, ratingMetric, classicMetric));
    assertThat(underTest.gateMetrics()).hasSize(3).containsOnly(classicMetric, hiddenMetric, nullHiddenMetric);
  }

  private Metric addMetric(String metricKey, String metricName) {
    Metric metric = Mockito.spy(CoreMetrics.COVERAGE);
    when(metric.getId()).thenReturn(METRIC_ID);
    when(metric.getName()).thenReturn(metricName);
    when(metricFinder.findByKey(metricKey)).thenReturn(metric);
    return metric;
  }

  private QualityGateConditionDto newCondition(String metricKey, int metricId) {
    return new QualityGateConditionDto()
      .setId(RandomUtils.nextLong())
      .setMetricKey(metricKey)
      .setMetricId(metricId)
      .setQualityGateId(QUALITY_GATE_ID)
      .setOperator("GT")
      .setWarningThreshold(RandomStringUtils.randomAlphanumeric(15))
      .setErrorThreshold(RandomStringUtils.randomAlphanumeric(15))
      .setPeriod(RandomUtils.nextBoolean() ? 1 : null);
  }

  private QualityGateConditionDto insertQualityGateConditionDto(QualityGateConditionDto conditionDto) {
    when(conditionDao.selectById(conditionDto.getId())).thenReturn(conditionDto);
    return conditionDto;
  }

}
