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
package org.sonar.server.qualityprofile;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileFactoryMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private DbClient db;
  private DbSession dbSession;
  private QProfileFactory factory;
  private OrganizationDto organization;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    factory = tester.get(QProfileFactory.class);
    organization = OrganizationTesting.newOrganizationDto();
    db.organizationDao().insert(dbSession, organization, false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void checkAndCreate() {
    String uuid = organization.getUuid();

    QualityProfileDto writtenDto = factory.checkAndCreate(dbSession, organization, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(writtenDto.getOrganizationUuid()).isEqualTo(uuid);
    assertThat(writtenDto.getKey()).isNotEmpty();
    assertThat(writtenDto.getName()).isEqualTo("P1");
    assertThat(writtenDto.getLanguage()).isEqualTo("xoo");
    assertThat(writtenDto.getId()).isNotNull();

    // reload the dto
    QualityProfileDto readDto = db.qualityProfileDao().selectByNameAndLanguage(organization, "P1", "xoo", dbSession);
    assertEqual(writtenDto, readDto);

    assertThat(db.qualityProfileDao().selectAll(dbSession, organization)).hasSize(1);
  }

  @Test
  public void create() {
    String uuid = organization.getUuid();

    QualityProfileDto writtenDto = factory.create(dbSession, organization, new QProfileName("xoo", "P1"), true);
    dbSession.commit();
    dbSession.clearCache();
    assertThat(writtenDto.getOrganizationUuid()).isEqualTo(uuid);
    assertThat(writtenDto.getKey()).isNotEmpty();
    assertThat(writtenDto.getName()).isEqualTo("P1");
    assertThat(writtenDto.getLanguage()).isEqualTo("xoo");
    assertThat(writtenDto.getId()).isNotNull();
    assertThat(writtenDto.getParentKee()).isNull();
    assertThat(writtenDto.isDefault()).isTrue();

    // reload the dto
    QualityProfileDto readDto = db.qualityProfileDao().selectByNameAndLanguage(organization, "P1", "xoo", dbSession);
    assertEqual(writtenDto, readDto);

    assertThat(db.qualityProfileDao().selectAll(dbSession, organization)).hasSize(1);
  }

  @Test
  public void checkAndCreate_throws_BadRequestException_if_name_null() {
    QProfileName name = new QProfileName("xoo", null);

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    factory.checkAndCreate(dbSession, organization, name);
  }

  @Test
  public void checkAndCreate_throws_BadRequestException_if_name_empty() {
    QProfileName name = new QProfileName("xoo", "");

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    factory.checkAndCreate(dbSession, organization, name);
  }

  @Test
  public void checkAndCreate_throws_BadRequestException_if_already_exists() {
    QProfileName name = new QProfileName("xoo", "P1");
    factory.checkAndCreate(dbSession, organization, name);
    dbSession.commit();
    dbSession.clearCache();

    expectBadRequestException("Quality profile already exists: {lang=xoo, name=P1}");

    factory.checkAndCreate(dbSession, organization, name);
  }

  @Test
  public void create_throws_BadRequestException_if_name_null() {
    QProfileName name = new QProfileName("xoo", null);

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    factory.create(dbSession, organization, name, true);
  }

  @Test
  public void create_throws_BadRequestException_if_name_empty() {
    QProfileName name = new QProfileName("xoo", "");

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    factory.create(dbSession, organization, name, false);
  }

  @Test
  public void create_does_not_fail_if_already_exists() {
    QProfileName name = new QProfileName("xoo", "P1");
    factory.create(dbSession, organization, name, true);
    dbSession.commit();
    dbSession.clearCache();

    assertThat(factory.create(dbSession, organization, name, true)).isNotNull();
  }

  private void expectBadRequestException(String message) {
    thrown.expect(BadRequestException.class);
    thrown.expectMessage(message);
  }

  private static void assertEqual(QualityProfileDto writtenDto, QualityProfileDto readDto) {
    assertThat(readDto.getOrganizationUuid()).isEqualTo(writtenDto.getOrganizationUuid());
    assertThat(readDto.getName()).isEqualTo(writtenDto.getName());
    assertThat(readDto.getKey()).startsWith(writtenDto.getKey());
    assertThat(readDto.getLanguage()).isEqualTo(writtenDto.getLanguage());
    assertThat(readDto.getId()).isEqualTo(writtenDto.getId());
    assertThat(readDto.getParentKee()).isEqualTo(writtenDto.getParentKee());
    assertThat(readDto.isDefault()).isEqualTo(writtenDto.isDefault());
  }
}
