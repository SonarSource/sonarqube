/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Organization;
import org.sonar.server.es.EsTester;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserIndexDefinition;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ScmAccountToUserLoaderTest {

  private static final String ORGANIZATION_UUID = "my-organization";

  @Rule
  public EsTester esTester = new EsTester(new UserIndexDefinition(new MapSettings().asConfig()));

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setOrganization(Organization.from(new OrganizationDto().setUuid(ORGANIZATION_UUID).setKey("Key").setName("Name").setDefaultQualityGateUuid("QGate")));

  @Test
  public void load_login_for_scm_account() {
    UserDoc user = new UserDoc()
      .setLogin("charlie")
      .setName("Charlie")
      .setEmail("charlie@hebdo.com")
      .setActive(true)
      .setScmAccounts(asList("charlie", "jesuis@charlie.com"))
      .setOrganizationUuids(singletonList(ORGANIZATION_UUID));
    esTester.putDocuments(UserIndexDefinition.INDEX_TYPE_USER.getIndex(), UserIndexDefinition.INDEX_TYPE_USER.getType(), user);

    UserIndex index = new UserIndex(esTester.client(), System2.INSTANCE);
    ScmAccountToUserLoader underTest = new ScmAccountToUserLoader(index, analysisMetadataHolder);

    assertThat(underTest.load("missing")).isNull();
    assertThat(underTest.load("jesuis@charlie.com")).isEqualTo("charlie");
  }

  @Test
  public void warn_if_multiple_users_share_the_same_scm_account() {
    UserDoc user1 = new UserDoc()
      .setLogin("charlie")
      .setName("Charlie")
      .setEmail("charlie@hebdo.com")
      .setActive(true)
      .setScmAccounts(asList("charlie", "jesuis@charlie.com"))
      .setOrganizationUuids(singletonList(ORGANIZATION_UUID));
    esTester.putDocuments(UserIndexDefinition.INDEX_TYPE_USER.getIndex(), UserIndexDefinition.INDEX_TYPE_USER.getType(), user1);

    UserDoc user2 = new UserDoc()
      .setLogin("another.charlie")
      .setName("Another Charlie")
      .setActive(true)
      .setScmAccounts(singletonList("charlie"))
      .setOrganizationUuids(singletonList(ORGANIZATION_UUID));
    esTester.putDocuments(UserIndexDefinition.INDEX_TYPE_USER.getIndex(), UserIndexDefinition.INDEX_TYPE_USER.getType(), user2);

    UserIndex index = new UserIndex(esTester.client(), System2.INSTANCE);
    ScmAccountToUserLoader underTest = new ScmAccountToUserLoader(index, analysisMetadataHolder);

    assertThat(underTest.load("charlie")).isNull();
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Multiple users share the SCM account 'charlie': another.charlie, charlie");
  }

  @Test
  public void load_by_multiple_scm_accounts_is_not_supported_yet() {
    UserIndex index = new UserIndex(esTester.client(), System2.INSTANCE);
    ScmAccountToUserLoader underTest = new ScmAccountToUserLoader(index, analysisMetadataHolder);
    try {
      underTest.loadAll(Collections.emptyList());
      fail();
    } catch (UnsupportedOperationException ignored) {
    }
  }
}
