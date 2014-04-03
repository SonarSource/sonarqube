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

package org.sonar.server.rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.rule.*;
import org.sonar.core.technicaldebt.db.CharacteristicDao;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.ESActiveRule;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.sonar.server.rule.RuleOperations.RuleChange;

@RunWith(MockitoJUnitRunner.class)
public class RuleOperationsTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  ActiveRuleDao activeRuleDao;

  @Mock
  RuleDao ruleDao;

  @Mock
  RuleTagDao ruleTagDao;

  @Mock
  CharacteristicDao characteristicDao;

  @Mock
  RuleTagOperations ruleTagOperations;

  @Mock
  ESActiveRule esActiveRule;

  @Mock
  RuleRegistry ruleRegistry;

  @Mock
  System2 system;

  Date now = DateUtils.parseDate("2014-03-19");

  @Captor
  ArgumentCaptor<RuleDto> ruleCaptor;

  Integer currentId = 1;

  UserSession authorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas");

  RuleOperations operations;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    when(system.now()).thenReturn(now.getTime());

    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ActiveRuleDto dto = (ActiveRuleDto) args[0];
        dto.setId(currentId++);
        return null;
      }
    }).when(activeRuleDao).insert(any(ActiveRuleDto.class), any(SqlSession.class));

    operations = new RuleOperations(myBatis, activeRuleDao, ruleDao, ruleTagDao, characteristicDao, ruleTagOperations, esActiveRule, ruleRegistry, system);
  }

  @Test
  public void update_rule_note_when_no_existing_note() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setNoteCreatedAt(null).setNoteData(null);

    List<RuleParamDto> ruleParams = newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10"));
    when(ruleDao.selectParametersByRuleId(eq(10), eq(session))).thenReturn(ruleParams);
    List<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTagsByRuleIds(eq(10), eq(session))).thenReturn(ruleTags);

    operations.updateRuleNote(rule, "My note", authorizedUserSession);

    ArgumentCaptor<RuleDto> argumentCaptor = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getNoteData()).isEqualTo("My note");
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isEqualTo("nicolas");
    assertThat(argumentCaptor.getValue().getNoteCreatedAt()).isEqualTo(now);
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt()).isEqualTo(now);

    verify(session).commit();
    verify(ruleRegistry).reindex(eq(rule), eq(session));
  }

  @Test
  public void fail_to_update_rule_note_without_profile_admin_permission() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle");

    try {
      operations.updateRuleNote(rule, "My note", unauthorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyNoMoreInteractions(ruleDao);
    verify(session, never()).commit();
  }

  @Test
  public void update_rule_note_when_already_note() throws Exception {
    Date createdAt = DateUtils.parseDate("2013-12-20");
    RuleDto rule = new RuleDto().setId(10).setNoteCreatedAt(createdAt).setNoteData("My previous note").setNoteUserLogin("nicolas");

    List<RuleParamDto> ruleParams = newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10"));
    when(ruleDao.selectParametersByRuleId(eq(10), eq(session))).thenReturn(ruleParams);
    List<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTagsByRuleIds(eq(10), eq(session))).thenReturn(ruleTags);

    operations.updateRuleNote(rule, "My new note", MockUserSession.create().setLogin("guy").setName("Guy").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));

    ArgumentCaptor<RuleDto> argumentCaptor = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getNoteData()).isEqualTo("My new note");
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isEqualTo("nicolas");
    assertThat(argumentCaptor.getValue().getNoteCreatedAt()).isEqualTo(createdAt);
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt()).isEqualTo(now);

    verify(session).commit();
    verify(ruleRegistry).reindex(eq(rule), eq(session));
  }

  @Test
  public void delete_rule_note() throws Exception {
    Date createdAt = DateUtils.parseDate("2013-12-20");
    RuleDto rule = new RuleDto().setId(10).setNoteData("My note").setNoteUserLogin("nicolas").setNoteCreatedAt(createdAt).setNoteUpdatedAt(createdAt);

    List<RuleParamDto> ruleParams = newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10"));
    when(ruleDao.selectParametersByRuleId(eq(10), eq(session))).thenReturn(ruleParams);
    List<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTagsByRuleIds(eq(10), eq(session))).thenReturn(ruleTags);

    long now = System.currentTimeMillis();
    doReturn(now).when(system).now();

    operations.deleteRuleNote(rule, authorizedUserSession);

    ArgumentCaptor<RuleDto> argumentCaptor = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getNoteData()).isNull();
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isNull();
    assertThat(argumentCaptor.getValue().getNoteCreatedAt()).isNull();
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt()).isNull();

    verify(session).commit();
    verify(ruleRegistry).reindex(eq(rule), eq(session));
  }

  @Test
  public void create_custom_rule() throws Exception {
    RuleDto templateRule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle").setConfigKey("Xpath")
      .setDefaultSubCharacteristicId(20).setDefaultRemediationFunction("LINEAR_OFFSET").setDefaultRemediationCoefficient("2h").setDefaultRemediationOffset("15min");
    when(ruleDao.selectParametersByRuleId(eq(10), eq(session))).thenReturn(newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10")));
    when(ruleDao.selectTagsByRuleIds(eq(10), eq(session))).thenReturn(newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM)));

    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    RuleDto result = operations.createCustomRule(templateRule, "My New Rule", Severity.BLOCKER, "Rule Description", paramsByKey, authorizedUserSession);
    assertThat(result).isNotNull();

    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).insert(ruleArgument.capture(), eq(session));
    assertThat(ruleArgument.getValue().getParentId()).isEqualTo(10);
    assertThat(ruleArgument.getValue().getName()).isEqualTo("My New Rule");
    assertThat(ruleArgument.getValue().getDescription()).isEqualTo("Rule Description");
    assertThat(ruleArgument.getValue().getSeverityString()).isEqualTo(Severity.BLOCKER);
    assertThat(ruleArgument.getValue().getConfigKey()).isEqualTo("Xpath");
    assertThat(ruleArgument.getValue().getRepositoryKey()).isEqualTo("squid");
    assertThat(ruleArgument.getValue().getRuleKey()).startsWith("AvoidCycle");
    assertThat(ruleArgument.getValue().getStatus()).isEqualTo("READY");
    assertThat(ruleArgument.getValue().getCardinality()).isEqualTo(Cardinality.SINGLE);
    assertThat(ruleArgument.getValue().getDefaultSubCharacteristicId()).isEqualTo(20);
    assertThat(ruleArgument.getValue().getDefaultRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(ruleArgument.getValue().getDefaultRemediationCoefficient()).isEqualTo("2h");
    assertThat(ruleArgument.getValue().getDefaultRemediationOffset()).isEqualTo("15min");

    ArgumentCaptor<RuleParamDto> ruleParamArgument = ArgumentCaptor.forClass(RuleParamDto.class);
    verify(ruleDao).insert(ruleParamArgument.capture(), eq(session));
    assertThat(ruleParamArgument.getValue().getName()).isEqualTo("max");
    assertThat(ruleParamArgument.getValue().getDefaultValue()).isEqualTo("20");

    ArgumentCaptor<RuleRuleTagDto> ruleTagArgument = ArgumentCaptor.forClass(RuleRuleTagDto.class);
    verify(ruleDao).insert(ruleTagArgument.capture(), eq(session));
    assertThat(ruleTagArgument.getValue().getTag()).isEqualTo("style");
    assertThat(ruleTagArgument.getValue().getType()).isEqualTo(RuleTagType.SYSTEM);

    verify(session).commit();
    verify(ruleRegistry).reindex(eq(ruleArgument.getValue()), eq(session));
  }

  @Test
  public void update_custom_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setConfigKey("Xpath");
    when(ruleDao.selectParametersByRuleId(eq(11), eq(session))).thenReturn(newArrayList(new RuleParamDto().setId(21).setName("max").setDefaultValue("20")));
    ArrayList<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTagsByRuleIds(eq(11), eq(session))).thenReturn(ruleTags);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");
    operations.updateCustomRule(rule, "Updated Rule", Severity.MAJOR, "Updated Description", paramsByKey, authorizedUserSession);

    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    assertThat(ruleArgument.getValue().getName()).isEqualTo("Updated Rule");
    assertThat(ruleArgument.getValue().getDescription()).isEqualTo("Updated Description");
    assertThat(ruleArgument.getValue().getSeverityString()).isEqualTo(Severity.MAJOR);

    ArgumentCaptor<RuleParamDto> ruleParamArgument = ArgumentCaptor.forClass(RuleParamDto.class);
    verify(ruleDao).update(ruleParamArgument.capture(), eq(session));
    assertThat(ruleParamArgument.getValue().getDefaultValue()).isEqualTo("21");

    verify(session).commit();
    verify(ruleRegistry).reindex(eq(ruleArgument.getValue()), eq(session));
  }

  @Test
  public void delete_custom_rule() throws Exception {
    final int ruleId = 11;
    RuleDto rule = new RuleDto().setId(ruleId).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setConfigKey("Xpath").setUpdatedAt(DateUtils.parseDate("2013-12-23"));
    RuleParamDto param = new RuleParamDto().setId(21).setName("max").setDefaultValue("20");
    when(ruleDao.selectParametersByRuleId(eq(ruleId), eq(session))).thenReturn(newArrayList(param));
    ArrayList<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTagsByRuleIds(eq(ruleId), eq(session))).thenReturn(ruleTags);

    final int activeRuleId = 5;
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(activeRuleId).setProfileId(1).setRuleId(ruleId).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectByRuleId(ruleId)).thenReturn(newArrayList(activeRule));

    operations.deleteCustomRule(rule, authorizedUserSession);

    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    assertThat(ruleCaptor.getValue().getStatus()).isEqualTo(Rule.STATUS_REMOVED);
    assertThat(ruleCaptor.getValue().getUpdatedAt()).isEqualTo(now);

    verify(ruleRegistry).reindex(eq(ruleCaptor.getValue()), eq(session));
    verify(activeRuleDao).deleteParameters(eq(activeRuleId), eq(session));
    verify(activeRuleDao).deleteFromRule(eq(ruleId), eq(session));
    verify(session, times(2)).commit();
    verify(esActiveRule).deleteActiveRules(newArrayList(activeRuleId));
  }

  @Test(expected = ForbiddenException.class)
  public void fail_to_update_tags_on_unauthorized_user() {
    operations.updateRuleTags(new RuleDto(), ImmutableList.of("polop"), unauthorizedUserSession);
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_update_tags_on_unknown_tag() {
    final String tag = "polop";
    when(ruleTagDao.selectId(tag, session)).thenReturn(null);
    operations.updateRuleTags(new RuleDto(), ImmutableList.of(tag), authorizedUserSession);
  }

  @Test
  public void add_new_tags() {
    final int ruleId = 24;
    final RuleDto rule = new RuleDto().setId(ruleId);
    final String tag = "polop";
    final long tagId = 42L;
    when(ruleTagDao.selectId(tag, session)).thenReturn(tagId);

    operations.updateRuleTags(rule, ImmutableList.of(tag), authorizedUserSession);

    verify(ruleTagDao).selectId(tag, session);
    ArgumentCaptor<RuleRuleTagDto> capture = ArgumentCaptor.forClass(RuleRuleTagDto.class);
    verify(ruleDao).insert(capture.capture(), eq(session));
    final RuleRuleTagDto newTag = capture.getValue();
    assertThat(newTag.getRuleId()).isEqualTo(ruleId);
    assertThat(newTag.getTagId()).isEqualTo(tagId);
    assertThat(newTag.getType()).isEqualTo(RuleTagType.ADMIN);
    verify(ruleDao).update(rule, session);
    verify(session).commit();
  }

  @Test
  public void delete_removed_tags() {
    final int ruleId = 24;
    final RuleDto rule = new RuleDto().setId(ruleId);
    final String tag = "polop";
    RuleRuleTagDto existingTag = new RuleRuleTagDto().setTag(tag).setType(RuleTagType.ADMIN);
    when(ruleDao.selectTagsByRuleIds(ruleId, session)).thenReturn(ImmutableList.of(existingTag));

    operations.updateRuleTags(rule, ImmutableList.<String>of(), authorizedUserSession);

    verify(ruleDao, atLeast(1)).selectTagsByRuleIds(ruleId, session);
    verify(ruleDao).deleteTag(existingTag, session);
    verify(ruleDao).update(rule, session);
    verify(ruleTagOperations).deleteUnusedTags(session);
    verify(session).commit();
  }

  @Test
  public void not_update_rule_tags_if_tags_unchanged() {
    final int ruleId = 24;
    final RuleDto rule = new RuleDto().setId(ruleId);
    final String tag = "polop";
    final long tagId = 42L;
    when(ruleTagDao.selectId(tag, session)).thenReturn(tagId);
    RuleRuleTagDto existingTag = new RuleRuleTagDto().setTag(tag).setType(RuleTagType.ADMIN);
    when(ruleDao.selectTagsByRuleIds(ruleId, session)).thenReturn(ImmutableList.of(existingTag));

    operations.updateRuleTags(rule, ImmutableList.of(tag), authorizedUserSession);

    verify(ruleTagDao).selectId(tag, session);
    verify(ruleDao).selectTagsByRuleIds(ruleId, session);
    verify(ruleTagOperations).deleteUnusedTags(session);
    verify(ruleDao, never()).update(rule);
  }

  @Test
  public void update_rule() throws Exception {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(6).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.selectByKey(ruleKey, session)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);

    // Call when reindexing rule in E/S
    when(characteristicDao.selectById(2, session)).thenReturn(subCharacteristic);
    CharacteristicDto characteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(2);
    when(characteristicDao.selectById(1, session)).thenReturn(characteristic);

    operations.updateRule(
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
        .setDebtRemediationFunction("LINEAR_OFFSET").setDebtRemediationCoefficient("2h").setDebtRemediationOffset("20min"),
      authorizedUserSession
    );

    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isEqualTo(2);
    assertThat(result.getRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(result.getRemediationCoefficient()).isEqualTo("2h");
    assertThat(result.getRemediationOffset()).isEqualTo("20min");
    assertThat(result.getUpdatedAt()).isEqualTo(now);

    verify(session).commit();
    verify(ruleRegistry).reindex(eq(result), eq(session));
  }

  @Test
  public void not_update_rule_if_same_sub_characteristic_and_function() throws Exception {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(2).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.selectByKey(ruleKey, session)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);

    operations.updateRule(
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
        .setDebtRemediationFunction("CONSTANT_ISSUE").setDebtRemediationOffset("10min"),
      authorizedUserSession
    );

    verify(ruleDao, never()).update(any(RuleDto.class), eq(session));
    verify(session, never()).commit();
    verify(ruleRegistry, never()).reindex(any(RuleDto.class), eq(session));
  }

  @Test
  public void disable_characteristic_and_remove_remediation_function_when_update_rule_with_no_sub_characteristic() throws Exception {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(6).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.selectByKey(ruleKey, session)).thenReturn(dto);

    operations.updateRule(
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey(null),
      authorizedUserSession
    );

    verify(ruleDao).update(ruleCaptor.capture(), eq(session));
    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(result.getRemediationFunction()).isNull();
    assertThat(result.getRemediationCoefficient()).isNull();
    assertThat(result.getRemediationOffset()).isNull();
    assertThat(result.getUpdatedAt()).isEqualTo(now);

    verify(session).commit();
    verify(ruleRegistry).reindex(eq(result), eq(session));
  }

  @Test
  public void not_disable_characteristic_when_update_rule_if_already_disabled() throws Exception {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setSubCharacteristicId(-1);
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.selectByKey(ruleKey, session)).thenReturn(dto);

    operations.updateRule(
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey(null),
      authorizedUserSession
    );

    verify(ruleDao, never()).update(any(RuleDto.class), eq(session));
    verify(session, never()).commit();
    verify(ruleRegistry, never()).reindex(any(RuleDto.class), eq(session));
  }

  @Test
  public void fail_to_update_rule_on_unknown_rule() throws Exception {
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.selectByKey(ruleKey, session)).thenReturn(null);

    try {
      operations.updateRule(
        new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
          .setDebtRemediationFunction("LINEAR_OFFSET").setDebtRemediationCoefficient("2h").setDebtRemediationOffset("20min"),
        authorizedUserSession
      );
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }

    verify(ruleDao, never()).update(any(RuleDto.class), eq(session));
    verify(session, never()).commit();
    verify(ruleRegistry, never()).reindex(any(RuleDto.class), eq(session));
  }

  @Test
  public void fail_to_update_rule_on_unknown_sub_characteristic() throws Exception {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(2).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.selectByKey(ruleKey, session)).thenReturn(dto);

    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(null);

    try {
      operations.updateRule(
        new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
          .setDebtRemediationFunction("LINEAR_OFFSET").setDebtRemediationCoefficient("2h").setDebtRemediationOffset("20min"),
        authorizedUserSession
      );
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }

    verify(ruleDao, never()).update(any(RuleDto.class), eq(session));
    verify(session, never()).commit();
    verify(ruleRegistry, never()).reindex(any(RuleDto.class), eq(session));
  }

  @Test
  public void fail_to_update_rule_on_invalid_coefficient() throws Exception {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(2).setRemediationFunction("LINEAR").setRemediationCoefficient("1h");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.selectByKey(ruleKey, session)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);

    try {
      operations.updateRule(
        new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
          .setDebtRemediationFunction("LINEAR").setDebtRemediationCoefficient("foo"),
        authorizedUserSession
      );
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Invalid coefficient: foo");
    }

    verify(ruleDao, never()).update(any(RuleDto.class), eq(session));
    verify(session, never()).commit();
    verify(ruleRegistry, never()).reindex(any(RuleDto.class), eq(session));
  }
}
