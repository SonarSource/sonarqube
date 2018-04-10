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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.issue.NewExternalRule;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.server.computation.task.step.ComputationStep;
import org.sonar.server.es.EsTester;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.Severity.BLOCKER;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.server.organization.TestDefaultOrganizationProvider.from;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;

public class PersistExternalRulesStepTest extends BaseStepTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setOrganizationUuid("org-1", "qg-uuid-1");

  private DbClient dbClient = db.getDbClient();
  private System2 system2 = System2.INSTANCE;

  private ComputationStep underTest;
  private RuleRepositoryImpl ruleRepository;

  @org.junit.Rule
  public EsTester es = new EsTester(new RuleIndexDefinition(new MapSettings().asConfig()));

  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private RuleCreator creator = new RuleCreator(System2.INSTANCE, ruleIndexer, db.getDbClient(), newFullTypeValidations(), from(db));


  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setup() {
    ruleRepository = new RuleRepositoryImpl(creator, dbClient, analysisMetadataHolder);
    underTest = new PersistExternalRulesStep(dbClient, ruleRepository);
  }

  @Test
  public void persist_new_external_rules() {

    RuleKey ruleKey = RuleKey.of("eslint", "no-cond-assign");
    ruleRepository.insertNewExternalRuleIfAbsent(ruleKey, () -> new NewExternalRule.Builder()
      .setKey(ruleKey)
      .setPluginKey("eslint")
      .setName("disallow assignment operators in conditional statements (no-cond-assign)")
      .setDescriptionUrl("https://eslint.org/docs/rules/no-cond-assign")
      .setSeverity(BLOCKER)
      .setType(BUG)
      .build());

    underTest.execute();

    RuleDao ruleDao = dbClient.ruleDao();
    Optional<RuleDefinitionDto> ruleDefinitionDtoOptional = ruleDao.selectDefinitionByKey(dbClient.openSession(false), ruleKey);
    assertThat(ruleDefinitionDtoOptional).isPresent();

    RuleDefinitionDto reloaded = ruleDefinitionDtoOptional.get();
    assertThat(reloaded.getRuleKey()).isEqualTo("no-cond-assign");
    assertThat(reloaded.getRepositoryKey()).isEqualTo("eslint");
    assertThat(reloaded.isExternal()).isTrue();
    assertThat(reloaded.getType()).isEqualTo(2);
    assertThat(reloaded.getSeverity()).isEqualTo(4);
    assertThat(reloaded.getDescriptionURL()).isEqualTo("https://eslint.org/docs/rules/no-cond-assign");
    assertThat(reloaded.getName()).isEqualTo("disallow assignment operators in conditional statements (no-cond-assign)");
    assertThat(reloaded.getPluginKey()).isEqualTo("eslint");


  }

}
