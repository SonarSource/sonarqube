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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.index.ActiveRuleDoc;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_KEY;

public class QProfileServiceMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withStartupTasks().withEsIndexes().addComponents(XooProfileImporter.class, XooExporter.class);

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;
  QProfileService service;
  QProfileLoader loader;
  RuleActivator activator;
  RuleIndexer ruleIndexer;
  ActiveRuleIndexer activeRuleIndexer;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    service = tester.get(QProfileService.class);
    loader = tester.get(QProfileLoader.class);
    activator = tester.get(RuleActivator.class);
    ruleIndexer = tester.get(RuleIndexer.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR");
    db.ruleDao().insert(dbSession, xooRule1);

    // create pre-defined profiles P1 and P2
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(), QProfileTesting.newXooP2());

    dbSession.commit();
    dbSession.clearCache();
    ruleIndexer.index();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void create_profile() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    QualityProfileDto profile = service.create(QProfileName.createFor("xoo", "New Profile"), null).profile();

    assertThat(loader.getByKey(profile.getKey())).isNotNull();
    assertThat(loader.getByKey(profile.getKey()).getLanguage()).isEqualTo("xoo");
    assertThat(loader.getByKey(profile.getKey()).getName()).isEqualTo("New Profile");
  }

  @Test
  public void create_profile_with_xml() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    db.ruleDao().insert(dbSession, RuleTesting.newDto(RuleKey.of("xoo", "R1")).setLanguage("xoo").setSeverity("MINOR"));
    dbSession.commit();
    ruleIndexer.index();

    QProfileResult result = service.create(QProfileName.createFor("xoo", "New Profile"), ImmutableMap.of("XooProfileImporter", "<xml/>"));
    QualityProfileDto profile = result.profile();

    assertThat(loader.getByKey(profile.getKey())).isNotNull();
    assertThat(loader.getByKey(profile.getKey()).getLanguage()).isEqualTo("xoo");
    assertThat(loader.getByKey(profile.getKey()).getName()).isEqualTo("New Profile");

    List<ActiveRuleDoc> activeRules = Lists.newArrayList(loader.findActiveRulesByProfile(profile.getKey()));
    assertThat(activeRules).hasSize(1);
  }

  @Test
  public void count_by_all_profiles() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    service.activate(XOO_P1_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    service.activate(XOO_P2_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    dbSession.clearCache();
    activeRuleIndexer.index();

    Map<String, Long> counts = loader.countAllActiveRules();
    assertThat(counts).hasSize(2);
    assertThat(counts.keySet()).containsOnly(XOO_P1_KEY, XOO_P2_KEY);
    assertThat(counts.values()).containsOnly(1L, 1L);
  }

  @Test
  public void stat_for_all_profiles() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

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

  @Test
  public void count_by_deprecated() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    // create deprecated rule
    RuleDto deprecatedXooRule = RuleTesting.newDto(RuleKey.of("xoo", "deprecated1"))
      .setSeverity("MINOR").setLanguage("xoo").setStatus(RuleStatus.DEPRECATED);
    db.ruleDao().insert(dbSession, deprecatedXooRule);
    dbSession.commit();
    ruleIndexer.index();

    // active some rules
    service.activate(XOO_P1_KEY, new RuleActivation(deprecatedXooRule.getKey()).setSeverity("BLOCKER"));
    service.activate(XOO_P1_KEY, new RuleActivation(RuleTesting.XOO_X1).setSeverity("BLOCKER"));
    dbSession.commit();

    assertThat(loader.countDeprecatedActiveRulesByProfile(XOO_P1_KEY)).isEqualTo(1);
  }

  @Test
  public void search_qprofile_activity() {
    userSessionRule.login("david").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    UserDto user = new UserDto().setLogin("david").setName("David").setEmail("dav@id.com").setCreatedAt(System.currentTimeMillis()).setUpdatedAt(System.currentTimeMillis());
    db.userDao().insert(dbSession, user);
    dbSession.commit();

    // We need an actual rule in DB to test RuleName in Activity
    RuleDto rule = db.ruleDao().selectOrFailByKey(dbSession, RuleTesting.XOO_X1);

    tester.get(ActivityService.class).save(ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1))
      .setSeverity(Severity.MAJOR)
      .setParameter("max", "10").toActivity());

    Result<QProfileActivity> activities = service.searchActivities(new QProfileActivityQuery(), new SearchOptions());
    assertThat(activities.getHits()).hasSize(1);

    QProfileActivity activity = activities.getHits().get(0);
    assertThat(activity.getType()).isEqualTo(Activity.Type.QPROFILE.name());
    assertThat(activity.getAction()).isEqualTo(ActiveRuleChange.Type.ACTIVATED.name());
    assertThat(activity.ruleKey()).isEqualTo(RuleTesting.XOO_X1);
    assertThat(activity.profileKey()).isEqualTo(XOO_P1_KEY);
    assertThat(activity.severity()).isEqualTo(Severity.MAJOR);
    assertThat(activity.ruleName()).isEqualTo(rule.getName());
    assertThat(activity.getLogin()).isEqualTo("david");
    assertThat(activity.authorName()).isEqualTo("David");

    assertThat(activity.parameters()).hasSize(1);
    assertThat(activity.parameters().get("max")).isEqualTo("10");
  }

  @Test
  public void search_qprofile_activity_without_severity() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    RuleKey ruleKey = RuleKey.of("xoo", "deleted_rule");

    tester.get(ActivityService.class).save(ActiveRuleChange.createFor(ActiveRuleChange.Type.UPDATED, ActiveRuleKey.of(XOO_P1_KEY, ruleKey))
      .setParameter("max", "10").toActivity()
      );

    Result<QProfileActivity> activities = service.searchActivities(new QProfileActivityQuery(), new SearchOptions());
    assertThat(activities.getHits()).hasSize(1);

    QProfileActivity activity = activities.getHits().get(0);
    assertThat(activity.severity()).isNull();
    assertThat(activity.parameters()).hasSize(1);
    assertThat(activity.parameters().get("max")).isEqualTo("10");
  }

  @Test
  public void search_qprofile_activity_with_user_not_found() {
    userSessionRule.login("david").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    // We need an actual rule in DB to test RuleName in Activity
    // TODO ???
    // db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    // dbSession.commit();

    tester.get(ActivityService.class).save(
      ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1))
        .setSeverity(Severity.MAJOR)
        .setParameter("max", "10")
        .toActivity()
      );

    Result<QProfileActivity> activities = service.searchActivities(new QProfileActivityQuery(), new SearchOptions());
    assertThat(activities.getHits()).hasSize(1);

    QProfileActivity activity = activities.getHits().get(0);
    assertThat(activity.getLogin()).isEqualTo("david");
    assertThat(activity.authorName()).isNull();
  }

  @Test
  public void search_qprofile_activity_with_rule_not_found() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    RuleKey ruleKey = RuleKey.of("xoo", "deleted_rule");

    tester.get(ActivityService.class).save(ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, ruleKey))
      .setSeverity(Severity.MAJOR)
      .setParameter("max", "10")
      .toActivity()
      );
    dbSession.commit();

    Result<QProfileActivity> activities = service.searchActivities(new QProfileActivityQuery(), new SearchOptions());
    assertThat(activities.getHits()).hasSize(1);

    QProfileActivity activity = activities.getHits().get(0);
    assertThat(activity.ruleKey()).isEqualTo(ruleKey);
    assertThat(activity.ruleName()).isNull();
  }

  @Test
  public void search_activity_by_qprofile() {

    tester.get(ActivityService.class).save(
      ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P1_KEY, RuleTesting.XOO_X1)).toActivity());
    tester.get(ActivityService.class).save(
      ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of(XOO_P2_KEY, RuleTesting.XOO_X1)).toActivity());

    // 0. Base case verify 2 activities in index
    assertThat(service.searchActivities(new QProfileActivityQuery(), new SearchOptions()).getHits())
      .hasSize(2);

    // 1. filter by QProfile
    List<QProfileActivity> result = service.searchActivities(new QProfileActivityQuery()
      .setQprofileKey(XOO_P1_KEY), new SearchOptions()).getHits();
    assertThat(result).hasSize(1);
  }

  @Test
  public void search_activity_by_qprofile_having_dashes_in_keys() {
    tester.get(ActivityService.class).save(
      ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of("java-default", RuleTesting.XOO_X1)).toActivity());
    tester.get(ActivityService.class).save(
      ActiveRuleChange.createFor(ActiveRuleChange.Type.ACTIVATED, ActiveRuleKey.of("java-toto", RuleTesting.XOO_X1)).toActivity());

    // 0. Base case verify 2 activities in index
    assertThat(service.searchActivities(new QProfileActivityQuery(), new SearchOptions()).getHits())
      .hasSize(2);

    // 1. filter by QProfile
    List<QProfileActivity> result = service.searchActivities(new QProfileActivityQuery()
      .setQprofileKey("java-default"), new SearchOptions()).getHits();
    assertThat(result).hasSize(1);
  }

  @Test
  public void set_default() {
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    assertThat(service.getDefault("xoo")).isNull();

    service.setDefault(XOO_P1_KEY);
    dbSession.clearCache();

    assertThat(service.getDefault("xoo").getKey()).isEqualTo(XOO_P1_KEY);
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

}
