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
package org.sonar.db.rule;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.es.RuleExtensionId;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.rule.RuleDto.Scope;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class RuleDaoTest {

  private static final String ORGANIZATION_UUID = "org-1";

  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private RuleDao underTest = db.getDbClient().ruleDao();
  private OrganizationDto organization;

  @Before
  public void before() {
    organization = db.organizations().insert(o -> o.setUuid(ORGANIZATION_UUID));
  }

  @Test
  public void selectByKey() {
    RuleDefinitionDto ruleDefinition = db.rules().insert();
    OrganizationDto organization = db.organizations().insert();
    RuleMetadataDto metadata = newRuleMetadata(organization, "1");
    db.rules().insertRule(ruleDefinition, metadata);

    assertThat(underTest.selectByKey(db.getSession(), organization, RuleKey.of("foo", "bar")))
      .isEmpty();
    RuleDto rule = underTest.selectByKey(db.getSession(), organization, ruleDefinition.getKey()).get();
    assertEquals(rule.getDefinition(), ruleDefinition);
    verifyMetadata(rule.getMetadata(), ruleDefinition, metadata);
  }

  @Test
  public void selectByKey_return_rule_even_if_organization_does_not_exist() {
    RuleDefinitionDto ruleDefinition = db.rules().insert();

    assertThat(underTest.selectByKey(db.getSession(), OrganizationTesting.newOrganizationDto(), ruleDefinition.getKey()))
      .isNotEmpty();
  }

  @Test
  public void selectByKey_populates_organizationUuid_even_when_organization_has_no_metadata() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto ruleDefinition = db.rules().insert();

    RuleDto rule = underTest.selectByKey(db.getSession(), organization, ruleDefinition.getKey()).get();
    verifyNoMetadata(rule.getMetadata(), ruleDefinition, organization);
  }

  @Test
  public void selectByKey_returns_metadata_of_specified_organization() {
    RuleDefinitionDto ruleDefinition = db.rules().insert();
    OrganizationDto organization1 = db.organizations().insert();
    RuleMetadataDto expectedOrg1 = newRuleMetadata(organization1, "1");
    db.rules().insertRule(ruleDefinition, expectedOrg1);
    OrganizationDto organization2 = db.organizations().insert();
    RuleMetadataDto expectedOrg2 = newRuleMetadata(organization2, "2");
    db.rules().insertRule(ruleDefinition, expectedOrg2);

    RuleDto rule = underTest.selectByKey(db.getSession(), organization1, ruleDefinition.getKey()).get();
    verifyMetadata(rule.getMetadata(), ruleDefinition, expectedOrg1);
    rule = underTest.selectByKey(db.getSession(), organization2, ruleDefinition.getKey()).get();
    verifyMetadata(rule.getMetadata(), ruleDefinition, expectedOrg2);
  }

  @Test
  public void selectDefinitionByKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionByKey(db.getSession(), RuleKey.of("NOT", "FOUND")).isPresent()).isFalse();

    Optional<RuleDefinitionDto> rule = underTest.selectDefinitionByKey(db.getSession(), RuleKey.of("java", "S001"));
    assertThat(rule.isPresent()).isTrue();
    assertThat(rule.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectById() {
    RuleDefinitionDto ruleDefinition = db.rules().insert();
    OrganizationDto organization = db.organizations().insert();
    RuleMetadataDto metadata = newRuleMetadata(organization, "1");
    RuleDto expected = db.rules().insertRule(ruleDefinition, metadata);

    assertThat(underTest.selectById(expected.getId() + 500, organization.getUuid(), db.getSession()))
      .isEmpty();
    RuleDto rule = underTest.selectById(expected.getId(), organization.getUuid(), db.getSession()).get();
    assertEquals(rule.getDefinition(), ruleDefinition);
    verifyMetadata(rule.getMetadata(), ruleDefinition, metadata);
  }

  @Test
  public void selectById_return_rule_even_if_organization_does_not_exist() {
    RuleDefinitionDto ruleDefinition = db.rules().insert();

    assertThat(underTest.selectById(ruleDefinition.getId(), "dfdfdf", db.getSession()))
      .isNotEmpty();
  }

  @Test
  public void selectById_populates_organizationUuid_even_when_organization_has_no_metadata() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto ruleDefinition = db.rules().insert();

    RuleDto rule = underTest.selectById(ruleDefinition.getId(), organization.getUuid(), db.getSession()).get();
    verifyNoMetadata(rule.getMetadata(), ruleDefinition, organization);
  }

  @Test
  public void selectById_returns_metadata_of_specified_organization() {
    RuleDefinitionDto ruleDefinition = db.rules().insert();
    OrganizationDto organization1 = db.organizations().insert();
    RuleMetadataDto expectedOrg1 = newRuleMetadata(organization1, "1");
    db.rules().insertRule(ruleDefinition, expectedOrg1);
    OrganizationDto organization2 = db.organizations().insert();
    RuleMetadataDto expectedOrg2 = newRuleMetadata(organization2, "2");
    db.rules().insertRule(ruleDefinition, expectedOrg2);

    RuleDto rule = underTest.selectById(ruleDefinition.getId(), organization1.getUuid(), db.getSession()).get();
    verifyMetadata(rule.getMetadata(), ruleDefinition, expectedOrg1);
    rule = underTest.selectById(ruleDefinition.getId(), organization2.getUuid(), db.getSession()).get();
    verifyMetadata(rule.getMetadata(), ruleDefinition, expectedOrg2);
  }

  @Test
  public void selectDefinitionById() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionById(55l, db.getSession())).isEmpty();
    Optional<RuleDefinitionDto> ruleDtoOptional = underTest.selectDefinitionById(1l, db.getSession());
    assertThat(ruleDtoOptional).isPresent();
    assertThat(ruleDtoOptional.get().getId()).isEqualTo(1);
  }

  @Test
  public void selectByIds() {
    db.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByIds(db.getSession(), organizationUuid, asList(1))).hasSize(1);
    assertThat(underTest.selectByIds(db.getSession(), organizationUuid, asList(1, 2))).hasSize(2);
    assertThat(underTest.selectByIds(db.getSession(), organizationUuid, asList(1, 2, 3))).hasSize(2);

    assertThat(underTest.selectByIds(db.getSession(), organizationUuid, asList(123))).isEmpty();
  }

  @Test
  public void selectByIds_populates_organizationUuid_even_when_organization_has_no_metadata() {
    db.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByIds(db.getSession(), organizationUuid, asList(1, 2)))
      .extracting(RuleDto::getOrganizationUuid)
      .containsExactly(organizationUuid, organizationUuid);
  }

  @Test
  public void selectDefinitionByIds() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionByIds(db.getSession(), asList(1))).hasSize(1);
    assertThat(underTest.selectDefinitionByIds(db.getSession(), asList(1, 2))).hasSize(2);
    assertThat(underTest.selectDefinitionByIds(db.getSession(), asList(1, 2, 3))).hasSize(2);

    assertThat(underTest.selectDefinitionByIds(db.getSession(), asList(123))).isEmpty();
  }

  @Test
  public void selectOrFailByKey() {
    db.prepareDbUnit(getClass(), "shared.xml");

    OrganizationDto organization = OrganizationTesting.newOrganizationDto().setUuid("org-1");
    RuleDto rule = underTest.selectOrFailByKey(db.getSession(), organization, RuleKey.of("java", "S001"));
    assertThat(rule.getId()).isEqualTo(1);
  }

  @Test
  public void selectOrFailByKey_fails_if_rule_not_found() {
    db.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Rule with key 'NOT:FOUND' does not exist");

    OrganizationDto organization = OrganizationTesting.newOrganizationDto().setUuid("org-1");
    underTest.selectOrFailByKey(db.getSession(), organization, RuleKey.of("NOT", "FOUND"));
  }

  @Test
  public void selectOrFailByKey_populates_organizationUuid_even_when_organization_has_no_metadata() {
    db.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    OrganizationDto organization = OrganizationTesting.newOrganizationDto().setUuid(organizationUuid);
    assertThat(underTest.selectOrFailByKey(db.getSession(), organization, RuleKey.of("java", "S001")).getOrganizationUuid())
      .isEqualTo(organizationUuid);
  }

  @Test
  public void selectOrFailDefinitionByKey_fails_if_rule_not_found() {
    db.prepareDbUnit(getClass(), "shared.xml");

    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Rule with key 'NOT:FOUND' does not exist");

    underTest.selectOrFailDefinitionByKey(db.getSession(), RuleKey.of("NOT", "FOUND"));
  }

  @Test
  public void selectByKeys() {
    db.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByKeys(db.getSession(), organizationUuid, Collections.emptyList())).isEmpty();
    assertThat(underTest.selectByKeys(db.getSession(), organizationUuid, asList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    List<RuleDto> rules = underTest.selectByKeys(db.getSession(), organizationUuid, asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getId()).isEqualTo(1);
  }

  @Test
  public void selectByKeys_populates_organizationUuid_even_when_organization_has_no_metadata() {
    db.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByKeys(db.getSession(), organizationUuid, asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER"))))
      .extracting(RuleDto::getOrganizationUuid)
      .containsExactly(organizationUuid);
  }

  @Test
  public void selectDefinitionByKeys() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectDefinitionByKeys(db.getSession(), Collections.emptyList())).isEmpty();
    assertThat(underTest.selectDefinitionByKeys(db.getSession(), asList(RuleKey.of("NOT", "FOUND")))).isEmpty();

    List<RuleDefinitionDto> rules = underTest.selectDefinitionByKeys(db.getSession(), asList(RuleKey.of("java", "S001"), RuleKey.of("java", "OTHER")));
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getId()).isEqualTo(1);
  }

  @Test
  public void selectAll() {
    OrganizationDto organization = db.organizations().insert();
    RuleDto rule1 = db.rules().insertRule(organization);
    RuleDto rule2 = db.rules().insertRule(organization);
    RuleDto rule3 = db.rules().insertRule(organization);

    assertThat(underTest.selectAll(db.getSession(), organization.getUuid()))
      .extracting(RuleDto::getId)
      .containsOnly(rule1.getId(), rule2.getId(), rule3.getId());
  }

  @Test
  public void selectAll_returns_all_rules_even_if_organization_does_not_exist() {
    RuleDefinitionDto rule1 = db.rules().insert();
    RuleDefinitionDto rule2 = db.rules().insert();
    RuleDefinitionDto rule3 = db.rules().insert();

    assertThat(underTest.selectAll(db.getSession(), "dfdfdf"))
      .extracting(RuleDto::getId)
      .containsOnly(rule1.getId(), rule2.getId(), rule3.getId());
  }

  @Test
  public void selectAll_populates_organizationUuid_even_when_organization_has_no_metadata() {
    OrganizationDto organization = db.organizations().insert();
    RuleDefinitionDto ruleDefinition1 = db.rules().insert();
    RuleDefinitionDto ruleDefinition2 = db.rules().insert();

    List<RuleDto> rules = underTest.selectAll(db.getSession(), organization.getUuid());
    assertThat(rules)
      .extracting(RuleDto::getId)
      .containsOnly(ruleDefinition1.getId(), ruleDefinition2.getId());
    assertThat(rules)
      .extracting(RuleDto::getOrganizationUuid)
      .containsExactly(organization.getUuid(), organization.getUuid());
  }

  @Test
  public void selectAll_returns_metadata_of_specified_organization() {
    RuleDefinitionDto ruleDefinition = db.rules().insert();
    OrganizationDto organization = db.organizations().insert();
    RuleMetadataDto expected = newRuleMetadata(organization, "1");
    db.rules().insertRule(ruleDefinition, expected);

    List<RuleDto> rules = underTest.selectAll(db.getSession(), organization.getUuid());
    assertThat(rules).hasSize(1);

    verifyMetadata(rules.iterator().next().getMetadata(), ruleDefinition, expected);
  }

  private void assertEquals(RuleDefinitionDto actual, RuleDefinitionDto expected) {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getRepositoryKey()).isEqualTo(expected.getRepositoryKey());
    assertThat(actual.getRuleKey()).isEqualTo(expected.getRuleKey());
    assertThat(actual.getKey()).isEqualTo(expected.getKey());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getDescriptionFormat()).isEqualTo(expected.getDescriptionFormat());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getConfigKey()).isEqualTo(expected.getConfigKey());
    assertThat(actual.getSeverity()).isEqualTo(expected.getSeverity());
    assertThat(actual.getSeverityString()).isEqualTo(expected.getSeverityString());
    assertThat(actual.isTemplate()).isEqualTo(expected.isTemplate());
    assertThat(actual.getLanguage()).isEqualTo(expected.getLanguage());
    assertThat(actual.getTemplateId()).isEqualTo(expected.getTemplateId());
    assertThat(actual.getDefRemediationFunction()).isEqualTo(expected.getDefRemediationFunction());
    assertThat(actual.getDefRemediationGapMultiplier()).isEqualTo(expected.getDefRemediationGapMultiplier());
    assertThat(actual.getDefRemediationBaseEffort()).isEqualTo(expected.getDefRemediationBaseEffort());
    assertThat(actual.getGapDescription()).isEqualTo(expected.getGapDescription());
    assertThat(actual.getSystemTags()).isEqualTo(expected.getSystemTags());
    assertThat(actual.getType()).isEqualTo(expected.getType());
    assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(actual.getUpdatedAt()).isEqualTo(expected.getUpdatedAt());
  }

  private static void verifyMetadata(RuleMetadataDto metadata, RuleDefinitionDto ruleDefinition, RuleMetadataDto expected) {
    assertThat(metadata.getOrganizationUuid()).isEqualTo(expected.getOrganizationUuid());
    assertThat(metadata.getRemediationBaseEffort()).isEqualTo(expected.getRemediationBaseEffort());
    assertThat(metadata.getRemediationFunction()).isEqualTo(expected.getRemediationFunction());
    assertThat(metadata.getRemediationGapMultiplier()).isEqualTo(expected.getRemediationGapMultiplier());
    assertThat(metadata.getTags()).isEqualTo(expected.getTags());
    assertThat(metadata.getNoteData()).isEqualTo(expected.getNoteData());
    assertThat(metadata.getNoteCreatedAt()).isEqualTo(expected.getNoteCreatedAt());
    assertThat(metadata.getNoteUpdatedAt()).isEqualTo(expected.getNoteUpdatedAt());
    assertThat(metadata.getCreatedAt()).isEqualTo(ruleDefinition.getCreatedAt());
    assertThat(metadata.getUpdatedAt()).isEqualTo(ruleDefinition.getUpdatedAt());
  }

  private static void verifyNoMetadata(RuleMetadataDto metadata, RuleDefinitionDto ruleDefinition, OrganizationDto organization) {
    assertThat(metadata.getOrganizationUuid()).isEqualTo(organization.getUuid());
    assertThat(metadata.getRemediationBaseEffort()).isNull();
    assertThat(metadata.getRemediationFunction()).isNull();
    assertThat(metadata.getRemediationGapMultiplier()).isNull();
    assertThat(metadata.getTags()).isEmpty();
    assertThat(metadata.getNoteData()).isNull();
    assertThat(metadata.getNoteCreatedAt()).isNull();
    assertThat(metadata.getNoteUpdatedAt()).isNull();
    assertThat(metadata.getCreatedAt()).isEqualTo(ruleDefinition.getCreatedAt());
    assertThat(metadata.getUpdatedAt()).isEqualTo(ruleDefinition.getUpdatedAt());
  }

  private static RuleMetadataDto newRuleMetadata(OrganizationDto organization, String seed) {
    String noteData = seed + randomAlphanumeric(7);
    return new RuleMetadataDto()
      .setOrganizationUuid(organization.getUuid())
      .setRemediationBaseEffort(seed + randomAlphanumeric(2))
      .setRemediationFunction(seed + randomAlphanumeric(3))
      .setRemediationGapMultiplier(seed + randomAlphanumeric(4))
      .setTags(of(seed + randomAlphanumeric(5), seed + randomAlphanumeric(6)))
      .setNoteData(noteData)
      .setNoteCreatedAt(noteData.hashCode() + 50L)
      .setNoteUpdatedAt(noteData.hashCode() + 1_999L)
      .setCreatedAt(seed.hashCode() + 8889L)
      .setUpdatedAt(seed.hashCode() + 10_333L);
  }

  @Test
  public void selectAllDefinitions() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<RuleDefinitionDto> ruleDtos = underTest.selectAllDefinitions(db.getSession());

    assertThat(ruleDtos).extracting("id").containsOnly(1, 2, 10);
  }

  @Test
  public void selectEnabled_with_ResultHandler() {
    db.prepareDbUnit(getClass(), "selectEnabled.xml");

    final List<RuleDefinitionDto> rules = new ArrayList<>();
    ResultHandler<RuleDefinitionDto> resultHandler = resultContext -> rules.add(resultContext.getResultObject());
    underTest.selectEnabled(db.getSession(), resultHandler);

    assertThat(rules.size()).isEqualTo(1);
    RuleDefinitionDto ruleDto = rules.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.HTML);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
  }

  @Test
  public void select_by_query() {
    db.prepareDbUnit(getClass(), "shared.xml");

    String organizationUuid = "org-1";
    assertThat(underTest.selectByQuery(db.getSession(), organizationUuid, RuleQuery.create())).hasSize(2);
    assertThat(underTest.selectByQuery(db.getSession(), organizationUuid, RuleQuery.create().withKey("S001"))).hasSize(1);
    assertThat(underTest.selectByQuery(db.getSession(), organizationUuid, RuleQuery.create().withConfigKey("S1"))).hasSize(1);
    assertThat(underTest.selectByQuery(db.getSession(), organizationUuid, RuleQuery.create().withRepositoryKey("java"))).hasSize(2);
    assertThat(underTest.selectByQuery(db.getSession(), organizationUuid,
      RuleQuery.create().withKey("S001").withConfigKey("S1").withRepositoryKey("java"))).hasSize(1);
  }

  @Test
  public void select_by_query_populates_organizationUuid_even_when_organization_has_no_metadata() {
    db.prepareDbUnit(getClass(), "shared.xml");
    String organizationUuid = "org-1";

    assertThat(underTest.selectByQuery(db.getSession(), organizationUuid, RuleQuery.create()))
      .extracting(RuleDto::getOrganizationUuid)
      .containsExactly(organizationUuid, organizationUuid);
  }

  @Test
  public void insert() {
    RuleDefinitionDto newRule = new RuleDefinitionDto()
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setLanguage("dart")
      .setTemplateId(3)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setType(RuleType.BUG)
      .setScope(Scope.ALL)
      .setCreatedAt(1_500_000_000_000L)
      .setUpdatedAt(2_000_000_000_000L);
    underTest.insert(db.getSession(), newRule);
    db.getSession().commit();

    RuleDefinitionDto ruleDto = underTest.selectOrFailDefinitionByKey(db.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getId()).isNotNull();
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getDescription()).isEqualTo("new description");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isEqualTo(0);
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.getTemplateId()).isEqualTo(3);
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getScope()).isEqualTo(Scope.ALL);
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2_000_000_000_000L);
  }

  @Test
  public void update_RuleDefinitionDto() {
    db.prepareDbUnit(getClass(), "update.xml");

    RuleDefinitionDto ruleToUpdate = new RuleDefinitionDto()
      .setId(1)
      .setRuleKey("NewRuleKey")
      .setRepositoryKey("plugin")
      .setName("new name")
      .setDescription("new description")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setStatus(RuleStatus.DEPRECATED)
      .setConfigKey("NewConfigKey")
      .setSeverity(Severity.INFO)
      .setIsTemplate(true)
      .setLanguage("dart")
      .setTemplateId(3)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("squid.S115.effortToFix")
      .setSystemTags(newHashSet("systag1", "systag2"))
      .setScope(Scope.ALL)
      .setType(RuleType.BUG)
      .setUpdatedAt(2_000_000_000_000L);

    underTest.update(db.getSession(), ruleToUpdate);
    db.getSession().commit();

    RuleDefinitionDto ruleDto = underTest.selectOrFailDefinitionByKey(db.getSession(), RuleKey.of("plugin", "NewRuleKey"));
    assertThat(ruleDto.getName()).isEqualTo("new name");
    assertThat(ruleDto.getDescription()).isEqualTo("new description");
    assertThat(ruleDto.getDescriptionFormat()).isEqualTo(RuleDto.Format.MARKDOWN);
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.DEPRECATED);
    assertThat(ruleDto.getRuleKey()).isEqualTo("NewRuleKey");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("plugin");
    assertThat(ruleDto.getConfigKey()).isEqualTo("NewConfigKey");
    assertThat(ruleDto.getSeverity()).isEqualTo(0);
    assertThat(ruleDto.getLanguage()).isEqualTo("dart");
    assertThat(ruleDto.isTemplate()).isTrue();
    assertThat(ruleDto.getTemplateId()).isEqualTo(3);
    assertThat(ruleDto.getDefRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isEqualTo("5d");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isEqualTo("10h");
    assertThat(ruleDto.getGapDescription()).isEqualTo("squid.S115.effortToFix");
    assertThat(ruleDto.getSystemTags()).containsOnly("systag1", "systag2");
    assertThat(ruleDto.getScope()).isEqualTo(Scope.ALL);
    assertThat(ruleDto.getType()).isEqualTo(RuleType.BUG.getDbConstant());
    assertThat(ruleDto.getCreatedAt()).isEqualTo(1_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(2_000_000_000_000L);
  }

  @Test
  public void update_RuleMetadataDto_inserts_row_in_RULE_METADATA_if_not_exists_yet() {
    db.prepareDbUnit(getClass(), "update.xml");
    String organizationUuid = "org-1";

    RuleMetadataDto metadataToUpdate = new RuleMetadataDto()
      .setRuleId(1)
      .setOrganizationUuid(organizationUuid)
      .setNoteData("My note")
      .setNoteUserLogin("admin")
      .setNoteCreatedAt(DateUtils.parseDate("2013-12-19").getTime())
      .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20").getTime())
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setRemediationGapMultiplier("1h")
      .setRemediationBaseEffort("5min")
      .setTags(newHashSet("tag1", "tag2"))
      .setCreatedAt(3_500_000_000_000L)
      .setUpdatedAt(4_000_000_000_000L);

    underTest.insertOrUpdate(db.getSession(), metadataToUpdate);
    db.getSession().commit();

    OrganizationDto organization = OrganizationTesting.newOrganizationDto().setUuid(organizationUuid);
    RuleDto ruleDto = underTest.selectOrFailByKey(db.getSession(), organization, RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isNull();
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRuleKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getConfigKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getSeverity()).isEqualTo(2);
    assertThat(ruleDto.getLanguage()).isEqualTo("golo");
    assertThat(ruleDto.isTemplate()).isFalse();
    assertThat(ruleDto.getTemplateId()).isNull();
    assertThat(ruleDto.getNoteData()).isEqualTo("My note");
    assertThat(ruleDto.getNoteUserLogin()).isEqualTo("admin");
    assertThat(ruleDto.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getGapDescription()).isNull();
    assertThat(ruleDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDto.getSystemTags()).isEmpty();
    assertThat(ruleDto.getType()).isEqualTo(0);
    assertThat(ruleDto.getCreatedAt()).isEqualTo(3_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(4_000_000_000_000L);
  }

  @Test
  public void update_RuleMetadataDto_updates_row_in_RULE_METADATA_if_already_exists() {
    db.prepareDbUnit(getClass(), "update.xml");
    String organizationUuid = "org-1";
    OrganizationDto organization = OrganizationTesting.newOrganizationDto().setUuid(organizationUuid);
    RuleMetadataDto metadataV1 = new RuleMetadataDto()
      .setRuleId(1)
      .setOrganizationUuid(organizationUuid)
      .setCreatedAt(3_500_000_000_000L)
      .setUpdatedAt(4_000_000_000_000L);
    RuleMetadataDto metadataV2 = new RuleMetadataDto()
      .setRuleId(1)
      .setOrganizationUuid(organizationUuid)
      .setNoteData("My note")
      .setNoteUserLogin("admin")
      .setNoteCreatedAt(DateUtils.parseDate("2013-12-19").getTime())
      .setNoteUpdatedAt(DateUtils.parseDate("2013-12-20").getTime())
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setRemediationGapMultiplier("1h")
      .setRemediationBaseEffort("5min")
      .setTags(newHashSet("tag1", "tag2"))
      .setCreatedAt(6_500_000_000_000L)
      .setUpdatedAt(7_000_000_000_000L);

    underTest.insertOrUpdate(db.getSession(), metadataV1);
    db.commit();

    assertThat(db.countRowsOfTable("RULES_METADATA")).isEqualTo(1);
    RuleDto ruleDto = underTest.selectOrFailByKey(db.getSession(), organization, RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isNull();
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRuleKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getConfigKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getSeverity()).isEqualTo(2);
    assertThat(ruleDto.getLanguage()).isEqualTo("golo");
    assertThat(ruleDto.isTemplate()).isFalse();
    assertThat(ruleDto.getTemplateId()).isNull();
    assertThat(ruleDto.getNoteData()).isNull();
    assertThat(ruleDto.getNoteUserLogin()).isNull();
    assertThat(ruleDto.getNoteCreatedAt()).isNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNull();
    assertThat(ruleDto.getRemediationFunction()).isNull();
    assertThat(ruleDto.getDefRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getDefRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getGapDescription()).isNull();
    assertThat(ruleDto.getTags()).isEmpty();
    assertThat(ruleDto.getSystemTags()).isEmpty();
    assertThat(ruleDto.getType()).isEqualTo(0);
    assertThat(ruleDto.getCreatedAt()).isEqualTo(3_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(4_000_000_000_000L);

    underTest.insertOrUpdate(db.getSession(), metadataV2);
    db.commit();

    ruleDto = underTest.selectOrFailByKey(db.getSession(), organization, RuleKey.of("checkstyle", "AvoidNull"));
    assertThat(ruleDto.getName()).isEqualTo("Avoid Null");
    assertThat(ruleDto.getDescription()).isEqualTo("Should avoid NULL");
    assertThat(ruleDto.getDescriptionFormat()).isNull();
    assertThat(ruleDto.getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(ruleDto.getRuleKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getRepositoryKey()).isEqualTo("checkstyle");
    assertThat(ruleDto.getConfigKey()).isEqualTo("AvoidNull");
    assertThat(ruleDto.getSeverity()).isEqualTo(2);
    assertThat(ruleDto.getLanguage()).isEqualTo("golo");
    assertThat(ruleDto.isTemplate()).isFalse();
    assertThat(ruleDto.getTemplateId()).isNull();
    assertThat(ruleDto.getNoteData()).isEqualTo("My note");
    assertThat(ruleDto.getNoteUserLogin()).isEqualTo("admin");
    assertThat(ruleDto.getNoteCreatedAt()).isNotNull();
    assertThat(ruleDto.getNoteUpdatedAt()).isNotNull();
    assertThat(ruleDto.getRemediationFunction()).isEqualTo("LINEAR");
    assertThat(ruleDto.getDefRemediationFunction()).isNull();
    assertThat(ruleDto.getRemediationGapMultiplier()).isEqualTo("1h");
    assertThat(ruleDto.getDefRemediationGapMultiplier()).isNull();
    assertThat(ruleDto.getRemediationBaseEffort()).isEqualTo("5min");
    assertThat(ruleDto.getDefRemediationBaseEffort()).isNull();
    assertThat(ruleDto.getGapDescription()).isNull();
    assertThat(ruleDto.getTags()).containsOnly("tag1", "tag2");
    assertThat(ruleDto.getSystemTags()).isEmpty();
    assertThat(ruleDto.getType()).isEqualTo(0);
    assertThat(ruleDto.getCreatedAt()).isEqualTo(3_500_000_000_000L);
    assertThat(ruleDto.getUpdatedAt()).isEqualTo(7_000_000_000_000L);
  }

  @Test
  public void select_parameters_by_rule_key() {
    db.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");
    List<RuleParamDto> ruleDtos = underTest.selectRuleParamsByRuleKey(db.getSession(), RuleKey.of("checkstyle", "AvoidNull"));

    assertThat(ruleDtos.size()).isEqualTo(1);
    RuleParamDto ruleDto = ruleDtos.get(0);
    assertThat(ruleDto.getId()).isEqualTo(1);
    assertThat(ruleDto.getName()).isEqualTo("myParameter");
    assertThat(ruleDto.getDescription()).isEqualTo("My Parameter");
    assertThat(ruleDto.getType()).isEqualTo("plop");
    assertThat(ruleDto.getRuleId()).isEqualTo(1);
  }

  @Test
  public void select_parameters_by_rule_keys() {
    db.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");

    assertThat(underTest.selectRuleParamsByRuleKeys(db.getSession(),
      Arrays.asList(RuleKey.of("checkstyle", "AvoidNull"), RuleKey.of("unused", "Unused")))).hasSize(2);

    assertThat(underTest.selectRuleParamsByRuleKeys(db.getSession(),
      singletonList(RuleKey.of("unknown", "Unknown")))).isEmpty();
  }

  @Test
  public void insert_parameter() {
    db.prepareDbUnit(getClass(), "insert_parameter.xml");
    RuleDefinitionDto rule1 = underTest.selectOrFailDefinitionByKey(db.getSession(), RuleKey.of("plugin", "NewRuleKey"));

    RuleParamDto param = RuleParamDto.createFor(rule1)
      .setName("max")
      .setType("INTEGER")
      .setDefaultValue("30")
      .setDescription("My Parameter");

    underTest.insertRuleParam(db.getSession(), rule1, param);
    db.getSession().commit();

    db.assertDbUnit(getClass(), "insert_parameter-result.xml", "rules_parameters");
  }

  @Test
  public void update_parameter() {
    db.prepareDbUnit(getClass(), "update_parameter.xml");

    RuleDefinitionDto rule1 = underTest.selectOrFailDefinitionByKey(db.getSession(), RuleKey.of("checkstyle", "AvoidNull"));

    List<RuleParamDto> params = underTest.selectRuleParamsByRuleKey(db.getSession(), rule1.getKey());
    assertThat(params).hasSize(1);

    RuleParamDto param = Iterables.getFirst(params, null);
    param
      .setName("format")
      .setType("STRING")
      .setDefaultValue("^[a-z]+(\\.[a-z][a-z0-9]*)*$")
      .setDescription("Regular expression used to check the package names against.");

    underTest.updateRuleParam(db.getSession(), rule1, param);
    db.getSession().commit();

    db.assertDbUnit(getClass(), "update_parameter-result.xml", "rules_parameters");
  }

  @Test
  public void delete_parameter() {
    db.prepareDbUnit(getClass(), "select_parameters_by_rule_key.xml");
    assertThat(underTest.selectRuleParamsByRuleKey(db.getSession(), RuleKey.of("checkstyle", "AvoidNull"))).hasSize(1);

    underTest.deleteRuleParam(db.getSession(), 1);
    db.getSession().commit();

    assertThat(underTest.selectRuleParamsByRuleKey(db.getSession(), RuleKey.of("checkstyle", "AvoidNull"))).isEmpty();
  }

  @Test
  public void scrollIndexingRules_on_empty_table() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();

    underTest.scrollIndexingRules(db.getSession(), accumulator);

    assertThat(accumulator.list).isEmpty();
  }

  @Test
  public void scrollIndexingRules() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDefinitionDto r1 = db.rules().insert();
    RuleDefinitionDto r2 = db.rules().insert();

    underTest.scrollIndexingRules(db.getSession(), accumulator);

    assertThat(accumulator.list)
      .extracting(RuleForIndexingDto::getId, RuleForIndexingDto::getRuleKey)
      .containsExactlyInAnyOrder(tuple(r1.getId(), r1.getKey()), tuple(r2.getId(), r2.getKey()));
    RuleForIndexingDto firstRule = accumulator.list.iterator().next();

    assertThat(firstRule.getRepository()).isEqualTo(r1.getRepositoryKey());
    assertThat(firstRule.getPluginRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(firstRule.getName()).isEqualTo(r1.getName());
    assertThat(firstRule.getDescription()).isEqualTo(r1.getDescription());
    assertThat(firstRule.getDescriptionFormat()).isEqualTo(r1.getDescriptionFormat());
    assertThat(firstRule.getSeverity()).isEqualTo(r1.getSeverity());
    assertThat(firstRule.getStatus()).isEqualTo(r1.getStatus());
    assertThat(firstRule.isTemplate()).isEqualTo(r1.isTemplate());
    assertThat(firstRule.getSystemTagsAsSet()).isEqualTo(r1.getSystemTags());
    assertThat(firstRule.getTemplateRuleKey()).isNull();
    assertThat(firstRule.getTemplateRepository()).isNull();
    assertThat(firstRule.getInternalKey()).isEqualTo(r1.getConfigKey());
    assertThat(firstRule.getLanguage()).isEqualTo(r1.getLanguage());
    assertThat(firstRule.getType()).isEqualTo(r1.getType());
    assertThat(firstRule.getCreatedAt()).isEqualTo(r1.getCreatedAt());
    assertThat(firstRule.getUpdatedAt()).isEqualTo(r1.getUpdatedAt());
  }

  @Test
  public void scrollIndexingRules_maps_rule_definition_fields_for_regular_rule_and_template_rule() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDefinitionDto r1 = db.rules().insert();
    RuleDefinitionDto r2 = db.rules().insert(rule -> rule.setTemplateId(r1.getId()));

    underTest.scrollIndexingRules(db.getSession(), accumulator);

    assertThat(accumulator.list).hasSize(2);
    RuleForIndexingDto firstRule = accumulator.list.get(0);
    RuleForIndexingDto secondRule = accumulator.list.get(1);

    assertRuleDefinitionFieldsAreEquals(r1, firstRule);
    assertThat(firstRule.getTemplateRuleKey()).isNull();
    assertThat(firstRule.getTemplateRepository()).isNull();
    assertRuleDefinitionFieldsAreEquals(r2, secondRule);
    assertThat(secondRule.getTemplateRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(secondRule.getTemplateRepository()).isEqualTo(r1.getRepositoryKey());
  }

  @Test
  public void scrollIndexingRulesByKeys() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDefinitionDto r1 = db.rules().insert();
    db.rules().insert();

    underTest.scrollIndexingRulesByKeys(db.getSession(), singletonList(r1.getId()), accumulator);

    assertThat(accumulator.list)
      .extracting(RuleForIndexingDto::getId, RuleForIndexingDto::getRuleKey)
      .containsExactlyInAnyOrder(tuple(r1.getId(), r1.getKey()));
  }

  @Test
  public void scrollIndexingRulesByKeys_maps_rule_definition_fields_for_regular_rule_and_template_rule() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    RuleDefinitionDto r1 = db.rules().insert();
    RuleDefinitionDto r2 = db.rules().insert(rule -> rule.setTemplateId(r1.getId()));

    underTest.scrollIndexingRulesByKeys(db.getSession(), Arrays.asList(r1.getId(), r2.getId()), accumulator);

    assertThat(accumulator.list).hasSize(2);
    RuleForIndexingDto firstRule = accumulator.list.stream().filter(t -> t.getId().equals(r1.getId())).findFirst().get();
    RuleForIndexingDto secondRule = accumulator.list.stream().filter(t -> t.getId().equals(r2.getId())).findFirst().get();

    assertRuleDefinitionFieldsAreEquals(r1, firstRule);
    assertThat(firstRule.getTemplateRuleKey()).isNull();
    assertThat(firstRule.getTemplateRepository()).isNull();
    assertRuleDefinitionFieldsAreEquals(r2, secondRule);
    assertThat(secondRule.getTemplateRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(secondRule.getTemplateRepository()).isEqualTo(r1.getRepositoryKey());
  }

  private void assertRuleDefinitionFieldsAreEquals(RuleDefinitionDto r1, RuleForIndexingDto firstRule) {
    assertThat(firstRule.getId()).isEqualTo(r1.getId());
    assertThat(firstRule.getRuleKey()).isEqualTo(r1.getKey());
    assertThat(firstRule.getRepository()).isEqualTo(r1.getRepositoryKey());
    assertThat(firstRule.getPluginRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(firstRule.getName()).isEqualTo(r1.getName());
    assertThat(firstRule.getDescription()).isEqualTo(r1.getDescription());
    assertThat(firstRule.getDescriptionFormat()).isEqualTo(r1.getDescriptionFormat());
    assertThat(firstRule.getSeverity()).isEqualTo(r1.getSeverity());
    assertThat(firstRule.getSeverityAsString()).isEqualTo(SeverityUtil.getSeverityFromOrdinal(r1.getSeverity()));
    assertThat(firstRule.getStatus()).isEqualTo(r1.getStatus());
    assertThat(firstRule.isTemplate()).isEqualTo(r1.isTemplate());
    assertThat(firstRule.getSystemTagsAsSet()).isEqualTo(r1.getSystemTags());
    assertThat(firstRule.getInternalKey()).isEqualTo(r1.getConfigKey());
    assertThat(firstRule.getLanguage()).isEqualTo(r1.getLanguage());
    assertThat(firstRule.getType()).isEqualTo(r1.getType());
    assertThat(firstRule.getTypeAsRuleType()).isEqualTo(RuleType.valueOf(r1.getType()));
    assertThat(firstRule.getCreatedAt()).isEqualTo(r1.getCreatedAt());
    assertThat(firstRule.getUpdatedAt()).isEqualTo(r1.getUpdatedAt());
  }

  @Test
  public void scrollIndexingRulesByKeys_scrolls_nothing_if_key_does_not_exist() {
    Accumulator<RuleForIndexingDto> accumulator = new Accumulator<>();
    db.rules().insert();
    int nonExistingRuleId = 42;

    underTest.scrollIndexingRulesByKeys(db.getSession(), singletonList(nonExistingRuleId), accumulator);

    assertThat(accumulator.list).isEmpty();
  }

  @Test
  public void scrollIndexingRuleExtensions() {
    Accumulator<RuleExtensionForIndexingDto> accumulator = new Accumulator<>();
    RuleDefinitionDto r1 = db.rules().insert();
    RuleMetadataDto r1Extension = db.rules().insertOrUpdateMetadata(r1, organization, r -> r.setTagsField("t1,t2"));
    RuleDefinitionDto r2 = db.rules().insert();
    RuleMetadataDto r2Extension = db.rules().insertOrUpdateMetadata(r2, organization, r -> r.setTagsField("t1,t3"));

    underTest.scrollIndexingRuleExtensions(db.getSession(), accumulator);

    assertThat(accumulator.list)
      .extracting(RuleExtensionForIndexingDto::getRuleId,
        RuleExtensionForIndexingDto::getRuleKey,
        RuleExtensionForIndexingDto::getOrganizationUuid, RuleExtensionForIndexingDto::getTags)
      .containsExactlyInAnyOrder(
        tuple(r1.getId(), r1.getKey(), organization.getUuid(), r1Extension.getTagsAsString()),
        tuple(r2.getId(), r2.getKey(), organization.getUuid(), r2Extension.getTagsAsString()));
  }

  @Test
  public void scrollIndexingRuleExtensionsByIds() {
    Accumulator<RuleExtensionForIndexingDto> accumulator = new Accumulator<>();
    RuleDefinitionDto r1 = db.rules().insert();
    RuleMetadataDto r1Extension = db.rules().insertOrUpdateMetadata(r1, organization, r -> r.setTagsField("t1,t2"));
    RuleExtensionId r1ExtensionId = new RuleExtensionId(organization.getUuid(), r1.getId());
    RuleDefinitionDto r2 = db.rules().insert();
    db.rules().insertOrUpdateMetadata(r2, organization, r -> r.setTagsField("t1,t3"));

    underTest.scrollIndexingRuleExtensionsByIds(db.getSession(), singletonList(r1ExtensionId), accumulator);

    assertThat(accumulator.list)
      .extracting(RuleExtensionForIndexingDto::getRuleId,
        RuleExtensionForIndexingDto::getRuleKey,
        RuleExtensionForIndexingDto::getOrganizationUuid, RuleExtensionForIndexingDto::getTags)
      .containsExactlyInAnyOrder(
        tuple(r1.getId(), r1.getKey(), organization.getUuid(), r1Extension.getTagsAsString()));
  }

  @Test
  public void selectAllDeprecatedRuleKeys() {
    RuleDefinitionDto r1 = db.rules().insert();
    RuleDefinitionDto r2 = db.rules().insert();

    db.rules().insertDeprecatedKey(r -> r.setRuleId(r1.getId()));
    db.rules().insertDeprecatedKey(r -> r.setRuleId(r2.getId()));

    db.getSession().commit();

    Set<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = underTest.selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeyDtos).hasSize(2);
  }

  @Test
  public void selectAllDeprecatedRuleKeys_return_values_even_if_there_is_no_rule() {
    db.rules().insertDeprecatedKey();
    db.rules().insertDeprecatedKey();

    Set<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = underTest.selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeyDtos).hasSize(2);
    assertThat(deprecatedRuleKeyDtos)
      .extracting(DeprecatedRuleKeyDto::getNewRepositoryKey, DeprecatedRuleKeyDto::getNewRuleKey)
      .containsExactly(
        tuple(null, null),
        tuple(null, null)
      );
  }

  @Test
  public void deleteDeprecatedRuleKeys_with_empty_list_has_no_effect() {
    db.rules().insertDeprecatedKey();
    db.rules().insertDeprecatedKey();

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);

    underTest.deleteDeprecatedRuleKeys(db.getSession(), emptyList());

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);
  }

  @Test
  public void deleteDeprecatedRuleKeys_with_non_existing_uuid_has_no_effect() {
    db.rules().insertDeprecatedKey(d -> d.setUuid("A1"));
    db.rules().insertDeprecatedKey(d -> d.setUuid("A2"));

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);

    underTest.deleteDeprecatedRuleKeys(db.getSession(), asList("B1", "B2"));

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);
  }

  @Test
  public void deleteDeprecatedRuleKeys() {
    DeprecatedRuleKeyDto deprecatedRuleKeyDto1 = db.rules().insertDeprecatedKey();
    db.rules().insertDeprecatedKey();;

    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(2);

    underTest.deleteDeprecatedRuleKeys(db.getSession(), singletonList(deprecatedRuleKeyDto1.getUuid()));
    assertThat(underTest.selectAllDeprecatedRuleKeys(db.getSession())).hasSize(1);
  }

  @Test
  public void insertDeprecatedRuleKey() {
    RuleDefinitionDto r1 = db.rules().insert();
    DeprecatedRuleKeyDto deprecatedRuleKeyDto = db.rules().insertDeprecatedKey(d -> d.setRuleId(r1.getId()));

    db.getSession().commit();

    Set<DeprecatedRuleKeyDto> deprecatedRuleKeyDtos = underTest.selectAllDeprecatedRuleKeys(db.getSession());
    assertThat(deprecatedRuleKeyDtos).hasSize(1);

    DeprecatedRuleKeyDto deprecatedRuleKeyDto1 = deprecatedRuleKeyDtos.iterator().next();
    assertThat(deprecatedRuleKeyDto1.getOldRepositoryKey()).isEqualTo(deprecatedRuleKeyDto.getOldRepositoryKey());
    assertThat(deprecatedRuleKeyDto1.getOldRuleKey()).isEqualTo(deprecatedRuleKeyDto.getOldRuleKey());
    assertThat(deprecatedRuleKeyDto1.getNewRepositoryKey()).isEqualTo(r1.getRepositoryKey());
    assertThat(deprecatedRuleKeyDto1.getNewRuleKey()).isEqualTo(r1.getRuleKey());
    assertThat(deprecatedRuleKeyDto1.getUuid()).isEqualTo(deprecatedRuleKeyDto.getUuid());
    assertThat(deprecatedRuleKeyDto1.getCreatedAt()).isEqualTo(deprecatedRuleKeyDto.getCreatedAt());
    assertThat(deprecatedRuleKeyDto1.getRuleId()).isEqualTo(r1.getId());
  }

  @Test
  public void insertDeprecatedRuleKey_with_same_RuleKey_should_fail() {
    String repositoryKey = randomAlphanumeric(50);
    String ruleKey = randomAlphanumeric(50);
    db.rules().insertDeprecatedKey(d -> d.setOldRepositoryKey(repositoryKey)
      .setOldRuleKey(ruleKey));

    thrown.expect(PersistenceException.class);

    db.rules().insertDeprecatedKey(d -> d.setOldRepositoryKey(repositoryKey)
      .setOldRuleKey(ruleKey));
  }

  private static class Accumulator<T> implements Consumer<T> {
    private final List<T> list = new ArrayList<>();

    @Override
    public void accept(T dto) {
      list.add(dto);
    }
  }
}
