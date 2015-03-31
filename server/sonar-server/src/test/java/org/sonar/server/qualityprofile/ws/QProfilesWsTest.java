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

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.rule.RuleService;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class QProfilesWsTest {

  WebService.Controller controller;

  Language xoo1, xoo2;

  @Before
  public void setUp() {
    Language xoo1 = new AbstractLanguage("xoo1", "Xoo1") {
      @Override
      public String[] getFileSuffixes() {
        return new String[] {"xoo1"};
      }
    };
    Language xoo2 = new AbstractLanguage("xoo2", "Xoo2") {
      @Override
      public String[] getFileSuffixes() {
        return new String[] {"xoo2"};
      }
    };

    QProfileService profileService = mock(QProfileService.class);
    RuleService ruleService = mock(RuleService.class);
    I18n i18n = mock(I18n.class);

    xoo1 = createLanguage("xoo1");
    xoo2 = createLanguage("xoo2");
    Languages languages = new Languages(xoo1, xoo2);

    controller = new WsTester(new QProfilesWs(
      new RuleActivationActions(profileService),
      new BulkRuleActivationActions(profileService, ruleService, i18n),
      new ProjectAssociationActions(null, null, null, languages),
      new QProfileRestoreBuiltInAction(
        mock(QProfileService.class)),
      new QProfileSearchAction(new Languages(xoo1, xoo2), null, null)
    )).controller(QProfilesWs.API_ENDPOINT);
  }

  @Test
  public void define_controller() throws Exception {
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo(QProfilesWs.API_ENDPOINT);
    assertThat(controller.description()).isNotEmpty();
    assertThat(controller.actions()).hasSize(8);
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
    assertThat(search.param("language").possibleValues()).containsOnly("xoo1", "xoo2");
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

  private Language createLanguage(final String key) {
    return new AbstractLanguage(key, StringUtils.capitalize(key)) {
      @Override
      public String[] getFileSuffixes() {
        return new String[] {key};
      }
    };
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
}
