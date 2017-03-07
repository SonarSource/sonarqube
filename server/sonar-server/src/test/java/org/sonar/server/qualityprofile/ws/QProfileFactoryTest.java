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
package org.sonar.server.qualityprofile.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileRef;

import static org.assertj.core.api.Assertions.assertThat;

public class QProfileFactoryTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  private DbSession dbSession = dbTester.getSession();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private QProfileFactory underTest = new QProfileFactory(dbTester.getDbClient(), UuidFactoryFast.getInstance());

  @Before
  public void setUp() throws Exception {
    QualityProfileDto dto = QualityProfileTesting.newQualityProfileDto().setKey("sw").setName("Sonar way").setLanguage("js");
    dbTester.getDbClient().qualityProfileDao().insert(dbSession, dto);
    dbTester.commit();
  }

  @Test
  public void find_profile_by_key() {
    QualityProfileDto profile = underTest.find(dbSession, QProfileRef.fromKey("sw"));
    assertThat(profile.getKey()).isEqualTo("sw");
    assertThat(profile.getLanguage()).isEqualTo("js");
    assertThat(profile.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void find_profile_by_name() {
    QualityProfileDto profile = underTest.find(dbSession, QProfileRef.fromName("js", "Sonar way"));
    assertThat(profile.getKey()).isEqualTo("sw");
    assertThat(profile.getLanguage()).isEqualTo("js");
    assertThat(profile.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void throw_NFE_if_profile_key_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    underTest.find(dbSession, QProfileRef.fromKey("missing"));
  }

  @Test
  public void throw_NFE_if_profile_name_does_not_exist() {
    expectedException.expect(NotFoundException.class);

    underTest.find(dbSession, QProfileRef.fromName("js", "Missing"));
  }
}
