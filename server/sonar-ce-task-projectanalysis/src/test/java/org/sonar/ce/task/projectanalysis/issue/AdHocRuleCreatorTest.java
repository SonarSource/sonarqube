/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.es.EsTester;
import org.sonar.server.rule.index.RuleIndexer;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;

public class AdHocRuleCreatorTest {

  @org.junit.Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @org.junit.Rule
  public EsTester es = EsTester.create();

  private RuleIndexer indexer = new RuleIndexer(es.client(), db.getDbClient());
  private AdHocRuleCreator underTest = new AdHocRuleCreator(db.getDbClient(), System2.INSTANCE, indexer);
  private DbSession dbSession = db.getSession();

  @Test
  public void create_ad_hoc_rule_from_issue() {
    OrganizationDto organization = db.organizations().insert();
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.ExternalIssue.newBuilder().setEngineId("eslint").setRuleId("no-cond-assign").build());

    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule, organization);

    assertThat(rule).isNotNull();
    assertThat(rule.isExternal()).isTrue();
    assertThat(rule.isAdHoc()).isTrue();
    assertThat(rule.getId()).isGreaterThan(0);
    assertThat(rule.getKey()).isEqualTo(RuleKey.of("external_eslint", "no-cond-assign"));
    assertThat(rule.getName()).isEqualTo("eslint:no-cond-assign");
    assertThat(rule.getDescription()).isNull();
    assertThat(rule.getSeverity()).isNull();
    assertThat(rule.getType()).isEqualTo(0);
    assertThat(rule.getMetadata().getAdHocName()).isNull();
    assertThat(rule.getMetadata().getAdHocDescription()).isNull();
    assertThat(rule.getMetadata().getAdHocSeverity()).isNull();
    assertThat(rule.getMetadata().getAdHocType()).isNull();
  }

  @Test
  public void create_ad_hoc_rule_from_scanner_report() {
    OrganizationDto organization = db.organizations().insert();
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName("No condition assigned")
      .setDescription("A description")
      .setSeverity(Constants.Severity.BLOCKER)
      .setType(ScannerReport.IssueType.BUG)
      .build());

    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule, organization);

    assertThat(rule).isNotNull();
    assertThat(rule.isExternal()).isTrue();
    assertThat(rule.isAdHoc()).isTrue();
    assertThat(rule.getId()).isGreaterThan(0);
    assertThat(rule.getKey()).isEqualTo(RuleKey.of("external_eslint", "no-cond-assign"));
    assertThat(rule.getName()).isEqualTo("eslint:no-cond-assign");
    assertThat(rule.getDescription()).isNull();
    assertThat(rule.getSeverity()).isNull();
    assertThat(rule.getType()).isEqualTo(0);
    assertThat(rule.getMetadata().getAdHocName()).isEqualTo("No condition assigned");
    assertThat(rule.getMetadata().getAdHocDescription()).isEqualTo("A description");
    assertThat(rule.getMetadata().getAdHocSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(rule.getMetadata().getAdHocType()).isEqualTo(RuleType.BUG.getDbConstant());
  }

  @Test
  public void truncate_metadata_name_and_desc_if_longer_than_max_value() {
    OrganizationDto organization = db.organizations().insert();
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName(repeat("a", 201))
      .setDescription(repeat("a", 16_777_216))
      .setSeverity(Constants.Severity.BLOCKER)
      .setType(ScannerReport.IssueType.BUG)
      .build());

    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule, organization);

    assertThat(rule.getMetadata().getAdHocName()).isEqualTo(repeat("a", 200));
    assertThat(rule.getMetadata().getAdHocDescription()).isEqualTo(repeat("a", 16_777_215));
  }

  @Test
  public void update_metadata_only() {
    OrganizationDto organization = db.organizations().insert();
    NewAdHocRule addHocRule = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName("No condition assigned")
      .setDescription("A description")
      .setSeverity(Constants.Severity.BLOCKER)
      .setType(ScannerReport.IssueType.BUG)
      .build());
    RuleDto rule = underTest.persistAndIndex(dbSession, addHocRule, organization);
    long creationDate = rule.getCreatedAt();
    NewAdHocRule addHocRuleUpdated = new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId("no-cond-assign")
      .setName("No condition assigned updated")
      .setDescription("A description updated")
      .setSeverity(Constants.Severity.CRITICAL)
      .setType(ScannerReport.IssueType.CODE_SMELL)
      .build());

    RuleDto ruleUpdated = underTest.persistAndIndex(dbSession, addHocRuleUpdated, organization);

    assertThat(ruleUpdated).isNotNull();
    assertThat(ruleUpdated.isExternal()).isTrue();
    assertThat(ruleUpdated.isAdHoc()).isTrue();
    assertThat(ruleUpdated.getId()).isGreaterThan(0);
    assertThat(ruleUpdated.getKey()).isEqualTo(RuleKey.of("external_eslint", "no-cond-assign"));
    assertThat(ruleUpdated.getName()).isEqualTo("eslint:no-cond-assign");
    assertThat(ruleUpdated.getDescription()).isNull();
    assertThat(ruleUpdated.getSeverity()).isNull();
    assertThat(ruleUpdated.getType()).isEqualTo(0);
    assertThat(ruleUpdated.getMetadata().getAdHocName()).isEqualTo("No condition assigned updated");
    assertThat(ruleUpdated.getMetadata().getAdHocDescription()).isEqualTo("A description updated");
    assertThat(ruleUpdated.getMetadata().getAdHocSeverity()).isEqualTo(Severity.CRITICAL);
    assertThat(ruleUpdated.getMetadata().getAdHocType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());

    assertThat(ruleUpdated.getDefinition().getCreatedAt()).isEqualTo(creationDate);
    assertThat(ruleUpdated.getMetadata().getCreatedAt()).isEqualTo(creationDate);
    assertThat(ruleUpdated.getMetadata().getUpdatedAt()).isGreaterThan(creationDate);
  }

  @Test
  public void does_not_update_rule_when_no_change() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto rule = db.rules().insert(r -> r.setRepositoryKey("external_eslint").setIsExternal(true).setIsAdHoc(true));
    RuleMetadataDto ruleMetadata = db.rules().insertOrUpdateMetadata(rule, organization);

    RuleDto ruleUpdated = underTest.persistAndIndex(dbSession, new NewAdHocRule(ScannerReport.AdHocRule.newBuilder()
      .setEngineId("eslint")
      .setRuleId(rule.getKey().rule())
      .setName(ruleMetadata.getAdHocName())
      .setDescription(ruleMetadata.getAdHocDescription())
      .setSeverity(Constants.Severity.valueOf(ruleMetadata.getAdHocSeverity()))
      .setType(ScannerReport.IssueType.forNumber(ruleMetadata.getAdHocType()))
      .build()),
      organization);

    assertThat(ruleUpdated).isNotNull();
    assertThat(ruleUpdated.isExternal()).isTrue();
    assertThat(ruleUpdated.isAdHoc()).isTrue();
    assertThat(ruleUpdated.getKey()).isEqualTo(rule.getKey());
    assertThat(ruleUpdated.getName()).isEqualTo(rule.getName());
    assertThat(ruleUpdated.getDescription()).isEqualTo(rule.getDescription());
    assertThat(ruleUpdated.getSeverity()).isEqualTo(rule.getSeverity());
    assertThat(ruleUpdated.getType()).isEqualTo(rule.getType());
    assertThat(ruleUpdated.getDefinition().getCreatedAt()).isEqualTo(rule.getCreatedAt());
    assertThat(ruleUpdated.getDefinition().getUpdatedAt()).isEqualTo(rule.getUpdatedAt());

    assertThat(ruleUpdated.getMetadata().getAdHocName()).isEqualTo(ruleMetadata.getAdHocName());
    assertThat(ruleUpdated.getMetadata().getAdHocDescription()).isEqualTo(ruleMetadata.getAdHocDescription());
    assertThat(ruleUpdated.getMetadata().getAdHocSeverity()).isEqualTo(ruleMetadata.getAdHocSeverity());
    assertThat(ruleUpdated.getMetadata().getAdHocType()).isEqualTo(ruleMetadata.getAdHocType());
    assertThat(ruleUpdated.getMetadata().getCreatedAt()).isEqualTo(rule.getCreatedAt());
    assertThat(ruleUpdated.getMetadata().getUpdatedAt()).isEqualTo(rule.getUpdatedAt());
  }

}
