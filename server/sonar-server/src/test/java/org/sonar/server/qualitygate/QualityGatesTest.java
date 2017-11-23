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
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
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

public class QualityGatesTest {

  private static final long QUALITY_GATE_ID = 42L;
  private static final String PROJECT_KEY = "SonarQube";
  private static final String PROJECT_UUID = Uuids.UUID_EXAMPLE_01;
  private static final String ORG_UUID = "ORG_UUID";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private TestDefaultOrganizationProvider organizationProvider = TestDefaultOrganizationProvider.fromUuid(ORG_UUID);
  private DbSession dbSession = mock(DbSession.class);
  private DbClient dbClient = mock(DbClient.class);
  private QualityGateDao dao = mock(QualityGateDao.class);
  private QualityGateConditionDao conditionDao = mock(QualityGateConditionDao.class);
  private PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private ComponentDao componentDao = mock(ComponentDao.class);
  private QualityGates underTest;

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.qualityGateDao()).thenReturn(dao);
    when(dbClient.gateConditionDao()).thenReturn(conditionDao);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.componentDao()).thenReturn(componentDao);

    when(componentDao.selectOrFailById(eq(dbSession), anyLong())).thenReturn(
      newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), PROJECT_UUID).setId(1L).setDbKey(PROJECT_KEY));

    underTest = new QualityGates(dbClient, userSession, organizationProvider);

    userSession.logIn().addPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, organizationProvider.get().getUuid());
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

}
