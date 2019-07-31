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
package org.sonar.db.qualitygate;

import java.util.Collections;
import java.util.Date;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private QualityGateDbTester qualityGateDbTester = new QualityGateDbTester(db);
  private DbSession dbSession = db.getSession();
  private QualityGateDao underTest = db.getDbClient().qualityGateDao();

  @Test
  public void insert() {
    QualityGateDto newQgate = new QualityGateDto()
      .setUuid(Uuids.createFast())
      .setName("My Quality Gate")
      .setBuiltIn(false)
      .setUpdatedAt(new Date());

    underTest.insert(dbSession, newQgate);
    dbSession.commit();

    QualityGateDto reloaded = underTest.selectById(dbSession, newQgate.getId());
    assertThat(reloaded.getName()).isEqualTo("My Quality Gate");
    assertThat(reloaded.getUuid()).isEqualTo(newQgate.getUuid());
    assertThat(reloaded.isBuiltIn()).isFalse();
    assertThat(reloaded.getCreatedAt()).isNotNull();
    assertThat(reloaded.getUpdatedAt()).isNotNull();
  }

  @Test
  public void associate() {
    QualityGateDto qgate = db.qualityGates().insertQualityGate(db.getDefaultOrganization());
    OrganizationDto org = db.organizations().insert();

    underTest.associate(dbSession, Uuids.createFast(), org, qgate);

    assertThat(underTest.selectByOrganizationAndUuid(dbSession, org, qgate.getUuid())).isNotNull();
    assertThat(underTest.selectByOrganizationAndUuid(dbSession, org, qgate.getUuid()))
      .extracting(QGateWithOrgDto::getId, QGateWithOrgDto::getUuid, QGateWithOrgDto::getOrganizationUuid, QGateWithOrgDto::getName)
      .containsExactly(qgate.getId(), qgate.getUuid(), org.getUuid(), qgate.getName());
  }

  @Test
  public void insert_built_in() {
    underTest.insert(db.getSession(), new QualityGateDto().setName("test").setBuiltIn(true).setUuid(Uuids.createFast()));

    QualityGateDto reloaded = underTest.selectByName(db.getSession(), "test");

    assertThat(reloaded.isBuiltIn()).isTrue();
  }

  @Test
  public void select_all() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    QGateWithOrgDto qualityGate1 = qualityGateDbTester.insertQualityGate(organization1);
    QGateWithOrgDto qualityGate2 = qualityGateDbTester.insertQualityGate(organization1);
    QGateWithOrgDto qualityGateOnOtherOrg = qualityGateDbTester.insertQualityGate(organization2);

    assertThat(underTest.selectAll(dbSession, organization1))
      .extracting(QualityGateDto::getUuid)
      .containsExactlyInAnyOrder(qualityGate1.getUuid(), qualityGate2.getUuid());
  }

  @Test
  public void testSelectByName() {
    insertQualityGates();
    assertThat(underTest.selectByName(dbSession, "Balanced").getName()).isEqualTo("Balanced");
    assertThat(underTest.selectByName(dbSession, "Unknown")).isNull();
  }

  @Test
  public void testSelectById() {
    insertQualityGates();
    assertThat(underTest.selectById(dbSession, underTest.selectByName(dbSession, "Very strict").getId()).getName()).isEqualTo("Very strict");
    assertThat(underTest.selectById(dbSession, -1L)).isNull();
  }

  @Test
  public void testSelectByUuid() {
    insertQualityGates();
    assertThat(underTest.selectByUuid(dbSession, underTest.selectByName(dbSession, "Very strict").getUuid()).getName()).isEqualTo("Very strict");
    assertThat(underTest.selectByUuid(dbSession, "not-existing-uuid")).isNull();
  }

  @Test
  public void select_by_organization_and_uuid() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto otherQualityGate = db.qualityGates().insertQualityGate(otherOrganization);

    assertThat(underTest.selectByOrganizationAndUuid(dbSession, organization, qualityGate.getUuid()).getUuid()).isEqualTo(qualityGate.getUuid());
    assertThat(underTest.selectByOrganizationAndUuid(dbSession, otherOrganization, qualityGate.getUuid())).isNull();
    assertThat(underTest.selectByOrganizationAndUuid(dbSession, organization, otherQualityGate.getUuid())).isNull();
  }

  @Test
  public void select_by_organization_and_name() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate1 = db.qualityGates().insertQualityGate(organization);
    QGateWithOrgDto qualityGate2 = db.qualityGates().insertQualityGate(organization);
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto qualityGate3 = db.qualityGates().insertQualityGate(otherOrganization);

    assertThat(underTest.selectByOrganizationAndName(dbSession, organization, qualityGate1.getName()).getUuid()).isEqualTo(qualityGate1.getUuid());
    assertThat(underTest.selectByOrganizationAndName(dbSession, otherOrganization, qualityGate3.getName()).getUuid()).isEqualTo(qualityGate3.getUuid());
    assertThat(underTest.selectByOrganizationAndName(dbSession, organization, "Unknown")).isNull();
  }

  @Test
  public void select_by_organization_and_id() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate1 = db.qualityGates().insertQualityGate(organization);
    QGateWithOrgDto qualityGate2 = db.qualityGates().insertQualityGate(organization);
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto qualityGate3 = db.qualityGates().insertQualityGate(otherOrganization);

    assertThat(underTest.selectByOrganizationAndId(dbSession, organization, qualityGate1.getId()).getUuid()).isEqualTo(qualityGate1.getUuid());
    assertThat(underTest.selectByOrganizationAndId(dbSession, otherOrganization, qualityGate3.getId()).getUuid()).isEqualTo(qualityGate3.getUuid());
    assertThat(underTest.selectByOrganizationAndId(dbSession, organization, 123L)).isNull();
  }

  @Test
  public void select_default() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = db.qualityGates().insertQualityGate(organization);
    db.getDbClient().organizationDao().setDefaultQualityGate(dbSession, organization, qualityGate);
    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto otherQualityGate = db.qualityGates().insertQualityGate(otherOrganization);
    db.getDbClient().organizationDao().setDefaultQualityGate(dbSession, otherOrganization, otherQualityGate);

    assertThat(underTest.selectDefault(dbSession, organization).getUuid()).isEqualTo(qualityGate.getUuid());
    assertThat(underTest.selectDefault(dbSession, otherOrganization).getUuid()).isEqualTo(otherQualityGate.getUuid());
  }

  @Test
  public void select_by_project_uuid() {
    OrganizationDto organization = db.organizations().insert();

    ComponentDto project = db.components().insertPrivateProject(organization);

    QGateWithOrgDto qualityGate1 = db.qualityGates().insertQualityGate(organization);
    QGateWithOrgDto qualityGate2 = db.qualityGates().insertQualityGate(organization);

    OrganizationDto otherOrganization = db.organizations().insert();
    QGateWithOrgDto qualityGate3 = db.qualityGates().insertQualityGate(otherOrganization);

    db.qualityGates().associateProjectToQualityGate(project, qualityGate1);

    assertThat(underTest.selectByProjectUuid(dbSession, project.uuid()).getUuid()).isEqualTo(qualityGate1.getUuid());
    assertThat(underTest.selectByProjectUuid(dbSession, "not-existing-uuid")).isNull();
  }

  @Test
  public void delete() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = qualityGateDbTester.insertQualityGate(organization);
    QGateWithOrgDto otherQualityGate = qualityGateDbTester.insertQualityGate(organization);

    underTest.delete(qualityGate, dbSession);
    dbSession.commit();

    assertThat(underTest.selectByOrganizationAndUuid(dbSession, organization, qualityGate.getUuid())).isNull();
    assertThat(db.countSql(dbSession, format("select count(*) from org_quality_gates where quality_gate_uuid='%s'", qualityGate.getUuid()))).isZero();
    assertThat(underTest.selectByOrganizationAndUuid(dbSession, organization, otherQualityGate.getUuid())).isNotNull();
    assertThat(db.countSql(dbSession, format("select count(*) from org_quality_gates where quality_gate_uuid='%s'", otherQualityGate.getUuid()))).isEqualTo(1);
  }

  @Test
  public void delete_by_uuids() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate1 = qualityGateDbTester.insertQualityGate(organization);
    QGateWithOrgDto qualityGate2 = qualityGateDbTester.insertQualityGate(organization);

    underTest.deleteByUuids(dbSession, asList(qualityGate1.getUuid(), qualityGate2.getUuid()));
    dbSession.commit();

    assertThat(underTest.selectAll(dbSession, organization).stream())
      .extracting(QualityGateDto::getUuid)
      .doesNotContain(qualityGate1.getUuid(), qualityGate2.getUuid());
  }

  @Test
  public void delete_by_uuids_does_nothing_on_empty_list() {
    int nbOfQualityGates = db.countRowsOfTable(dbSession, "quality_gates");
    underTest.deleteByUuids(dbSession, Collections.emptyList());
    dbSession.commit();

    assertThat(db.countRowsOfTable(dbSession, "quality_gates")).isEqualTo(nbOfQualityGates);
  }

  @Test
  public void deleteOrgQualityGatesByOrganization() {
    OrganizationDto organization = db.organizations().insert();
    qualityGateDbTester.insertQualityGate(organization);
    OrganizationDto otherOrganization = db.organizations().insert();
    qualityGateDbTester.insertQualityGate(otherOrganization);

    underTest.deleteOrgQualityGatesByOrganization(dbSession, organization);
    dbSession.commit();

    assertThat(db.select("select organization_uuid as \"organizationUuid\" from org_quality_gates"))
      .extracting(row -> (String) row.get("organizationUuid"))
      .containsOnly(otherOrganization.getUuid());
  }

  @Test
  public void update() {
    OrganizationDto organization = db.organizations().insert();
    QGateWithOrgDto qualityGate = qualityGateDbTester.insertQualityGate(organization, qg -> qg.setName("old name"));

    underTest.update(qualityGate.setName("Not so strict"), dbSession);
    dbSession.commit();

    QGateWithOrgDto reloaded = underTest.selectByOrganizationAndUuid(dbSession, organization, qualityGate.getUuid());
    assertThat(reloaded.getName()).isEqualTo("Not so strict");
  }

  private void insertQualityGates() {
    qualityGateDbTester.insertQualityGate(db.getDefaultOrganization(), g -> g.setName("Very strict").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(db.getDefaultOrganization(), g -> g.setName("Balanced").setBuiltIn(false));
    qualityGateDbTester.insertQualityGate(db.getDefaultOrganization(), g -> g.setName("Lenient").setBuiltIn(false));
  }
}
