/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.qualitygate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualitygate.db.QualityGateConditionDao;
import org.sonar.core.qualitygate.db.QualityGateConditionDto;
import org.sonar.core.qualitygate.db.QualityGateDao;
import org.sonar.core.qualitygate.db.QualityGateDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserSessionTestUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QualityGatesTest {

  @Mock
  DbSession session;

  @Mock
  QualityGateDao dao;

  @Mock
  QualityGateConditionDao conditionDao;

  @Mock
  MetricFinder metricFinder;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  ComponentDao componentDao;

  @Mock
  MyBatis myBatis;

  QualityGates qGates;

  static final String PROJECT_KEY = "SonarQube";

  UserSession authorizedProfileAdminUserSession = MockUserSession.create().setLogin("gaudol").setName("Olivier").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession authorizedProjectAdminUserSession = MockUserSession.create().setLogin("gaudol").setName("Olivier").addProjectPermissions(UserRole.ADMIN, PROJECT_KEY);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("polop").setName("Polop");
  UserSession unauthenticatedUserSession = MockUserSession.create();

  @Before
  public void initialize() {
    when(componentDao.getById(anyLong(), eq(session))).thenReturn(new ComponentDto().setId(1L).setKey(PROJECT_KEY));

    when(myBatis.openSession(false)).thenReturn(session);
    qGates = new QualityGates(dao, conditionDao, metricFinder, propertiesDao, componentDao, myBatis);
    UserSessionTestUtils.setUserSession(authorizedProfileAdminUserSession);
  }

  @Test
  public void should_list_qgates() throws Exception {
    List<QualityGateDto> allQgates = Lists.newArrayList(new QualityGateDto().setName("Gate One"), new QualityGateDto().setName("Gate Two"));
    when(dao.selectAll()).thenReturn(allQgates);
    assertThat(qGates.list()).isEqualTo(allQgates);
  }

  @Test
  public void should_create_qgate() throws Exception {
    String name = "SG-1";
    QualityGateDto sg1 = qGates.create(name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectByName(name);
    verify(dao).insert(sg1);
    assertThat(qGates.currentUserHasWritePermission()).isTrue();
  }

  @Test(expected = ForbiddenException.class)
  public void should_fail_create_on_anonymous() throws Exception {
    UserSessionTestUtils.setUserSession(unauthenticatedUserSession);
    assertThat(qGates.currentUserHasWritePermission()).isFalse();
    qGates.create("polop");
  }

  @Test(expected = ForbiddenException.class)
  public void should_fail_create_on_missing_permission() throws Exception {
    UserSessionTestUtils.setUserSession(unauthorizedUserSession);
    qGates.create("polop");
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_on_empty_name() throws Exception {
    qGates.create("");
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_on_duplicate_name() throws Exception {
    String name = "SG-1";
    when(dao.selectByName(name)).thenReturn(new QualityGateDto().setName(name).setId(42L));
    qGates.create(name);
  }

  @Test
  public void should_get_qgate_by_id() throws Exception {
    long id = 42L;
    final String name = "Golden";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectById(id)).thenReturn(existing);
    assertThat(qGates.get(id)).isEqualTo(existing);
    verify(dao).selectById(id);
  }

  @Test
  public void should_get_qgate_by_name() throws Exception {
    long id = 42L;
    final String name = "Golden";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectByName(name)).thenReturn(existing);
    assertThat(qGates.get(name)).isEqualTo(existing);
    verify(dao).selectByName(name);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_to_find_qgate_by_name() throws Exception {
    qGates.get("Does not exist");
  }

  @Test
  public void should_rename_qgate() throws Exception {
    long id = 42L;
    String name = "SG-1";
    QualityGateDto existing = new QualityGateDto().setId(id).setName("Golden");
    when(dao.selectById(id)).thenReturn(existing);
    QualityGateDto sg1 = qGates.rename(id, name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectById(id);
    verify(dao).selectByName(name);
    verify(dao).update(sg1);
  }

  @Test
  public void should_allow_rename_with_same_name() throws Exception {
    long id = 42L;
    String name = "SG-1";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectById(id)).thenReturn(existing);
    QualityGateDto sg1 = qGates.rename(id, name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectById(id);
    verify(dao).selectByName(name);
    verify(dao).update(sg1);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_rename_on_inexistent_qgate() throws Exception {
    qGates.rename(42L, "Unknown");
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_rename_on_duplicate_name() throws Exception {
    long id = 42L;
    String name = "SG-1";
    QualityGateDto existing = new QualityGateDto().setId(id).setName("Golden");
    when(dao.selectById(id)).thenReturn(existing);
    when(dao.selectByName(name)).thenReturn(new QualityGateDto().setId(666L).setName(name));
    qGates.rename(id, name);
  }

  @Test
  public void should_select_default_qgate() throws Exception {
    long defaultId = 42L;
    String defaultName = "Default Name";
    when(dao.selectById(defaultId)).thenReturn(new QualityGateDto().setId(defaultId).setName(defaultName));
    qGates.setDefault(defaultId);
    verify(dao).selectById(defaultId);
    ArgumentCaptor<PropertyDto> propertyCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(propertyCaptor.capture());
    assertThat(propertyCaptor.getValue().getKey()).isEqualTo("sonar.qualitygate");
    assertThat(propertyCaptor.getValue().getValue()).isEqualTo("42");
  }

  @Test
  public void should_unset_default_qgate() throws Exception {
    qGates.setDefault(null);
    verify(propertiesDao).deleteGlobalProperty("sonar.qualitygate");
  }

  @Test
  public void should_delete_qgate() throws Exception {
    long idToDelete = 42L;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(idToDelete)).thenReturn(toDelete);
    DbSession session = mock(DbSession.class);
    when(myBatis.openSession(false)).thenReturn(session);
    qGates.delete(idToDelete);
    verify(dao).selectById(idToDelete);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", session);
    verify(dao).delete(toDelete, session);
  }

  @Test
  public void should_delete_qgate_if_non_default() throws Exception {
    long idToDelete = 42L;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(idToDelete)).thenReturn(toDelete);
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue("666"));
    DbSession session = mock(DbSession.class);
    when(myBatis.openSession(false)).thenReturn(session);
    qGates.delete(idToDelete);
    verify(dao).selectById(idToDelete);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", session);
    verify(dao).delete(toDelete, session);
  }

  @Test
  public void should_delete_qgate_even_if_default() throws Exception {
    long idToDelete = 42L;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(idToDelete)).thenReturn(toDelete);
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue("42"));
    DbSession session = mock(DbSession.class);
    when(myBatis.openSession(false)).thenReturn(session);
    qGates.delete(idToDelete);
    verify(dao).selectById(idToDelete);
    verify(propertiesDao).deleteGlobalProperty("sonar.qualitygate", session);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", session);
    verify(dao).delete(toDelete, session);
  }

  @Test
  public void should_return_default_qgate_if_set() throws Exception {
    String defaultName = "Sonar way";
    long defaultId = 42L;
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue(Long.toString(defaultId)));
    QualityGateDto defaultQgate = new QualityGateDto().setId(defaultId).setName(defaultName);
    when(dao.selectById(defaultId)).thenReturn(defaultQgate);
    assertThat(qGates.getDefault()).isEqualTo(defaultQgate);
  }

  @Test
  public void should_return_null_default_qgate_if_unset() throws Exception {
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue(""));
    assertThat(qGates.getDefault()).isNull();
  }

  @Test
  public void should_create_warning_condition_without_period() {
    long qGateId = 42L;
    String metricKey = "coverage";
    String operator = "LT";
    String warningThreshold = "90";
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    int metricId = 10;
    Metric coverage = Mockito.spy(CoreMetrics.COVERAGE);
    when(coverage.getId()).thenReturn(metricId);
    when(metricFinder.findByKey(metricKey)).thenReturn(coverage);

    QualityGateConditionDto newCondition = qGates.createCondition(qGateId, metricKey, operator, warningThreshold, null, null);
    assertThat(newCondition.getQualityGateId()).isEqualTo(qGateId);
    assertThat(newCondition.getMetricId()).isEqualTo((long)metricId);
    assertThat(newCondition.getOperator()).isEqualTo(operator);
    assertThat(newCondition.getWarningThreshold()).isEqualTo(warningThreshold);
    assertThat(newCondition.getErrorThreshold()).isNull();
    assertThat(newCondition.getPeriod()).isNull();
    verify(conditionDao).insert(newCondition);
  }

  @Test
  public void should_create_error_condition_with_period() {
    long qGateId = 42L;
    String metricKey = "new_coverage";
    String operator = "LT";
    String errorThreshold = "80";
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    int metricId = 10;
    Metric newCoverage = Mockito.spy(CoreMetrics.NEW_COVERAGE);
    when(newCoverage.getId()).thenReturn(metricId);
    when(metricFinder.findByKey(metricKey)).thenReturn(newCoverage);
    int period = 2;

    QualityGateConditionDto newCondition = qGates.createCondition(qGateId, metricKey, operator, null, errorThreshold, period);
    assertThat(newCondition.getQualityGateId()).isEqualTo(qGateId);
    assertThat(newCondition.getMetricId()).isEqualTo((long)metricId);
    assertThat(newCondition.getMetricKey()).isEqualTo(metricKey);
    assertThat(newCondition.getOperator()).isEqualTo(operator);
    assertThat(newCondition.getWarningThreshold()).isNull();
    assertThat(newCondition.getErrorThreshold()).isEqualTo(errorThreshold);
    assertThat(newCondition.getPeriod()).isEqualTo(period);
    verify(conditionDao).insert(newCondition);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_create_condition_on_missing_metric() {
    long qGateId = 42L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "new_coverage", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_alert_metric() {
    long qGateId = 42L;
    when(metricFinder.findByKey(anyString())).thenReturn(CoreMetrics.ALERT_STATUS);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "EQ", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_non_data_metric() {
    long qGateId = 42L;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.DATA);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_hidden_metric() {
    long qGateId = 42L;
    final Metric metric = mock(Metric.class);
    when(metric.isHidden()).thenReturn(true);
    when(metric.getType()).thenReturn(ValueType.INT);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_rating_metric() {
    long qGateId = 42L;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.RATING);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_unallowed_operator() {
    long qGateId = 42L;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "LT", null, "80", 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_missing_thresholds() {
    long qGateId = 42L;
    final Metric metric = mock(Metric.class);
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "EQ", null, null, 2);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_missing_period() {
    long qGateId = 42L;
    final Metric metric = mock(Metric.class);
    when(metric.getKey()).thenReturn("new_coverage");
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "EQ", null, "90", null);
  }

  @Test(expected = BadRequestException.class)
  public void should_fail_create_condition_on_invalid_period() {
    long qGateId = 42L;
    final Metric metric = mock(Metric.class);
    when(metric.getKey()).thenReturn("new_coverage");
    when(metric.getType()).thenReturn(ValueType.BOOL);
    when(metricFinder.findByKey(anyString())).thenReturn(metric);
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.createCondition(qGateId, "alert_status", "EQ", null, "90", 6);
  }

  @Test
  public void should_update_condition() {
    long condId = 42L;
    String metricKey = "new_coverage";
    String operator = "LT";
    String errorThreshold = "80";
    final QualityGateConditionDto condition = new QualityGateConditionDto().setId(condId)
      .setMetricId(666L).setOperator("GT").setWarningThreshold("123");
    when(conditionDao.selectById(condId)).thenReturn(condition);
    int metricId = 10;
    Metric newCoverage = Mockito.spy(CoreMetrics.NEW_COVERAGE);
    when(newCoverage.getId()).thenReturn(metricId);
    when(metricFinder.findByKey(metricKey)).thenReturn(newCoverage);
    int period = 2;

    assertThat(qGates.updateCondition(condId, metricKey, operator, null, errorThreshold, period)).isEqualTo(condition);
    assertThat(condition.getId()).isEqualTo(condId);
    assertThat(condition.getMetricId()).isEqualTo((long)metricId);
    assertThat(condition.getMetricKey()).isEqualTo(metricKey);
    assertThat(condition.getOperator()).isEqualTo(operator);
    assertThat(condition.getWarningThreshold()).isNull();
    assertThat(condition.getErrorThreshold()).isEqualTo(errorThreshold);
    assertThat(condition.getPeriod()).isEqualTo(period);
    verify(conditionDao).update(condition);
  }

  @Test
  public void should_list_conditions() throws Exception {
    long qGateId = 42L;
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
    assertThat(qGates.listConditions(qGateId)).isEqualTo(conditions);
    Iterator<QualityGateConditionDto> iterator = conditions.iterator();
    assertThat(iterator.next().getMetricKey()).isEqualTo(metric1Key);
    assertThat(iterator.next().getMetricKey()).isEqualTo(metric2Key);
  }

  @Test(expected = IllegalStateException.class)
  public void should_do_a_sanity_check_when_listing_conditions() throws Exception {
    long qGateId = 42L;
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
    qGates.listConditions(qGateId);
  }

  @Test
  public void should_delete_condition() throws Exception {
    long idToDelete = 42L;
    QualityGateConditionDto toDelete = new QualityGateConditionDto().setId(idToDelete);
    when(conditionDao.selectById(idToDelete)).thenReturn(toDelete);
    qGates.deleteCondition(idToDelete);
    verify(conditionDao).selectById(idToDelete);
    verify(conditionDao).delete(toDelete);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_delete_condition() throws Exception {
    qGates.deleteCondition(42L);
  }

  @Test
  public void should_associate_project() {
    Long qGateId = 42L;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.associateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    ArgumentCaptor<PropertyDto> propertyCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(propertyCaptor.capture());
    PropertyDto property = propertyCaptor.getValue();
    assertThat(property.getKey()).isEqualTo("sonar.qualitygate");
    assertThat(property.getResourceId()).isEqualTo(projectId);
    assertThat(property.getValue()).isEqualTo("42");
  }

  @Test
  public void associate_project_with_project_admin_permission() {
    UserSessionTestUtils.setUserSession(authorizedProjectAdminUserSession);

    Long qGateId = 42L;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.associateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    ArgumentCaptor<PropertyDto> propertyCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).setProperty(propertyCaptor.capture());
    PropertyDto property = propertyCaptor.getValue();
    assertThat(property.getKey()).isEqualTo("sonar.qualitygate");
    assertThat(property.getResourceId()).isEqualTo(projectId);
    assertThat(property.getValue()).isEqualTo("42");
  }

  @Test
  public void should_dissociate_project() {
    Long qGateId = 42L;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.dissociateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    verify(propertiesDao).deleteProjectProperty("sonar.qualitygate", projectId);
  }

  @Test
  public void dissociate_project_with_project_admin_permission() {
    UserSessionTestUtils.setUserSession(authorizedProjectAdminUserSession);

    Long qGateId = 42L;
    Long projectId = 24L;
    when(dao.selectById(qGateId)).thenReturn(new QualityGateDto().setId(qGateId));
    qGates.dissociateProject(qGateId, projectId);
    verify(dao).selectById(qGateId);
    verify(propertiesDao).deleteProjectProperty("sonar.qualitygate", projectId);
  }

  @Test
  public void should_copy_qgate() throws Exception {
    String name = "Atlantis";
    long sourceId = 42L;
    final long destId = 43L;
    long metric1Id = 1L;
    long metric2Id = 2L;
    QualityGateConditionDto cond1 = new QualityGateConditionDto().setMetricId(metric1Id);
    QualityGateConditionDto cond2 = new QualityGateConditionDto().setMetricId(metric2Id);
    Collection<QualityGateConditionDto> conditions = ImmutableList.of(cond1, cond2);

    when(dao.selectById(sourceId)).thenReturn(new QualityGateDto().setId(sourceId).setName("SG-1"));
    DbSession session = mock(DbSession.class);
    when(myBatis.openSession(false)).thenReturn(session);
    Mockito.doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ((QualityGateDto) invocation.getArguments()[0]).setId(destId);
        return null;
      }
    }).when(dao).insert(any(QualityGateDto.class), eq(session));
    when(conditionDao.selectForQualityGate(anyLong(), eq(session))).thenReturn(conditions);
    QualityGateDto atlantis = qGates.copy(sourceId, name);
    assertThat(atlantis.getName()).isEqualTo(name);
    verify(dao).selectByName(name);
    verify(dao).insert(atlantis, session);
    verify(conditionDao).selectForQualityGate(anyLong(), eq(session));
    verify(conditionDao, times(conditions.size())).insert(any(QualityGateConditionDto.class), eq(session));
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
    assertThat(qGates.gateMetrics()).hasSize(3).containsOnly(classicMetric, hiddenMetric, nullHiddenMetric);
  }
}
