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
package org.sonar.server.qualityprofile.ws;

import java.io.Reader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.db.DbClient;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QProfilesWsTest {
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private WebService.Controller controller;
  private String xoo1Key = "xoo1";
  private String xoo2Key = "xoo2";
  private DefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid("ORG1");
  private QProfileWsSupport wsSupport = new QProfileWsSupport(mock(DbClient.class), userSession, defaultOrganizationProvider);

  @Before
  public void setUp() {
    DbClient dbClient = mock(DbClient.class);

    Languages languages = LanguageTesting.newLanguages(xoo1Key, xoo2Key);

    ProfileImporter[] importers = createImporters(languages);

    controller = new WsTester(new QProfilesWs(
      new CreateAction(null, null, null, languages, wsSupport, userSession, null, importers),
      new ImportersAction(importers),
      new SearchAction(userSession, languages, dbClient, wsSupport, null),
      new SetDefaultAction(languages, null, null, wsSupport),
      new ProjectsAction(null, userSession, wsSupport),
      new ChangelogAction(wsSupport, languages, dbClient),
      new ChangeParentAction(dbClient, null, languages, wsSupport, userSession),
      new CompareAction(null, null, languages),
      new DeleteAction(languages, null, null, userSession, wsSupport),
      new ExportersAction(),
      new InheritanceAction(null, null, languages),
      new RenameAction(dbClient, userSession, wsSupport))).controller(QProfilesWs.API_ENDPOINT);
  }

  private ProfileImporter[] createImporters(Languages languages) {
    class NoopImporter extends ProfileImporter {
      public NoopImporter(Language language) {
        super(language.getKey(), language.getName());
      }

      @Override
      public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
        return null;
      }
    }
    return new ProfileImporter[] {
      new NoopImporter(languages.get(xoo1Key)),
      new NoopImporter(languages.get(xoo2Key))
    };
  }

  @Test
  public void define_controller() {
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo(QProfilesWs.API_ENDPOINT);
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).isNotEmpty();
  }

  @Test
  public void define_create_action() {
    WebService.Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.isPost()).isTrue();
    assertThat(create.params()).hasSize(5);
    assertThat(create.param("organization")).isNotNull();
    assertThat(create.param("organization").isRequired()).isFalse();
    assertThat(create.param("organization").isInternal()).isTrue();
    assertThat(create.param("name")).isNotNull();
    assertThat(create.param("name").isRequired()).isTrue();
    assertThat(create.param("language").possibleValues()).containsOnly(xoo1Key, xoo2Key);
    assertThat(create.param("language").isRequired()).isTrue();
    assertThat(create.param("backup_" + xoo1Key)).isNotNull();
    assertThat(create.param("backup_" + xoo1Key).isRequired()).isFalse();
    assertThat(create.param("backup_" + xoo2Key)).isNotNull();
    assertThat(create.param("backup_" + xoo2Key).isRequired()).isFalse();
  }

  @Test
  public void define_importers_action() {
    WebService.Action importers = controller.action("importers");
    assertThat(importers).isNotNull();
    assertThat(importers.isPost()).isFalse();
    assertThat(importers.params()).isEmpty();
    assertThat(importers.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void define_changelog_action() {
    WebService.Action changelog = controller.action("changelog");
    assertThat(changelog).isNotNull();
    assertThat(changelog.isPost()).isFalse();
    assertThat(changelog.params().size()).isPositive();
    assertThat(changelog.responseExampleAsString()).isNotEmpty();
    assertThat(changelog.param("organization").since()).isEqualTo("6.4");
    assertThat(changelog.param("organization").isInternal()).isTrue();
  }

  @Test
  public void define_compare_action() {
    WebService.Action compare = controller.action("compare");
    assertThat(compare).isNotNull();
    assertThat(compare.isPost()).isFalse();
    assertThat(compare.isInternal()).isTrue();
    assertThat(compare.params()).hasSize(2).extracting("key").containsOnly(
      "leftKey", "rightKey");
    assertThat(compare.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void define_delete_action() {
    WebService.Action delete = controller.action("delete");
    assertThat(delete).isNotNull();
    assertThat(delete.isPost()).isTrue();
    assertThat(delete.params()).hasSize(4).extracting(Param::key).containsOnly(
      "organization", "key", "language", "qualityProfile");
  }

  @Test
  public void define_exporters_action() {
    WebService.Action exporters = controller.action("exporters");
    assertThat(exporters).isNotNull();
    assertThat(exporters.isPost()).isFalse();
    assertThat(exporters.params()).isEmpty();
    assertThat(exporters.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void define_inheritance_action() {
    WebService.Action inheritance = controller.action("inheritance");
    assertThat(inheritance).isNotNull();
    assertThat(inheritance.isPost()).isFalse();
    assertThat(inheritance.params()).hasSize(4).extracting(Param::key).containsExactlyInAnyOrder(
      "organization", "key", "language", "qualityProfile");
    assertThat(inheritance.responseExampleAsString()).isNotEmpty();
  }
}
