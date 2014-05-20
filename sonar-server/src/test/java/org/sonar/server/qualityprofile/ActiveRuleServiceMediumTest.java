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
package org.sonar.server.qualityprofile;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleServiceMediumTest {

  @Rule
  public ServerTester tester = new ServerTester().addComponents(XooRulesDefinition.class);

  DbClient dbClient;
  DbSession dbSession;
  ActiveRuleService service;

  @Before
  public void before() {
    dbClient = tester.get(DbClient.class);
    dbSession = dbClient.openSession(false);
    service = tester.get(ActiveRuleService.class);

    // create quality profile
    dbClient.qualityProfileDao().insert(QualityProfileDto.createFor("MyProfile", "xoo"), dbSession);
    dbSession.commit();
  }

  @Test
  public void activate() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("marius");
    QualityProfileKey profileKey = QualityProfileKey.of("MyProfile", "xoo");
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    service.activate(activation);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();

    // verify es
    ActiveRuleIndex index = tester.get(ActiveRuleIndex.class);
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
  }

  @Test
  public void activate_with_default_severity() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("marius");
    QualityProfileKey profileKey = QualityProfileKey.of("MyProfile", "xoo");
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    service.activate(activation);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.MINOR);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();

    // verify es
    ActiveRuleIndex index = tester.get(ActiveRuleIndex.class);
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.MINOR);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
  }

  @Test
  public void update_activation_severity_and_parameters() throws Exception {
    // initial activation
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("marius");
    QualityProfileKey profileKey = QualityProfileKey.of("MyProfile", "xoo");
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    service.activate(activation);

    // update
    RuleActivation update = new RuleActivation(ActiveRuleKey.of(profileKey, RuleKey.of("xoo", "x1")));
    update.setSeverity(Severity.CRITICAL);
    service.activate(update);

    // verify db
    List<ActiveRuleDto> activeRuleDtos = dbClient.activeRuleDao().findByProfileKey(profileKey, dbSession);
    assertThat(activeRuleDtos).hasSize(1);
    assertThat(activeRuleDtos.get(0).getSeverityString()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRuleDtos.get(0).getInheritance()).isNull();

    // verify es
    ActiveRuleIndex index = tester.get(ActiveRuleIndex.class);
    index.refresh();
    ActiveRule activeRule = index.getByKey(activation.getKey());
    assertThat(activeRule).isNotNull();
    assertThat(activeRule.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(activeRule.inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
  }

  @Test
  @Ignore
  public void activate_with_params() throws Exception {

  }

  @Test
  @Ignore
  public void activate_with_default_params() throws Exception {

  }

  @Test
  @Ignore
  public void fail_to_activate_if_not_granted() throws Exception {

  }

  @Test
  @Ignore
  public void fail_to_activate_if_different_languages() throws Exception {
    // profile and rule have different languages
  }

  @Test
  @Ignore
  public void fail_to_activate_if_invalid_parameter() throws Exception {

  }

  public static class XooRulesDefinition implements RulesDefinition {
    @Override
    public void define(Context context) {
      NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
      repository.createRule("x1")
        .setName("x1 name")
        .setHtmlDescription("x1 desc")
        .setSeverity(Severity.MINOR)
        .createParam("max")
        .setDefaultValue("10")
        .setType(RuleParamType.INTEGER)
        .setDescription("Maximum");
      repository.done();
    }
  }
}
