/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.check.Cardinality;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.core.rule.RuleRuleTagDto;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.core.rule.RuleTagType;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.rule.RuleTagOperations;
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
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileRuleOperationsTest {

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
  RuleTagOperations ruleTagOperations;

  @Mock
  ESActiveRule esActiveRule;

  @Mock
  RuleRegistry ruleRegistry;

  @Mock
  System2 system;

  Integer currentId = 1;

  UserSession authorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas");

  QProfileRuleOperations operations;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);

    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ActiveRuleDto dto = (ActiveRuleDto) args[0];
        dto.setId(currentId++);
        return null;
      }
    }).when(activeRuleDao).insert(any(ActiveRuleDto.class), any(SqlSession.class));

    operations = new QProfileRuleOperations(myBatis, activeRuleDao, ruleDao, ruleTagDao, ruleTagOperations, esActiveRule, ruleRegistry, system);
  }

  @Test
  public void update_rule_note_when_no_existing_note() throws Exception {
    RuleDto rule = new RuleDto().setId(10).setNoteCreatedAt(null).setNoteData(null);

    List<RuleParamDto> ruleParams = newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10"));
    when(ruleDao.selectParameters(eq(10), eq(session))).thenReturn(ruleParams);
    List<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTags(eq(10), eq(session))).thenReturn(ruleTags);

    long now = System.currentTimeMillis();
    doReturn(now).when(system).now();

    operations.updateRuleNote(rule, "My note", authorizedUserSession);

    ArgumentCaptor<RuleDto> argumentCaptor = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getNoteData()).isEqualTo("My note");
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isEqualTo("nicolas");
    assertThat(argumentCaptor.getValue().getNoteCreatedAt().getTime()).isEqualTo(now);
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt().getTime()).isEqualTo(now);

    verify(session).commit();
    verify(ruleRegistry).save(eq(rule), eq(ruleParams), eq(ruleTags));
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
    when(ruleDao.selectParameters(eq(10), eq(session))).thenReturn(ruleParams);
    List<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTags(eq(10), eq(session))).thenReturn(ruleTags);

    long now = System.currentTimeMillis();
    doReturn(now).when(system).now();

    operations.updateRuleNote(rule, "My new note", MockUserSession.create().setLogin("guy").setName("Guy").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));

    ArgumentCaptor<RuleDto> argumentCaptor = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getNoteData()).isEqualTo("My new note");
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isEqualTo("nicolas");
    assertThat(argumentCaptor.getValue().getNoteCreatedAt()).isEqualTo(createdAt);
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt().getTime()).isEqualTo(now);

    verify(session).commit();
    verify(ruleRegistry).save(eq(rule), eq(ruleParams), eq(ruleTags));
  }

  @Test
  public void delete_rule_note() throws Exception {
    Date createdAt = DateUtils.parseDate("2013-12-20");
    RuleDto rule = new RuleDto().setId(10).setNoteData("My note").setNoteUserLogin("nicolas").setNoteCreatedAt(createdAt).setNoteUpdatedAt(createdAt);

    List<RuleParamDto> ruleParams = newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10"));
    when(ruleDao.selectParameters(eq(10), eq(session))).thenReturn(ruleParams);
    List<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTags(eq(10), eq(session))).thenReturn(ruleTags);

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
    verify(ruleRegistry).save(eq(rule), eq(ruleParams), eq(ruleTags));
  }

  @Test
  public void create_rule() throws Exception {
    RuleDto templateRule = new RuleDto().setId(10).setRepositoryKey("squid").setRuleKey("AvoidCycle").setConfigKey("Xpath");
    when(ruleDao.selectParameters(eq(10), eq(session))).thenReturn(newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10")));
    when(ruleDao.selectTags(eq(10), eq(session))).thenReturn(newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM)));

    Map<String, String> paramsByKey = ImmutableMap.of("max", "20");
    RuleDto result = operations.createRule(templateRule, "My New Rule", Severity.BLOCKER, "Rule Description", paramsByKey, authorizedUserSession);
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

    ArgumentCaptor<RuleParamDto> ruleParamArgument = ArgumentCaptor.forClass(RuleParamDto.class);
    verify(ruleDao).insert(ruleParamArgument.capture(), eq(session));
    assertThat(ruleParamArgument.getValue().getName()).isEqualTo("max");
    assertThat(ruleParamArgument.getValue().getDefaultValue()).isEqualTo("20");

    ArgumentCaptor<RuleRuleTagDto> ruleTagArgument = ArgumentCaptor.forClass(RuleRuleTagDto.class);
    verify(ruleDao).insert(ruleTagArgument.capture(), eq(session));
    assertThat(ruleTagArgument.getValue().getTag()).isEqualTo("style");
    assertThat(ruleTagArgument.getValue().getType()).isEqualTo(RuleTagType.SYSTEM);

    verify(session).commit();
    verify(ruleRegistry).save(eq(ruleArgument.getValue()), eq(newArrayList(ruleParamArgument.getValue())), eq(newArrayList(ruleTagArgument.getValue())));
  }

  @Test
  public void update_rule() throws Exception {
    RuleDto rule = new RuleDto().setId(11).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setConfigKey("Xpath");
    when(ruleDao.selectParameters(eq(11), eq(session))).thenReturn(newArrayList(new RuleParamDto().setId(21).setName("max").setDefaultValue("20")));
    ArrayList<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTags(eq(11), eq(session))).thenReturn(ruleTags);

    Map<String, String> paramsByKey = ImmutableMap.of("max", "21");
    operations.updateRule(rule, "Updated Rule", Severity.MAJOR, "Updated Description", paramsByKey, authorizedUserSession);

    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    assertThat(ruleArgument.getValue().getName()).isEqualTo("Updated Rule");
    assertThat(ruleArgument.getValue().getDescription()).isEqualTo("Updated Description");
    assertThat(ruleArgument.getValue().getSeverityString()).isEqualTo(Severity.MAJOR);

    ArgumentCaptor<RuleParamDto> ruleParamArgument = ArgumentCaptor.forClass(RuleParamDto.class);
    verify(ruleDao).update(ruleParamArgument.capture(), eq(session));
    assertThat(ruleParamArgument.getValue().getDefaultValue()).isEqualTo("21");

    verify(session).commit();
    verify(ruleRegistry).save(eq(ruleArgument.getValue()), eq(newArrayList(ruleParamArgument.getValue())), eq(ruleTags));
  }

  @Test
  public void delete_rule() throws Exception {
    final int ruleId = 11;
    RuleDto rule = new RuleDto().setId(ruleId).setRepositoryKey("squid").setRuleKey("XPath_1387869254").setConfigKey("Xpath").setUpdatedAt(DateUtils.parseDate("2013-12-23"));
    RuleParamDto param = new RuleParamDto().setId(21).setName("max").setDefaultValue("20");
    when(ruleDao.selectParameters(eq(ruleId), eq(session))).thenReturn(newArrayList(param));
    ArrayList<RuleRuleTagDto> ruleTags = newArrayList(new RuleRuleTagDto().setId(30L).setTag("style").setType(RuleTagType.SYSTEM));
    when(ruleDao.selectTags(eq(ruleId), eq(session))).thenReturn(ruleTags);

    final int activeRuleId = 5;
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(activeRuleId).setProfileId(1).setRuleId(ruleId).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectByRuleId(ruleId)).thenReturn(newArrayList(activeRule));

    long now = System.currentTimeMillis();
    doReturn(now).when(system).now();

    operations.deleteRule(rule, authorizedUserSession);

    ArgumentCaptor<RuleDto> ruleArgument = ArgumentCaptor.forClass(RuleDto.class);
    verify(ruleDao).update(ruleArgument.capture(), eq(session));
    assertThat(ruleArgument.getValue().getStatus()).isEqualTo(Rule.STATUS_REMOVED);
    assertThat(ruleArgument.getValue().getUpdatedAt()).isEqualTo(new Date(now));

    verify(ruleRegistry).save(eq(ruleArgument.getValue()), eq(newArrayList(param)), eq(ruleTags));
    verify(activeRuleDao).deleteParameters(eq(activeRuleId), eq(session));
    verify(activeRuleDao).deleteFromRule(eq(ruleId), eq(session));
    verify(session, times(2)).commit();
    verify(esActiveRule).deleteActiveRules(newArrayList(activeRuleId));
  }

  @Test(expected = ForbiddenException.class)
  public void should_fail_update_tags_on_unauthorized_user() {
    operations.updateTags(new RuleDto(), ImmutableList.of("polop"), unauthorizedUserSession);
  }

  @Test(expected = NotFoundException.class)
  public void should_fail_update_tags_on_unknown_tag() {
    final String tag = "polop";
    when(ruleTagDao.selectId(tag, session)).thenReturn(null);
    operations.updateTags(new RuleDto(), ImmutableList.of(tag), authorizedUserSession);
  }

  @Test
  public void should_add_new_tags() {
    final int ruleId = 24;
    final RuleDto rule = new RuleDto().setId(ruleId);
    final String tag = "polop";
    final long tagId = 42L;
    when(ruleTagDao.selectId(tag, session)).thenReturn(tagId);

    operations.updateTags(rule, ImmutableList.of(tag), authorizedUserSession);

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
  public void should_delete_removed_tags() {
    final int ruleId = 24;
    final RuleDto rule = new RuleDto().setId(ruleId);
    final String tag = "polop";
    RuleRuleTagDto existingTag = new RuleRuleTagDto().setTag(tag).setType(RuleTagType.ADMIN);
    when(ruleDao.selectTags(ruleId, session)).thenReturn(ImmutableList.of(existingTag));


    operations.updateTags(rule, ImmutableList.<String>of(), authorizedUserSession);

    verify(ruleDao, atLeast(1)).selectTags(ruleId, session);
    verify(ruleDao).deleteTag(existingTag, session);
    verify(ruleDao).update(rule, session);
    verify(ruleTagOperations).deleteUnusedTags(session);
    verify(session).commit();
  }

  @Test
  public void should_not_update_rule_if_tags_unchanged() {
    final int ruleId = 24;
    final RuleDto rule = new RuleDto().setId(ruleId);
    final String tag = "polop";
    final long tagId = 42L;
    when(ruleTagDao.selectId(tag, session)).thenReturn(tagId);
    RuleRuleTagDto existingTag = new RuleRuleTagDto().setTag(tag).setType(RuleTagType.ADMIN);
    when(ruleDao.selectTags(ruleId, session)).thenReturn(ImmutableList.of(existingTag));

    operations.updateTags(rule, ImmutableList.of(tag), authorizedUserSession);

    verify(ruleTagDao).selectId(tag, session);
    verify(ruleDao).selectTags(ruleId, session);
    verify(ruleTagOperations).deleteUnusedTags(session);
    verify(ruleDao, never()).update(rule);
  }
}
