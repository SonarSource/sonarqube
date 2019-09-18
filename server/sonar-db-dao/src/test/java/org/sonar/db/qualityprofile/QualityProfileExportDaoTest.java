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
package org.sonar.db.qualityprofile;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleMetadataDto;
import org.sonar.db.rule.RuleParamDto;

import static org.assertj.core.api.Assertions.assertThat;

public class QualityProfileExportDaoTest {

  @Rule
  public DbTester db = DbTester.create(new AlwaysIncreasingSystem2());

  private DbSession dbSession = db.getSession();
  private QualityProfileExportDao underTest = db.getDbClient().qualityProfileExportDao();

  @Test
  public void selectRulesByProfile_ready_rules_only() {
    String language = "java";
    RuleDefinitionDto rule1 = createRule(language);
    RuleDefinitionDto rule2 = createRule(language);
    RuleDefinitionDto rule3 = createRule(language);
    createRule(language, RuleStatus.REMOVED);

    QProfileDto profile = createProfile(language);

    activate(profile, rule1, rule2, rule3);

    List<ExportRuleDto> results = underTest.selectRulesByProfile(dbSession, profile);
    assertThat(results).isNotNull();
    assertThat(results).asList()
      .extracting("ruleKey")
      .containsOnly(rule1.getKey(), rule2.getKey(), rule3.getKey());
  }

  @Test
  public void selectRulesByProfile_verify_columns() {
    String language = "java";
    RuleDefinitionDto ruleTemplate = createRule(language);
    RuleDefinitionDto rule = createRule(language, RuleStatus.READY, ruleTemplate.getId());
    RuleMetadataDto ruleMetadata = createRuleMetadata(new RuleMetadataDto()
      .setRuleId(rule.getId())
      .setOrganizationUuid(db.getDefaultOrganization().getUuid())
      .setNoteData("Extended description")
      .setTags(Sets.newHashSet("tag1", "tag2", "tag3")));

    QProfileDto profile = createProfile(language);

    List<ActiveRuleDto> activeRules = activate(profile, rule);

    List<ExportRuleDto> results = underTest.selectRulesByProfile(dbSession, profile);
    assertThat(results).isNotNull();
    assertThat(results).asList().isNotEmpty();

    ExportRuleDto exportRuleDto = results.get(0);
    assertThat(exportRuleDto).isNotNull();
    assertThat(exportRuleDto.getParams()).asList().isEmpty();
    assertThat(exportRuleDto.getDescription()).isEqualTo(rule.getDescription());
    assertThat(exportRuleDto.getExtendedDescription()).isEqualTo(ruleMetadata.getNoteData());
    assertThat(exportRuleDto.getName()).isEqualTo(rule.getName());
    assertThat(exportRuleDto.getRuleKey()).isEqualTo(rule.getKey());
    assertThat(exportRuleDto.getRuleType()).isEqualTo(RuleType.valueOf(rule.getType()));
    assertThat(exportRuleDto.getSeverityString()).isEqualTo(activeRules.get(0).getSeverityString());
    assertThat(exportRuleDto.getTags()).isEqualTo(String.join(",", ruleMetadata.getTags()));
    assertThat(exportRuleDto.getTemplateRuleKey()).isEqualTo(ruleTemplate.getKey());
  }

  @Test
  public void selectRulesByProfile_verify_rows_over_1000() {
    String language = "java";
    int numberOfParamsToCreate = 1005;
    RuleDefinitionDto rule = createRule(language);
    List<RuleParamDto> ruleParams = addParamsToRule(rule, numberOfParamsToCreate);

    QProfileDto profile = createProfile(language);
    ActiveRuleDto activatedRule = activate(profile, rule, ruleParams);

    List<ExportRuleDto> results = underTest.selectRulesByProfile(dbSession, profile);

    assertThat(results)
      .extracting("activeRuleId")
      .containsOnly(activatedRule.getId());

    assertThat(results.get(0).getParams())
      .extracting("key")
      .containsOnly(ruleParams.stream().map(RuleParamDto::getName).toArray());
  }

  @Test
  public void selectRulesByProfile_params_assigned_correctly() {
    String language = "java";
    RuleDefinitionDto firstRule = createRule(language);
    List<RuleParamDto> ruleParamsOfFirstRule = addParamsToRule(firstRule, 2);

    RuleDefinitionDto secondRule = createRule(language);
    List<RuleParamDto> ruleParamsOfSecondRule = addParamsToRule(secondRule, 3);

    String otherLanguage = "js";
    RuleDefinitionDto thirdRule = createRule(otherLanguage);
    List<RuleParamDto> ruleParamsOfThirdRule = addParamsToRule(thirdRule, 4);

    QProfileDto profile = createProfile(language);
    QProfileDto otherProfile = createProfile(otherLanguage);

    ActiveRuleDto firstActivatedRule = activate(profile, firstRule, ruleParamsOfFirstRule);
    ActiveRuleDto secondActivatedRule = activate(profile, secondRule, ruleParamsOfSecondRule);

    ActiveRuleDto thirdActivatedRule = activate(otherProfile, thirdRule, ruleParamsOfThirdRule);

    List<ExportRuleDto> results = underTest.selectRulesByProfile(dbSession, profile);
    List<ExportRuleDto> otherProfileResults = underTest.selectRulesByProfile(dbSession, otherProfile);

    assertThat(results)
      .extracting("activeRuleId")
      .containsOnly(firstActivatedRule.getId(), secondActivatedRule.getId());

    assertThat(otherProfileResults).asList()
      .extracting("activeRuleId")
      .containsOnly(thirdActivatedRule.getId());

    ExportRuleDto firstExportedRule = findExportedRuleById(firstActivatedRule.getId(), results);
    assertThat(firstExportedRule.getParams())
      .extracting("key")
      .containsOnly(ruleParamsOfFirstRule.stream().map(RuleParamDto::getName).toArray());

    ExportRuleDto secondExportedRule = findExportedRuleById(secondActivatedRule.getId(), results);
    assertThat(secondExportedRule.getParams())
      .extracting("key")
      .containsOnly(ruleParamsOfSecondRule.stream().map(RuleParamDto::getName).toArray());

    ExportRuleDto thirdExportedRule = findExportedRuleById(thirdActivatedRule.getId(), otherProfileResults);
    assertThat(thirdExportedRule.getParams())
      .extracting("key")
      .containsOnly(ruleParamsOfThirdRule.stream().map(RuleParamDto::getName).toArray());
  }


  private ExportRuleDto findExportedRuleById(Integer id, List<ExportRuleDto> results) {
    Optional<ExportRuleDto> found = results.stream().filter(exportRuleDto -> id.equals(exportRuleDto.getActiveRuleId())).findFirst();
    if (!found.isPresent()) {
      Assert.fail();
    }
    return found.get();
  }

  private List<RuleParamDto> addParamsToRule(RuleDefinitionDto firstRule, int numberOfParams) {
    return IntStream.range(0, numberOfParams)
      .mapToObj(value -> db.rules().insertRuleParam(firstRule,
        ruleParamDto -> ruleParamDto.setName("name_" + firstRule.getId() + "_" + value)))
      .collect(Collectors.toList());
  }

  private RuleDefinitionDto createRule(String language) {
    return createRule(language, RuleStatus.READY);
  }

  private RuleDefinitionDto createRule(String language, RuleStatus status) {
    return createRule(language, status, null);
  }

  private RuleDefinitionDto createRule(String language, RuleStatus status, @Nullable Integer templateId) {
    return db.rules().insert(ruleDefinitionDto -> ruleDefinitionDto.setRepositoryKey("repoKey").setLanguage(language).setStatus(status).setTemplateId(templateId));
  }

  private RuleMetadataDto createRuleMetadata(RuleMetadataDto metadataDto) {
    return db.rules().insertOrUpdateMetadata(metadataDto);
  }

  private QProfileDto createProfile(String lanugage) {
    return db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(lanugage));
  }

  private List<ActiveRuleDto> activate(QProfileDto profile, RuleDefinitionDto... rules) {
    return Stream.of(rules)
      .map(ruleDefinitionDto -> db.qualityProfiles().activateRule(profile, ruleDefinitionDto))
      .collect(Collectors.toList());
  }

  private ActiveRuleDto activate(QProfileDto profile, RuleDefinitionDto rule, Collection<RuleParamDto> params) {
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(profile, rule);

    params.forEach(ruleParamDto -> {
      ActiveRuleParamDto dto = ActiveRuleParamDto.createFor(ruleParamDto)
        .setKey(ruleParamDto.getName())
        .setValue("20")
        .setActiveRuleId(activeRule.getId());
      db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule, dto);
    });

    return activeRule;
  }
}
