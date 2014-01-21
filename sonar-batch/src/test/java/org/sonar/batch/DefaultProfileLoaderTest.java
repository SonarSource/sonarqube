/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.language.ModuleLanguages;
import org.sonar.jpa.dao.ProfilesDao;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultProfileLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private ProfilesDao dao;
  private Languages languages;
  private Project javaProject = newProject(Java.KEY);

  @Before
  public void setUp() {
    dao = mock(ProfilesDao.class);
    Language cobol = new AbstractLanguage("cobol", "Cobol") {
      public String[] getFileSuffixes() {
        return null;
      };
    };
    languages = new Languages(Java.INSTANCE, cobol);
  }

  @Test
  public void should_get_configured_profile() {
    Settings settings = new Settings();
    settings.setProperty("sonar.profile.java", "legacy profile");
    settings.setProperty("sonar.profile.cobol", "cobol profile");
    when(dao.getProfile(Java.KEY, "legacy profile")).thenReturn(RulesProfile.create("legacy profile", "java"));
    when(dao.getProfile("cobol", "cobol profile")).thenReturn(RulesProfile.create("cobol profile", "cobol"));

    ModuleLanguages moduleLanguages = new ModuleLanguages(settings, languages);
    moduleLanguages.addLanguage("java");
    RulesProfile profile = new DefaultProfileLoader(dao, moduleLanguages, languages).load(javaProject, settings);

    assertThat(profile.getName()).isEqualTo("legacy profile");
  }

  @Test
  public void some_methods_should_support_multilanguage() {
    Settings settings = new Settings();
    settings.setProperty("sonar.profile.java", "java profile");
    settings.setProperty("sonar.profile.cobol", "cobol profile");

    RulesProfile javaProfile = RulesProfile.create("java profile", "java");
    org.sonar.api.rules.Rule javaRule = new org.sonar.api.rules.Rule("javaplugin", "javarule");
    ActiveRule javaActiveRule = new ActiveRule(javaProfile, javaRule, RulePriority.BLOCKER);
    javaProfile.addActiveRule(javaActiveRule);
    Alert javaAlert = mock(Alert.class);
    javaProfile.setAlerts(Arrays.asList(javaAlert));
    when(dao.getProfile(Java.KEY, "java profile")).thenReturn(javaProfile);

    RulesProfile cobolProfile = RulesProfile.create("cobol profile", "cobol");
    org.sonar.api.rules.Rule cobolRule = new org.sonar.api.rules.Rule("cobolplugin", "cobolrule");
    ActiveRule cobolActiveRule = new ActiveRule(cobolProfile, cobolRule, RulePriority.BLOCKER);
    cobolProfile.addActiveRule(cobolActiveRule);
    Alert cobolAlert = mock(Alert.class);
    cobolProfile.setAlerts(Arrays.asList(cobolAlert));
    when(dao.getProfile("cobol", "cobol profile")).thenReturn(cobolProfile);

    ModuleLanguages moduleLanguages = new ModuleLanguages(settings, languages);
    moduleLanguages.addLanguage("java");
    moduleLanguages.addLanguage("cobol");
    RulesProfile profile = new DefaultProfileLoader(dao, moduleLanguages, languages).load(javaProject, settings);

    assertThat(profile.getActiveRules()).containsOnly(javaActiveRule, cobolActiveRule);
    assertThat(profile.getActiveRules(true)).containsOnly(javaActiveRule, cobolActiveRule);
    assertThat(profile.getActiveRulesByRepository("javaplugin")).containsOnly(javaActiveRule);
    assertThat(profile.getActiveRulesByRepository("cobolplugin")).containsOnly(cobolActiveRule);
    assertThat(profile.getActiveRule("javaplugin", "javarule")).isEqualTo(javaActiveRule);
    assertThat(profile.getActiveRule(javaRule)).isEqualTo(javaActiveRule);
    assertThat(profile.getActiveRule("cobolplugin", "cobolrule")).isEqualTo(cobolActiveRule);
    assertThat(profile.getActiveRule(cobolRule)).isEqualTo(cobolActiveRule);
    assertThat(profile.getAlerts()).containsOnly(javaAlert, cobolAlert);
    // Hack for CommonChecksDecorator
    assertThat(profile.getLanguage()).isEqualTo("");
  }

  @Test
  public void some_methods_should_not_support_multilanguage() {
    Settings settings = new Settings();
    settings.setProperty("sonar.profile.java", "java profile");
    settings.setProperty("sonar.profile.cobol", "cobol profile");

    RulesProfile javaProfile = RulesProfile.create("java profile", "java");
    org.sonar.api.rules.Rule javaRule = new org.sonar.api.rules.Rule("javaplugin", "javarule");
    ActiveRule javaActiveRule = new ActiveRule(javaProfile, javaRule, RulePriority.BLOCKER);
    javaProfile.addActiveRule(javaActiveRule);
    when(dao.getProfile(Java.KEY, "java profile")).thenReturn(javaProfile);

    RulesProfile cobolProfile = RulesProfile.create("cobol profile", "cobol");
    org.sonar.api.rules.Rule cobolRule = new org.sonar.api.rules.Rule("cobolplugin", "cobolrule");
    ActiveRule cobolActiveRule = new ActiveRule(cobolProfile, cobolRule, RulePriority.BLOCKER);
    cobolProfile.addActiveRule(cobolActiveRule);
    when(dao.getProfile("cobol", "cobol profile")).thenReturn(cobolProfile);

    ModuleLanguages moduleLanguages = new ModuleLanguages(settings, languages);
    moduleLanguages.addLanguage("java");
    moduleLanguages.addLanguage("cobol");
    RulesProfile profile = new DefaultProfileLoader(dao, moduleLanguages, languages).load(javaProject, settings);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Please update your plugin to support multi-language analysis");
    profile.getId();
  }

  @Test
  public void should_fail_if_not_found() {
    Settings settings = new Settings();
    settings.setProperty("sonar.profile.java", "unknown");

    thrown.expect(SonarException.class);
    thrown.expectMessage("Quality profile not found : unknown, language java");
    new DefaultProfileLoader(dao, new ModuleLanguages(settings, languages), languages).load(javaProject, settings);
  }

  private Project newProject(String language) {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty("sonar.language", language);
    return new Project("project").setConfiguration(configuration);
  }
}
