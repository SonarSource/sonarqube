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

package org.sonar.server.qualityprofile.ws;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.rule.RuleService;
import org.sonar.server.ws.WsTester;

import java.io.Reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QProfilesWsTest {

  WebService.Controller controller;

  String xoo1Key = "xoo1", xoo2Key = "xoo2";

  @Before
  public void setUp() {
    QProfileService profileService = mock(QProfileService.class);
    RuleService ruleService = mock(RuleService.class);
    I18n i18n = mock(I18n.class);

    Languages languages = LanguageTesting.newLanguages(xoo1Key, xoo2Key);

    ProfileImporter[] importers = createImporters(languages);

    controller = new WsTester(new QProfilesWs(
      new RuleActivationActions(profileService),
      new BulkRuleActivationActions(profileService, ruleService, i18n),
      new ProjectAssociationActions(null, null, null, languages),
      new QProfileCreateAction(null, null, null, languages, importers),
      new QProfileImportersAction(importers),
      new QProfileRestoreBuiltInAction(null),
      new QProfileSearchAction(languages, null, null),
      new QProfileSetDefaultAction(languages, null, null),
      new QProfileProjectsAction(null),
      new QProfileBackupAction(null, null, null, languages),
      new QProfileRestoreAction(null, languages)
    )).controller(QProfilesWs.API_ENDPOINT);
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
  public void define_controller() throws Exception {
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo(QProfilesWs.API_ENDPOINT);
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(14);
  }

  @Test
  public void define_restore_built_action() throws Exception {
    WebService.Action restoreProfiles = controller.action("restore_built_in");
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(1);
  }

  @Test
  public void define_search() throws Exception {
    WebService.Action search = controller.action("search");
    assertThat(search).isNotNull();
    assertThat(search.isPost()).isFalse();
    assertThat(search.params()).hasSize(2);
    assertThat(search.param("language").possibleValues()).containsOnly(xoo1Key, xoo2Key);
    assertThat(search.param("f").possibleValues())
      .containsOnly("key", "name", "language", "languageName", "isInherited", "parentKey", "parentName", "isDefault", "activeRuleCount");
  }

  @Test
  public void define_activate_rule_action() throws Exception {
    WebService.Action restoreProfiles = controller.action(RuleActivationActions.ACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(5);
  }

  @Test
  public void define_deactivate_rule_action() throws Exception {
    WebService.Action restoreProfiles = controller.action(RuleActivationActions.DEACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(2);
  }

  @Test
  public void define_add_project_action() throws Exception {
    WebService.Action addProject = controller.action("add_project");
    assertThat(addProject).isNotNull();
    assertThat(addProject.isPost()).isTrue();
    assertThat(addProject.params()).hasSize(5);
  }

  @Test
  public void define_remove_project_action() throws Exception {
    WebService.Action removeProject = controller.action("remove_project");
    assertThat(removeProject).isNotNull();
    assertThat(removeProject.isPost()).isTrue();
    assertThat(removeProject.params()).hasSize(5);
  }

  @Test
  public void define_set_default_action() throws Exception {
    WebService.Action setDefault = controller.action("set_default");
    assertThat(setDefault).isNotNull();
    assertThat(setDefault.isPost()).isTrue();
    assertThat(setDefault.params()).hasSize(3);
  }

  @Test
  public void define_projects_action() throws Exception {
    WebService.Action projects = controller.action("projects");
    assertThat(projects).isNotNull();
    assertThat(projects.isPost()).isFalse();
    assertThat(projects.params()).hasSize(5);
    assertThat(projects.responseExampleAsString()).isNotEmpty();
  }

  @Test
  public void define_backup_action() throws Exception {
    WebService.Action backup = controller.action("backup");
    assertThat(backup).isNotNull();
    assertThat(backup.isPost()).isFalse();
    assertThat(backup.params()).hasSize(3);
  }

  @Test
  public void define_restore_action() throws Exception {
    WebService.Action restore = controller.action("restore");
    assertThat(restore).isNotNull();
    assertThat(restore.isPost()).isTrue();
    assertThat(restore.params()).hasSize(1);
  }

  public void define_bulk_activate_rule_action() throws Exception {
    WebService.Action restoreProfiles = controller.action(BulkRuleActivationActions.BULK_ACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(20);
  }

  @Test
  public void define_bulk_deactivate_rule_action() throws Exception {
    WebService.Action restoreProfiles = controller.action(BulkRuleActivationActions.BULK_DEACTIVATE_ACTION);
    assertThat(restoreProfiles).isNotNull();
    assertThat(restoreProfiles.isPost()).isTrue();
    assertThat(restoreProfiles.params()).hasSize(19);
  }

  @Test
  public void define_create_action() throws Exception {
    WebService.Action create = controller.action("create");
    assertThat(create).isNotNull();
    assertThat(create.isPost()).isTrue();
    assertThat(create.params()).hasSize(4);
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
  public void define_importers_action() throws Exception {
    WebService.Action importers = controller.action("importers");
    assertThat(importers).isNotNull();
    assertThat(importers.isPost()).isFalse();
    assertThat(importers.params()).isEmpty();
    assertThat(importers.responseExampleAsString()).isNotEmpty();
  }
}
