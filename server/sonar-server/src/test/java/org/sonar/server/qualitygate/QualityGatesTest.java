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
package org.sonar.server.qualitygate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.api.measures.MetricFinder;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

@RunWith(MockitoJUnitRunner.class)
public class QualityGatesTest {

  private static final long QUALITY_GATE_ID = 42L;
  private static final String PROJECT_KEY = "SonarQube";
  private static final String PROJECT_UUID = Uuids.UUID_EXAMPLE_01;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private DbSession dbSession = mock(DbSession.class);
  private DbClient dbClient = mock(DbClient.class);
  private QualityGateDao dao = mock(QualityGateDao.class);
  private QualityGateConditionDao conditionDao = mock(QualityGateConditionDao.class);
  private PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private ComponentDao componentDao = mock(ComponentDao.class);
  private MetricFinder metricFinder = mock(MetricFinder.class);
  private QualityGates underTest;

  @Before
  public void initialize() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.qualityGateDao()).thenReturn(dao);
    when(dbClient.gateConditionDao()).thenReturn(conditionDao);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.componentDao()).thenReturn(componentDao);

    when(componentDao.selectOrFailById(eq(dbSession), anyLong())).thenReturn(
      newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), PROJECT_UUID).setId(1L).setKey(PROJECT_KEY));

    underTest = new QualityGates(dbClient, metricFinder, userSession);

    userSession.logIn().setSystemAdministrator();
  }

  @Test
  public void should_list_qgates() {
    List<QualityGateDto> allQgates = Lists.newArrayList(new QualityGateDto().setName("Gate One"), new QualityGateDto().setName("Gate Two"));
    when(dao.selectAll(dbSession)).thenReturn(allQgates);
    assertThat(underTest.list()).isEqualTo(allQgates);
  }

  @Test
  public void should_get_qgate_by_id() {
    long id = QUALITY_GATE_ID;
    final String name = "Golden";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectById(dbSession, id)).thenReturn(existing);
    assertThat(underTest.get(id)).isEqualTo(existing);
    verify(dao).selectById(dbSession, id);
  }

  @Test
  public void should_get_qgate_by_name() {
    long id = QUALITY_GATE_ID;
    final String name = "Golden";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectByName(dbSession, name)).thenReturn(existing);
    assertThat(underTest.get(name)).isEqualTo(existing);
    verify(dao).selectByName(dbSession, name);
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
    when(dao.selectById(dbSession, id)).thenReturn(existing);
    QualityGateDto sg1 = underTest.rename(id, name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectById(dbSession, id);
    verify(dao).selectByName(dbSession, name);
    verify(dao).update(sg1, dbSession);
  }

  @Test
  public void should_allow_rename_with_same_name() {
    long id = QUALITY_GATE_ID;
    String name = "SG-1";
    QualityGateDto existing = new QualityGateDto().setId(id).setName(name);
    when(dao.selectById(dbSession, id)).thenReturn(existing);
    QualityGateDto sg1 = underTest.rename(id, name);
    assertThat(sg1.getName()).isEqualTo(name);
    verify(dao).selectById(dbSession, id);
    verify(dao).selectByName(dbSession, name);
    verify(dao).update(sg1, dbSession);
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
    when(dao.selectById(dbSession, id)).thenReturn(existing);
    when(dao.selectByName(dbSession, name)).thenReturn(new QualityGateDto().setId(666L).setName(name));
    underTest.rename(id, name);
  }

  @Test
  public void should_select_default_qgate() {
    long defaultId = QUALITY_GATE_ID;
    String defaultName = "Default Name";
    when(dao.selectById(dbSession, defaultId)).thenReturn(new QualityGateDto().setId(defaultId).setName(defaultName));

    underTest.setDefault(defaultId);

    verify(dao).selectById(dbSession, defaultId);
    ArgumentCaptor<PropertyDto> propertyCaptor = ArgumentCaptor.forClass(PropertyDto.class);
    verify(propertiesDao).saveProperty(any(DbSession.class), propertyCaptor.capture());

    assertThat(propertyCaptor.getValue().getKey()).isEqualTo("sonar.qualitygate");
    assertThat(propertyCaptor.getValue().getValue()).isEqualTo("42");
  }

  @Test
  public void should_delete_qgate() {
    long idToDelete = QUALITY_GATE_ID;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(dbSession, idToDelete)).thenReturn(toDelete);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest.delete(idToDelete);
    verify(dao).selectById(dbSession, idToDelete);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", dbSession);
    verify(dao).delete(toDelete, dbSession);
  }

  @Test
  public void should_delete_qgate_if_non_default() {
    long idToDelete = QUALITY_GATE_ID;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(dbSession, idToDelete)).thenReturn(toDelete);
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue("666"));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest.delete(idToDelete);
    verify(dao).selectById(dbSession, idToDelete);
    verify(propertiesDao).deleteProjectProperties("sonar.qualitygate", "42", dbSession);
    verify(dao).delete(toDelete, dbSession);
  }

  @Test
  public void should_delete_qgate_even_if_default() {
    long idToDelete = QUALITY_GATE_ID;
    String name = "To Delete";
    QualityGateDto toDelete = new QualityGateDto().setId(idToDelete).setName(name);
    when(dao.selectById(dbSession, idToDelete)).thenReturn(toDelete);
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue("42"));
    when(dbClient.openSession(false)).thenReturn(dbSession);
    underTest.delete(idToDelete);
    verify(dao).selectById(dbSession, idToDelete);
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
    when(dao.selectById(dbSession, defaultId)).thenReturn(defaultQgate);
    assertThat(underTest.getDefault()).isEqualTo(defaultQgate);
  }

  @Test
  public void should_return_null_default_qgate_if_unset() {
    when(propertiesDao.selectGlobalProperty("sonar.qualitygate")).thenReturn(new PropertyDto().setValue(""));
    assertThat(underTest.getDefault()).isNull();
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
    when(conditionDao.selectForQualityGate(dbSession, qGateId)).thenReturn(conditions);
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
    when(conditionDao.selectForQualityGate(dbSession, qGateId)).thenReturn(conditions);
    Metric metric1 = mock(Metric.class);
    when(metric1.getKey()).thenReturn(metric1Key);
    when(metricFinder.findById((int) metric1Id)).thenReturn(metric1);
    underTest.listConditions(qGateId);
  }

  @Test
  public void should_delete_condition() {
    long idToDelete = QUALITY_GATE_ID;
    QualityGateConditionDto toDelete = new QualityGateConditionDto().setId(idToDelete);
    when(conditionDao.selectById(idToDelete, dbSession)).thenReturn(toDelete);
    underTest.deleteCondition(idToDelete);
    verify(conditionDao).selectById(idToDelete, dbSession);
    verify(conditionDao).delete(toDelete, dbSession);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_delete_condition() {
    underTest.deleteCondition(QUALITY_GATE_ID);
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

    when(dao.selectById(dbSession, sourceId)).thenReturn(new QualityGateDto().setId(sourceId).setName("SG-1"));
    Mockito.doAnswer(invocation -> {
      ((QualityGateDto) invocation.getArguments()[1]).setId(destId);
      return null;
    }).when(dao).insert(eq(dbSession), any(QualityGateDto.class));
    when(conditionDao.selectForQualityGate(eq(dbSession), anyLong())).thenReturn(conditions);
    QualityGateDto atlantis = underTest.copy(sourceId, name);
    assertThat(atlantis.getName()).isEqualTo(name);
    verify(dao).selectByName(dbSession, name);
    verify(dao).insert(dbSession, atlantis);
    verify(conditionDao).selectForQualityGate(eq(dbSession), anyLong());
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
  }

}
