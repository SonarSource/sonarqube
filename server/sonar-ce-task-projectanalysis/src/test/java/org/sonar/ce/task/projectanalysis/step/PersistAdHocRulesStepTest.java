/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.issue.AdHocRuleCreator;
import org.sonar.ce.task.projectanalysis.issue.NewAdHocRule;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.es.EsTester;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistAdHocRulesStepTest extends BaseStepTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();

  private ComputationStep underTest;
  private RuleRepositoryImpl ruleRepository;

  @org.junit.Rule
  public EsTester es = EsTester.create();

  private RuleIndexer indexer = new RuleIndexer(es.client(), dbClient);
  private AdHocRuleCreator adHocRuleCreator = new AdHocRuleCreator(dbClient, System2.INSTANCE, indexer, new SequenceUuidFactory());

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setup() {
    ruleRepository = new RuleRepositoryImpl(adHocRuleCreator, dbClient);
    underTest = new PersistAdHocRulesStep(dbClient, ruleRepository);
  }

  @Test
  public void persist_and_index_new_ad_hoc_rules() {
    RuleKey ruleKey = RuleKey.of("external_eslint", "no-cond-assign");
    ruleRepository.addOrUpdateAddHocRuleIfNeeded(ruleKey,
      () -> new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build()));

    underTest.execute(new TestComputationStepContext());

    RuleDao ruleDao = dbClient.ruleDao();
    Optional<RuleDto> ruleDefinitionDtoOptional = ruleDao.selectByKey(dbClient.openSession(false), ruleKey);
    assertThat(ruleDefinitionDtoOptional).isPresent();

    RuleDto reloaded = ruleDefinitionDtoOptional.get();
    assertThat(reloaded.getRuleKey()).isEqualTo("no-cond-assign");
    assertThat(reloaded.getRepositoryKey()).isEqualTo("external_eslint");
    assertThat(reloaded.isExternal()).isTrue();
    assertThat(reloaded.isAdHoc()).isTrue();
    assertThat(reloaded.getType()).isZero();
    assertThat(reloaded.getSeverity()).isNull();
    assertThat(reloaded.getName()).isEqualTo("eslint:no-cond-assign");

    assertThat(es.countDocuments(RuleIndexDefinition.TYPE_RULE)).isOne();
    assertThat(es.getDocuments(RuleIndexDefinition.TYPE_RULE).iterator().next().getId()).isEqualTo(reloaded.getUuid());
  }

  @Test
  public void do_not_persist_existing_external_rules() {
    RuleKey ruleKey = RuleKey.of("eslint", "no-cond-assign");
    db.rules().insert(ruleKey, r -> r.setIsExternal(true));
    ruleRepository.addOrUpdateAddHocRuleIfNeeded(ruleKey,
      () -> new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build()));

    underTest.execute(new TestComputationStepContext());

    RuleDao ruleDao = dbClient.ruleDao();
    assertThat(ruleDao.selectAll(dbClient.openSession(false), "org")).hasSize(1);
    assertThat(es.countDocuments(RuleIndexDefinition.TYPE_RULE)).isZero();
  }

}
