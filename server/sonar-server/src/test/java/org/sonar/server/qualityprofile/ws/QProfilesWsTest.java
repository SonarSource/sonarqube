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
package org.sonar.server.qualityprofile.ws;

import java.io.Reader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.db.DbClient;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileExporters;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QProfilesWsTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  WebService.Controller controller;

  String xoo1Key = "xoo1";
  String xoo2Key = "xoo2";

  @Before
  public void setUp() {
    QProfileService profileService = mock(QProfileService.class);
    I18n i18n = mock(I18n.class);
    DbClient dbClient = mock(DbClient.class);

    Languages languages = LanguageTesting.newLanguages(xoo1Key, xoo2Key);
    ProjectAssociationParameters projectAssociationParameters = new ProjectAssociationParameters(languages);

    ProfileImporter[] importers = createImporters(languages);

    controller = new WsTester(new QProfilesWs(
      new RuleActivationActions(profileService),
      new BulkRuleActivationActions(profileService, null, i18n, userSessionRule),
      new AddProjectAction(projectAssociationParameters, null, null, null),
      new RemoveProjectAction(projectAssociationParameters, null, null, null),
      new CreateAction(null, null, null, languages, importers, userSessionRule, null),
      new ImportersAction(importers),
      new RestoreBuiltInAction(null, languages, userSessionRule),
      new SearchAction(null, languages),
      new SetDefaultAction(languages, null, null, userSessionRule),
      new ProjectsAction(null, userSessionRule),
      new BackupAction(dbClient, null, null, languages),
      new RestoreAction(null, languages, userSessionRule),
      new ChangelogAction(null, mock(QProfileFactory.class), languages, dbClient),
      new ChangeParentAction(dbClient, null, null, languages, userSessionRule),
      new CompareAction(null, null, languages),
      new CopyAction(null, languages, userSessionRule),
      new DeleteAction(languages, null, null, userSessionRule),
      new ExportAction(null, null, null, mock(QProfileExporters.class), languages),
      new ExportersAction(),
      new InheritanceAction(null, null, null, null, languages),
      new RenameAction(null, userSessionRule))).controller(QProfilesWs.API_ENDPOINT);
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
    assertThat(controller.actions()).hasSize(23);
  }

  @Test
  public void define_restore_built_action() {
    WebService.Action restoreProfiles = controller.action("restore_built_in");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(1);
  }

  @Test
  public void define_search() {
    WebService.Action search = controller.action("search");
    assertThat(search).isNotNull();
    assertThat(search.isPost()).isFalse();
    assertThat(search.params()).hasSize(4);
    assertThat(search.param("language").possibleValues()).containsOnly(xoo1Key, xoo2Key);
  }

  @Test
  public void define_activate_rule_action() {
    WebService.Action restoreProfiles = controller.action(RuleActivationActions.ACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(5);
  }

  @Test
  public void define_deactivate_rule_action() {
    WebService.Action restoreProfiles = controller.action(RuleActivationActions.DEACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(2);
  }

  @Test
  public void define_add_project_action() {
    WebService.Action addProject = controller.action("add_project");
    assertThat(addProject).isNotNull();
    assertThat(addProject.isPost()).isTrue();
    assertThat(addProject.params()).hasSize(5);
  }

  @Test
  public void define_remove_project_action() {
    WebService.Action removeProject = controller.action("remove_project");
    assertThat(removeProject).isNotNull();
    assertThat(removeProject.isPost()).isTrue();
    assertThat(removeProject.params()).hasSize(5);
  }

  @Test
  public void define_set_default_action() {
    WebService.Action setDefault = controller.action("set_default");
    assertThat(setDefault).isNotNull();
    assertThat(setDefault.isPost()).isTrue();
    assertThat(setDefault.params()).hasSize(3);
  }

  @Test
  public void define_projects_action() {
    WebService.Action projects = controller.action("projects");
    assertThat(projects).isNotNull();
    assertThat(projects.isPost()).isFalse();
    assertThat(projects.params()).hasSize(5);
    assertThat(projects.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void define_backup_action() {
    WebService.Action backup = controller.action("backup");
    assertThat(backup).isNotNull();
    assertThat(backup.isPost()).isFalse();
    assertThat(backup.params()).hasSize(3);
  }

  @Test
  public void define_restore_action() {
    WebService.Action restore = controller.action("restore");
    assertThat(restore).isNotNull();
    assertThat(restore.isPost()).isTrue();
    assertThat(restore.params()).hasSize(1);
  }

  public void define_bulk_activate_rule_action() {
    WebService.Action restoreProfiles = controller.action(BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(20);
  }

  @Test
  public void define_bulk_deactivate_rule_action() {
    WebService.Action restoreProfiles = controller.action(BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(18);
  }

  @Test
  public void define_create_action() {
    WebService.Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.isPost()).isTrue();
    assertThat(create.params()).hasSize(4);
    assertThat(create.param("profileName")).isNotNull();
    assertThat(create.param("profileName").isRequired()).isTrue();
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
  }

  @Test
  public void define_change_parent_action() {
    WebService.Action changeParent = controller.action("change_parent");
    assertThat(changeParent).isNotNull();
    assertThat(changeParent.isPost()).isTrue();
    assertThat(changeParent.params()).hasSize(5).extracting("key").containsOnly(
      "profileKey", "profileName", "language", "parentKey", "parentName");
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
  public void define_copy_action() {
    WebService.Action copy = controller.action("copy");
    assertThat(copy).isNotNull();
    assertThat(copy.isPost()).isTrue();
    assertThat(copy.params()).hasSize(2).extracting("key").containsOnly(
      "fromKey", "toName");
  }

  @Test
  public void define_delete_action() {
    WebService.Action delete = controller.action("delete");
    assertThat(delete).isNotNull();
    assertThat(delete.isPost()).isTrue();
    assertThat(delete.params()).hasSize(3).extracting("key").containsOnly(
      "profileKey", "language", "profileName");
  }

  @Test
  public void define_export_action() {
    WebService.Action export = controller.action("export");
    assertThat(export).isNotNull();
    assertThat(export.isPost()).isFalse();
    assertThat(export.params()).hasSize(2).extracting("key").containsOnly(
      "language", "name");
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
    assertThat(inheritance.params()).hasSize(3).extracting("key").containsOnly(
      "profileKey", "language", "profileName");
    assertThat(inheritance.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void define_rename_action() {
    WebService.Action rename = controller.action("rename");
    assertThat(rename).isNotNull();
    assertThat(rename.isPost()).isTrue();
    assertThat(rename.params()).hasSize(2).extracting("key").containsOnly(
      "key", "name");
  }
}
