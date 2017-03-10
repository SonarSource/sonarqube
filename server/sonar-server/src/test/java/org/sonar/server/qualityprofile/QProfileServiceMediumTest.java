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

import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.search.FacetValue;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleTesting.newXooX1;
import static org.sonar.db.rule.RuleTesting.newXooX2;
import static org.sonar.db.rule.RuleTesting.newXooX3;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.getDefaultOrganization;

public class QProfileServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes().addComponents(XooProfileImporter.class, XooExporter.class);

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private DbClient dbClient;
  private DbSession dbSession;
  private QProfileService service;
  private QProfileLoader loader;
  private ActiveRuleIndex activeRuleIndex;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;

  private RuleDto xooRule1 = newXooX1().setSeverity("MINOR");

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbClient = tester.get(DbClient.class);
    dbSession = dbClient.openSession(false);
    service = tester.get(QProfileService.class);
    loader = tester.get(QProfileLoader.class);
    ruleIndexer = tester.get(RuleIndexer.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    activeRuleIndex = tester.get(ActiveRuleIndex.class);

    dbClient.ruleDao().insert(dbSession, xooRule1);

    // create pre-defined profiles P1 and P2
    OrganizationDto defaultOrganization = getDefaultOrganization(tester, dbClient, dbSession);
    dbClient.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(defaultOrganization), QProfileTesting.newXooP2(defaultOrganization));

    dbSession.commit();
    dbSession.clearCache();
    ruleIndexer.index();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void count_by_all_profiles() {
    logInAsQProfileAdministrator();

    service.activate(XOO_P1_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    service.activate(XOO_P2_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    dbSession.clearCache();
    activeRuleIndexer.index();

    Map<String, Long> counts = activeRuleIndex.countAllByQualityProfileKey();
    assertThat(counts).hasSize(2);
    assertThat(counts.keySet()).containsOnly(XOO_P1_KEY, XOO_P2_KEY);
    assertThat(counts.values()).containsOnly(1L, 1L);
  }

  @Test
  public void count_by_all_deprecated_profiles() {
    logInAsQProfileAdministrator();

    RuleDto xooRule2 = newXooX2().setStatus(RuleStatus.DEPRECATED);
    RuleDto xooRule3 = newXooX3().setStatus(RuleStatus.DEPRECATED);
    dbClient.ruleDao().insert(dbSession, xooRule2);
    dbClient.ruleDao().insert(dbSession, xooRule3);
    dbSession.commit();
    ruleIndexer.index();

    // active some rules
    service.activate(XOO_P1_KEY, new RuleActivation(xooRule1.getKey()));
    service.activate(XOO_P1_KEY, new RuleActivation(xooRule2.getKey()));
    service.activate(XOO_P1_KEY, new RuleActivation(xooRule3.getKey()));
    service.activate(XOO_P2_KEY, new RuleActivation(xooRule1.getKey()));
    service.activate(XOO_P2_KEY, new RuleActivation(xooRule3.getKey()));
    dbSession.commit();

    Map<String, Long> counts = activeRuleIndex.countAllDeprecatedByQualityProfileKey();
    assertThat(counts)
      .hasSize(2)
      .containsEntry(XOO_P1_KEY, 2L)
      .containsEntry(XOO_P2_KEY, 1L);
  }

  @Test
  public void stat_for_all_profiles() {
    logInAsQProfileAdministrator();

    service.activate(XOO_P1_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("MINOR"));
    service.activate(XOO_P2_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    dbSession.clearCache();
    activeRuleIndexer.index();

    Map<String, Multimap<String, FacetValue>> stats = loader.getAllProfileStats();

    assertThat(stats.size()).isEqualTo(2);
    assertThat(stats.get(XOO_P1_KEY).size()).isEqualTo(3);
    assertThat(stats.get(XOO_P1_KEY).get(RuleIndexDefinition.FIELD_ACTIVE_RULE_SEVERITY).size()).isEqualTo(1);
    assertThat(stats.get(XOO_P1_KEY).get(RuleIndexDefinition.FIELD_ACTIVE_RULE_INHERITANCE).size()).isEqualTo(1);
    assertThat(stats.get(XOO_P1_KEY).get("countActiveRules").size()).isEqualTo(1);
  }

  public static class XooExporter extends ProfileExporter {
    public XooExporter() {
      super("xootool", "Xoo Tool");
    }

    @Override
    public String[] getSupportedLanguages() {
      return new String[] {"xoo"};
    }

    @Override
    public String getMimeType() {
      return "plain/custom";
    }

    @Override
    public void exportProfile(RulesProfile profile, Writer writer) {
      try {
        writer.write("xoo -> " + profile.getName() + " -> " + profile.getActiveRules().size());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public static class XooProfileImporter extends ProfileImporter {
    public XooProfileImporter() {
      super("XooProfileImporter", "Xoo Profile Importer");
    }

    @Override
    public String[] getSupportedLanguages() {
      return new String[] {"xoo"};
    }

    @Override
    public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
      RulesProfile rulesProfile = RulesProfile.create();
      Rule rule = Rule.create("xoo", "R1");
      rule.createParameter("acceptWhitespace");
      org.sonar.api.rules.ActiveRule activeRule = rulesProfile.activateRule(rule, RulePriority.CRITICAL);
      activeRule.setParameter("acceptWhitespace", "true");
      return rulesProfile;
    }
  }

  private void logInAsQProfileAdministrator() {
    userSessionRule.logIn().addPermission(ADMINISTER_QUALITY_PROFILES, tester.get(DefaultOrganizationProvider.class).get().getUuid());
  }
}
