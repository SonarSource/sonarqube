/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.configuration;

import org.junit.Before;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;
import static org.junit.Assert.*;

public class ProfilesBackupTest extends AbstractDbUnitTestCase {

  private SonarConfig sonarConfig;

  @Before
  public void setup() {
    sonarConfig = new SonarConfig();
  }

  @Test
  public void shouldExportClonedProfiles() {

    RulesProfile profileProvided = new RulesProfile();
    profileProvided.setProvided(false);
    profileProvided.setLanguage("test");
    profileProvided.setName("provided");

    ProfilesBackup profilesBackup = new ProfilesBackup(Arrays.asList(profileProvided));
    profilesBackup.exportXml(sonarConfig);

    assertFalse(sonarConfig.getProfiles().iterator().next() == profileProvided);
    assertEquals(sonarConfig.getProfiles().iterator().next().getName(), "provided");
  }

  @Test
  public void shouldExportAllProfiles() {

    RulesProfile profileProvided = new RulesProfile();
    profileProvided.setProvided(true);
    profileProvided.setLanguage("test");
    profileProvided.setName("provided");

    RulesProfile profileNotProvided = new RulesProfile();
    profileNotProvided.setProvided(false);
    profileNotProvided.setLanguage("test");
    profileNotProvided.setName("not provided");

    ProfilesBackup profilesBackup = new ProfilesBackup(Arrays.asList(profileProvided, profileNotProvided));
    profilesBackup.exportXml(sonarConfig);

    assertThat(sonarConfig.getProfiles(), hasItem(profileNotProvided));
    assertThat(sonarConfig.getProfiles(), hasItem(profileProvided));
  }

  @Test
  public void testExportWithNoProfiles() {

    RulesProfile profileProvided = new RulesProfile("test provided", "lang", false, true);
    RulesProfile profileNotProvided = new RulesProfile("test not provided", "lang", false, false);
    getSession().save(profileProvided, profileNotProvided);

    assertThat(getHQLCount(RulesProfile.class), equalTo(2l));

    ProfilesBackup profilesBackup = new ProfilesBackup(getSession());
    assertNull(sonarConfig.getProfiles());
    profilesBackup.importXml(sonarConfig);

    assertThat(getHQLCount(RulesProfile.class), equalTo(2l));

    RulesProfile profileProvidedRemains = getSession().getSingleResult(RulesProfile.class, "name", "test provided", "provided", true);
    assertNotNull(profileProvidedRemains);
    assertEquals(profileProvided, profileProvidedRemains);
  }

  @Test
  public void testExportWithProfilesAndAlerts() {

    RulesProfile profileProvided = new RulesProfile("test provided", "lang", false, true);
    RulesProfile profileNotProvided = new RulesProfile("test not provided", "lang", false, false);
    getSession().save(profileProvided, profileNotProvided);

    Rule rule1 = new Rule("testPlugin", "testKey", "testName", null, null);
    Rule rule2 = new Rule("testPlugin", "testKey2", "testName2", null, null);
    getSession().save(rule1, rule2);
    RuleParam ruleParam1 = new RuleParam(rule1, "paramKey", "test", "int");
    getSession().save(ruleParam1);

    Metric metric1 = new Metric("testKey");
    Metric metric2 = new Metric("testKey2");
    getSession().save(metric1, metric2);

    RulesProfile testProfile = new RulesProfile("testProfile", "lang", false, false);
    ActiveRule ar = new ActiveRule(null, new Rule("testPlugin", "testKey"), RulePriority.MAJOR);
    ar.getActiveRuleParams().add(new ActiveRuleParam(null, new RuleParam(null, "paramKey", null, null), "testValue"));
    testProfile.getActiveRules().add(ar);
    testProfile.getActiveRules().add(new ActiveRule(null, new Rule("testPlugin", "testKey2"), RulePriority.MINOR));

    testProfile.getAlerts().add(new Alert(null, new Metric("testKey"), Alert.OPERATOR_EQUALS, "10", "22"));
    testProfile.getAlerts().add(new Alert(null, new Metric("testKey2"), Alert.OPERATOR_GREATER, "10", "22"));

    sonarConfig.setProfiles(Arrays.asList(testProfile));

    ProfilesBackup profilesBackupTest = new ProfilesBackup(getSession());
    profilesBackupTest.importXml(sonarConfig);

    assertThat(getHQLCount(RulesProfile.class), equalTo(1l));

    RulesProfile profileProvidedRemains = getSession().getSingleResult(RulesProfile.class, "name", "test provided", "provided", true);
    RulesProfile newProfile = getSession().getSingleResult(RulesProfile.class, "name", "testProfile");

    assertNull(profileProvidedRemains);

    assertNotNull(newProfile);
    assertEquals(2, newProfile.getActiveRules().size());
    assertEquals(1, newProfile.getActiveRules(RulePriority.MAJOR).get(0).getActiveRuleParams().size());
    assertEquals(2, newProfile.getAlerts().size());

  }
}
