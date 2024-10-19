/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.es.EsTester;
import org.sonar.server.rule.index.RuleIndexer;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AdHocRuleCreatorIT {

  @org.junit.Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @org.junit.Rule
  public EsTester es = EsTester.create();

  private RuleIndexer indexer = new RuleIndexer(es.client(), db.getDbClient());
  private AdHocRuleCreator underTest = new AdHocRuleCreator(db.getDbClient(), System2.INSTANCE, indexer, new SequenceUuidFactory());
  private DbSession dbSession = db.getSession();

  @Test
  public void create_ad_hoc_rule_from_issue() {
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build());

    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule);

    assertThat(rule).isNotNull();
    assertThat(rule.isExternal()).isTrue();
    assertThat(rule.isAdHoc()).isTrue();
    assertThat(rule.getUuid()).isNotBlank();
    assertThat(rule.getKey()).isEqualTo(RuleKey.of("external_eslint", "no-cond-assign"));
    assertThat(rule.getName()).isEqualTo("eslint:no-cond-assign");
    assertThat(rule.getRuleDescriptionSectionDtos()).isEmpty();
    assertThat(rule.getSeverity()).isNull();
    assertThat(rule.getType()).isZero();
    assertThat(rule.getAdHocName()).isNull();
    assertThat(rule.getAdHocDescription()).isNull();
    assertThat(rule.getAdHocSeverity()).isNull();
    assertThat(rule.getAdHocType()).isNull();
    assertThat(rule.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.defaultCleanCodeAttribute());
    assertThat(rule.getDefaultImpacts()).extracting(i -> i.getSoftwareQuality(), i -> i.getSeverity())
      .containsExactly(Tuple.tuple(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.MEDIUM));
  }

  @Test
  public void create_ad_hoc_rule_from_scanner_report() {
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName("No condition assigned")
      .setDescription("A description")
      .setSeverity(Constants.Severity.BLOCKER)
      .setType(ScannerReport.IssueType.BUG)
      .setCleanCodeAttribute(CleanCodeAttribute.DISTINCT.name())
      .addDefaultImpacts(ScannerReport.Impact.newBuilder().setSoftwareQuality(SoftwareQuality.RELIABILITY.name())
        .setSeverity(org.sonar.api.issue.impact.Severity.LOW.name()).build())
      .build());

    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule);

    assertThat(rule).isNotNull();
    assertThat(rule.isExternal()).isTrue();
    assertThat(rule.isAdHoc()).isTrue();
    assertThat(rule.getUuid()).isNotBlank();
    assertThat(rule.getKey()).isEqualTo(RuleKey.of("external_eslint", "no-cond-assign"));
    assertThat(rule.getName()).isEqualTo("eslint:no-cond-assign");
    assertThat(rule.getRuleDescriptionSectionDtos()).isEmpty();
    assertThat(rule.getSeverity()).isNull();
    assertThat(rule.getType()).isZero();
    assertThat(rule.getAdHocName()).isEqualTo("No condition assigned");
    assertThat(rule.getAdHocDescription()).isEqualTo("A description");
    assertThat(rule.getAdHocSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.getAdHocType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(rule.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.DISTINCT);
    assertThat(rule.getDefaultImpacts()).extracting(i -> i.getSoftwareQuality(), i -> i.getSeverity())
      .containsExactly(tuple(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW));
  }

  @Test
  public void truncate_metadata_name_and_desc_if_longer_than_max_value() {
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName(repeat("a", 201))
      .setDescription(repeat("a", 16_777_216))
      .setSeverity(Constants.Severity.BLOCKER)
      .setType(ScannerReport.IssueType.BUG)
      .build());

    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule);

    assertThat(rule.getAdHocName()).isEqualTo(repeat("a", 200));
    assertThat(rule.getAdHocDescription()).isEqualTo(repeat("a", 16_777_215));
  }

  @Test
  public void update_metadata_only() {
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName("No condition assigned")
      .setDescription("A description")
      .setSeverity(Constants.Severity.BLOCKER)
      .setType(ScannerReport.IssueType.BUG)
      .build());
    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule);
    long creationDate = rule.getCreatedAt();
    NewAdHocRule addHocRuleUpdated = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName("No condition assigned updated")
      .setDescription("A description updated")
      .setSeverity(Constants.Severity.CRITICAL)
      .setType(ScannerReport.IssueType.CODE_SMELL)
      .build());

    RuleDto ruleUpdated = underTest.persistAndIndex(dbSession, addHocRuleUpdated);

    assertThat(ruleUpdated).isNotNull();
    assertThat(ruleUpdated.isExternal()).isTrue();
    assertThat(ruleUpdated.isAdHoc()).isTrue();
    assertThat(ruleUpdated.getUuid()).isNotBlank();
    assertThat(ruleUpdated.getKey()).isEqualTo(RuleKey.of("external_eslint", "no-cond-assign"));
    assertThat(ruleUpdated.getName()).isEqualTo("eslint:no-cond-assign");
    assertThat(ruleUpdated.getRuleDescriptionSectionDtos()).isEmpty();
    assertThat(ruleUpdated.getSeverity()).isNull();
    assertThat(ruleUpdated.getType()).isZero();
    assertThat(ruleUpdated.getAdHocName()).isEqualTo("No condition assigned updated");
    assertThat(ruleUpdated.getAdHocDescription()).isEqualTo("A description updated");
    assertThat(ruleUpdated.getAdHocSeverity()).isEqualTo(Severity.CRITICAL);
    assertThat(ruleUpdated.getAdHocType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(ruleUpdated.getCreatedAt()).isEqualTo(creationDate);
    assertThat(ruleUpdated.getUpdatedAt()).isGreaterThan(creationDate);
  }

  @Test
  public void does_not_update_rule_when_no_change() {
    RuleDto rule = db.rules().insert(r -> r.setRepositoryKey("external_eslint").setIsExternal(true).setIsAdHoc(true));

    RuleDto ruleUpdated = underTest.persistAndIndex(dbSession, new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId(rule.getKey().rule())
      .setName(rule.getAdHocName())
      .setDescription(rule.getAdHocDescription())
      .setSeverity(Constants.Severity.valueOf(rule.getAdHocSeverity()))
      .setType(ScannerReport.IssueType.forNumber(rule.getAdHocType()))
      .addDefaultImpacts(ScannerReport.Impact.newBuilder().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY.name())
        .setSeverity(org.sonar.api.issue.impact.Severity.HIGH.name()).build())
      .setCleanCodeAttribute(CleanCodeAttribute.CLEAR.name())
      .build()));

    assertThat(ruleUpdated).isNotNull();
    assertThat(ruleUpdated.isExternal()).isTrue();
    assertThat(ruleUpdated.isAdHoc()).isTrue();
    assertThat(ruleUpdated.getKey()).isEqualTo(rule.getKey());
    assertThat(ruleUpdated.getName()).isEqualTo(rule.getName());
    assertThat(ruleUpdated.getRuleDescriptionSectionDtos()).usingRecursiveFieldByFieldElementComparator().isEqualTo(rule.getRuleDescriptionSectionDtos());
    assertThat(ruleUpdated.getSeverity()).isEqualTo(rule.getSeverity());
    assertThat(ruleUpdated.getType()).isEqualTo(rule.getType());
    assertThat(ruleUpdated.getCreatedAt()).isEqualTo(rule.getCreatedAt());
    assertThat(ruleUpdated.getUpdatedAt()).isEqualTo(rule.getUpdatedAt());

    assertThat(ruleUpdated.getAdHocName()).isEqualTo(rule.getAdHocName());
    assertThat(ruleUpdated.getAdHocDescription()).isEqualTo(rule.getAdHocDescription());
    assertThat(ruleUpdated.getAdHocSeverity()).isEqualTo(rule.getAdHocSeverity());
    assertThat(ruleUpdated.getAdHocType()).isEqualTo(rule.getAdHocType());
  }

}
