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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.PropertyType;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.TypeValidations;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileActiveRuleOperationsTest {

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  ActiveRuleDao activeRuleDao;

  @Mock
  RuleDao ruleDao;

  @Mock
  QualityProfileDao profileDao;

  @Mock
  ESActiveRule esActiveRule;

  @Mock
  ProfilesManager profilesManager;

  @Mock
  TypeValidations typeValidations;

  @Mock
  System2 system;

  Integer currentId = 1;

  UserSession authorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas");

  QProfileActiveRuleOperations operations;

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

    operations = new QProfileActiveRuleOperations(myBatis, activeRuleDao, ruleDao, profileDao, esActiveRule, profilesManager, typeValidations, system);
  }

  @Test
  public void fail_to_activate_rule_without_profile_admin_permission() throws Exception {
    when(profileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    try {
      operations.activateRule(1, 10, Severity.CRITICAL, unauthorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyNoMoreInteractions(activeRuleDao);
    verify(session, never()).commit();
  }

  @Test
  public void activate_rule() throws Exception {
    when(profileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(ruleDao.selectById(10, session)).thenReturn(new RuleDto().setId(10));

    when(ruleDao.selectParameters(eq(10), eq(session))).thenReturn(newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10")));
    final int idActiveRuleToUpdate = 42;
    final int idActiveRuleToDelete = 24;
    ProfilesManager.RuleInheritanceActions inheritanceActions = new ProfilesManager.RuleInheritanceActions()
      .addToIndex(idActiveRuleToUpdate)
      .addToDelete(idActiveRuleToDelete);
    when(profilesManager.activated(eq(1), anyInt(), eq("Nicolas"))).thenReturn(inheritanceActions);

    operations.activateRule(1, 10, Severity.CRITICAL, authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> activeRuleArgument = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).insert(activeRuleArgument.capture(), eq(session));
    assertThat(activeRuleArgument.getValue().getRulId()).isEqualTo(10);
    assertThat(activeRuleArgument.getValue().getSeverityString()).isEqualTo(Severity.CRITICAL);

    ArgumentCaptor<ActiveRuleParamDto> activeRuleParamArgument = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).insert(activeRuleParamArgument.capture(), eq(session));
    assertThat(activeRuleParamArgument.getValue().getKey()).isEqualTo("max");
    assertThat(activeRuleParamArgument.getValue().getValue()).isEqualTo("10");

    verify(session).commit();
    verify(profilesManager).activated(eq(1), anyInt(), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(eq(newArrayList(idActiveRuleToDelete)));
    verify(esActiveRule).bulkIndexActiveRuleIds(eq(newArrayList(idActiveRuleToUpdate)), eq(session));
  }

  @Test
  public void update_severity() throws Exception {
    when(profileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(ruleDao.selectById(10, session)).thenReturn(new RuleDto().setId(10));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectByProfileAndRule(1, 10, session)).thenReturn(activeRule);

    when(profilesManager.ruleSeverityChanged(eq(1), eq(5), eq(RulePriority.MINOR), eq(RulePriority.MAJOR), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.activateRule(1, 10, Severity.MAJOR, authorizedUserSession);

    verify(activeRuleDao).update(eq(activeRule), eq(session));
    verify(session).commit();
    verify(profilesManager).ruleSeverityChanged(eq(1), eq(5), eq(RulePriority.MINOR), eq(RulePriority.MAJOR), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void fail_to_update_severity_on_invalid_severity() throws Exception {
    when(profileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(ruleDao.selectById(10)).thenReturn(new RuleDto().setId(10));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectByProfileAndRule(1, 10)).thenReturn(activeRule);

    try {
      operations.activateRule(1, 10, "Unknown", authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
    verify(activeRuleDao, never()).update(eq(activeRule), eq(session));
    verifyZeroInteractions(profilesManager);
  }

  @Test
  public void activate_rules() throws Exception {
    when(profileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(ruleDao.selectById(10, session)).thenReturn(new RuleDto().setId(10).setSeverity(Severity.CRITICAL));

    when(ruleDao.selectParameters(eq(10), eq(session))).thenReturn(newArrayList(new RuleParamDto().setId(20).setName("max").setDefaultValue("10")));
    final int idActiveRuleToUpdate = 42;
    final int idActiveRuleToDelete = 24;
    ProfilesManager.RuleInheritanceActions inheritanceActions = new ProfilesManager.RuleInheritanceActions()
      .addToIndex(idActiveRuleToUpdate)
      .addToDelete(idActiveRuleToDelete);
    when(profilesManager.activated(eq(1), anyInt(), eq("Nicolas"))).thenReturn(inheritanceActions);

    operations.activateRules(1, newArrayList(10), authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> activeRuleArgument = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).insert(activeRuleArgument.capture(), eq(session));
    assertThat(activeRuleArgument.getValue().getRulId()).isEqualTo(10);
    assertThat(activeRuleArgument.getValue().getSeverityString()).isEqualTo(Severity.CRITICAL);

    ArgumentCaptor<ActiveRuleParamDto> activeRuleParamArgument = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).insert(activeRuleParamArgument.capture(), eq(session));
    assertThat(activeRuleParamArgument.getValue().getKey()).isEqualTo("max");
    assertThat(activeRuleParamArgument.getValue().getValue()).isEqualTo("10");

    verify(session).commit();
    verify(profilesManager).activated(eq(1), anyInt(), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(eq(newArrayList(idActiveRuleToDelete)));
    verify(esActiveRule).bulkIndexActiveRuleIds(eq(newArrayList(idActiveRuleToUpdate)), eq(session));
  }

  @Test
  public void deactivate_rule() throws Exception {
    when(profileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(ruleDao.selectById(10, session)).thenReturn(new RuleDto().setId(10));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectByProfileAndRule(1, 10, session)).thenReturn(activeRule);
    when(profilesManager.deactivated(eq(1), anyInt(), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    boolean result = operations.deactivateRule(1, 10, authorizedUserSession);

    assertThat(result).isTrue();
    verify(activeRuleDao).delete(eq(5), eq(session));
    verify(activeRuleDao).deleteParameters(eq(5), eq(session));
    verify(session).commit();
    verify(profilesManager).deactivated(eq(1), anyInt(), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void not_deactivate_rule_if_inheritance() throws Exception {
    when(profileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(ruleDao.selectById(10, session)).thenReturn(new RuleDto().setId(10));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setInheritance(ActiveRuleDto.INHERITED);
    when(activeRuleDao.selectByProfileAndRule(1, 10, session)).thenReturn(activeRule);
    when(profilesManager.deactivated(eq(1), anyInt(), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    boolean result = operations.deactivateRule(1, 10, authorizedUserSession);

    assertThat(result).isFalse();
    verify(activeRuleDao, never()).delete(anyInt(), eq(session));
    verify(activeRuleDao, never()).deleteParameters(anyInt(), eq(session));
    verify(session, never()).commit();
    verifyZeroInteractions(profilesManager);
    verifyZeroInteractions(esActiveRule);
  }

  @Test
  public void deactivate_rules() throws Exception {
    when(ruleDao.selectById(10, session)).thenReturn(new RuleDto().setId(10));
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    when(profilesManager.deactivated(eq(1), anyInt(), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    int result = operations.deactivateRules(newArrayList(5), authorizedUserSession);

    assertThat(result).isEqualTo(1);
    verify(activeRuleDao).delete(eq(5), eq(session));
    verify(activeRuleDao).deleteParameters(eq(5), eq(session));
    verify(session).commit();
    verify(profilesManager).deactivated(eq(1), anyInt(), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void create_active_rule_param() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    RuleParamDto ruleParam = new RuleParamDto().setRuleId(10).setName("max").setDefaultValue("20").setType(PropertyType.INTEGER.name());
    when(ruleDao.selectParamByRuleAndKey(10, "max", session)).thenReturn(ruleParam);
    when(profilesManager.ruleParamChanged(eq(1), eq(5), eq("max"), eq((String) null), eq("30"), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.updateActiveRuleParam(5, "max", "30", authorizedUserSession);

    ArgumentCaptor<ActiveRuleParamDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).insert(argumentCaptor.capture(), eq(session));
    assertThat(argumentCaptor.getValue().getKey()).isEqualTo("max");
    assertThat(argumentCaptor.getValue().getValue()).isEqualTo("30");
    assertThat(argumentCaptor.getValue().getActiveRuleId()).isEqualTo(5);

    verify(typeValidations).validate(eq("30"), eq("INTEGER"), anyList());
    verify(session).commit();
    verify(profilesManager).ruleParamChanged(eq(1), eq(5), eq("max"), eq((String) null), eq("30"), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void fail_to_create_active_rule_if_no_rule_param() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    when(ruleDao.selectParamByRuleAndKey(10, "max", session)).thenReturn(null);

    try {
      operations.updateActiveRuleParam(5, "max", "30", authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
    verify(activeRuleDao, never()).insert(any(ActiveRuleParamDto.class), eq(session));
    verifyZeroInteractions(profilesManager);
  }

  @Test
  public void update_active_rule_param() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    RuleParamDto ruleParam = new RuleParamDto().setRuleId(10).setName("max").setDefaultValue("20").setType(PropertyType.INTEGER.name());
    when(ruleDao.selectParamByRuleAndKey(10, "max", session)).thenReturn(ruleParam);
    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setId(100).setActiveRuleId(5).setKey("max").setValue("20");
    when(activeRuleDao.selectParamByActiveRuleAndKey(5, "max", session)).thenReturn(activeRuleParam);

    when(profilesManager.ruleParamChanged(eq(1), eq(5), eq("max"), eq("20"), eq("30"), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.updateActiveRuleParam(5, "max", "30", authorizedUserSession);

    ArgumentCaptor<ActiveRuleParamDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).update(argumentCaptor.capture(), eq(session));
    assertThat(argumentCaptor.getValue().getId()).isEqualTo(100);
    assertThat(argumentCaptor.getValue().getValue()).isEqualTo("30");

    verify(typeValidations).validate(eq("30"), eq("INTEGER"), anyList());
    verify(session).commit();
    verify(profilesManager).ruleParamChanged(eq(1), eq(5), eq("max"), eq("20"), eq("30"), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void update_active_rule_param_with_single_select_list_type() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    RuleParamDto ruleParam = new RuleParamDto().setRuleId(10).setName("max").setDefaultValue("20").setType(RuleParamType.multipleListOfValues("30", "31", "32", "33").toString());
    when(ruleDao.selectParamByRuleAndKey(10, "max", session)).thenReturn(ruleParam);
    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setId(100).setActiveRuleId(5).setKey("max").setValue("20");
    when(activeRuleDao.selectParamByActiveRuleAndKey(5, "max", session)).thenReturn(activeRuleParam);
    when(profilesManager.ruleParamChanged(eq(1), eq(5), eq("max"), eq("20"), eq("30,31,32"), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.updateActiveRuleParam(5, "max", "30,31,32", authorizedUserSession);

    verify(typeValidations).validate(eq(newArrayList("30", "31", "32")), eq("SINGLE_SELECT_LIST"), anyList());
  }

  @Test
  public void remove_active_rule_param() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    RuleParamDto ruleParam = new RuleParamDto().setRuleId(10).setName("max").setDefaultValue("20").setType(PropertyType.INTEGER.name());
    when(ruleDao.selectParamByRuleAndKey(10, "max", session)).thenReturn(ruleParam);
    ActiveRuleParamDto activeRuleParam = new ActiveRuleParamDto().setId(100).setActiveRuleId(5).setKey("max").setValue("20");
    when(activeRuleDao.selectParamByActiveRuleAndKey(5, "max", session)).thenReturn(activeRuleParam);

    when(profilesManager.ruleParamChanged(eq(1), eq(5), eq("max"), eq("20"), eq((String) null), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.updateActiveRuleParam(5, "max", null, authorizedUserSession);

    verify(session).commit();
    verify(activeRuleDao).deleteParameter(100, session);
    verify(profilesManager).ruleParamChanged(eq(1), eq(5), eq("max"), eq("20"), eq((String) null), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
  }

  @Test
  public void revert_active_rule_with_severity_to_update() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setInheritance(ActiveRuleDto.OVERRIDES).setParentId(4);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    ActiveRuleDto parent = new ActiveRuleDto().setId(4).setProfileId(1).setRuleId(10).setSeverity(Severity.MAJOR);
    when(activeRuleDao.selectById(4, session)).thenReturn(parent);

    when(profilesManager.ruleSeverityChanged(eq(1), eq(5), eq(RulePriority.MINOR), eq(RulePriority.MAJOR), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.revertActiveRule(5, authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao, times(2)).update(argumentCaptor.capture(), eq(session));
    List<ActiveRuleDto> activeRulesChanged = argumentCaptor.getAllValues();
    assertThat(activeRulesChanged.get(0).getSeverityString()).isEqualTo(Severity.MAJOR);
    assertThat(activeRulesChanged.get(1).getInheritance()).isEqualTo(ActiveRuleDto.INHERITED);

    verify(session, times(2)).commit();
    verify(profilesManager).ruleSeverityChanged(eq(1), eq(5), eq(RulePriority.MINOR), eq(RulePriority.MAJOR), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
    verify(esActiveRule).save(eq(activeRule), anyListOf(ActiveRuleParamDto.class));
  }

  @Test
  public void fail_to_revert_active_rule_if_no_parent() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setInheritance(ActiveRuleDto.OVERRIDES).setParentId(4);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    when(activeRuleDao.selectById(4, session)).thenReturn(null);

    when(profilesManager.ruleSeverityChanged(eq(1), eq(5), eq(RulePriority.MINOR), eq(RulePriority.MAJOR), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());
    try {
      operations.revertActiveRule(5, authorizedUserSession);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  public void revert_active_rule_with_param_to_update() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setInheritance(ActiveRuleDto.OVERRIDES).setParentId(4);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    when(activeRuleDao.selectParamsByActiveRuleId(eq(5), eq(session))).thenReturn(newArrayList(
      new ActiveRuleParamDto().setId(102).setActiveRuleId(5).setKey("max").setValue("20")
    ));

    ActiveRuleDto parent = new ActiveRuleDto().setId(4).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(4, session)).thenReturn(parent);
    when(activeRuleDao.selectParamsByActiveRuleId(eq(4), eq(session))).thenReturn(newArrayList(
      new ActiveRuleParamDto().setId(100).setActiveRuleId(5).setKey("max").setValue("15")
    ));

    when(profilesManager.ruleParamChanged(eq(1), eq(5), eq("max"), eq("20"), eq("15"), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.revertActiveRule(5, authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).update(argumentCaptor.capture(), eq(session));
    assertThat(argumentCaptor.getValue().getInheritance()).isEqualTo(ActiveRuleDto.INHERITED);

    ArgumentCaptor<ActiveRuleParamDto> paramCaptor = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).update(paramCaptor.capture(), eq(session));
    assertThat(paramCaptor.getValue().getId()).isEqualTo(102);
    assertThat(paramCaptor.getValue().getKey()).isEqualTo("max");
    assertThat(paramCaptor.getValue().getValue()).isEqualTo("15");

    verify(session, times(2)).commit();
    verify(profilesManager).ruleParamChanged(eq(1), eq(5), eq("max"), eq("20"), eq("15"), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
    verify(esActiveRule).save(eq(activeRule), anyListOf(ActiveRuleParamDto.class));
  }

  @Test
  public void revert_active_rule_with_param_to_delete() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setInheritance(ActiveRuleDto.OVERRIDES).setParentId(4);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);
    when(activeRuleDao.selectParamsByActiveRuleId(eq(5), eq(session))).thenReturn(newArrayList(
      new ActiveRuleParamDto().setId(103).setActiveRuleId(5).setKey("format").setValue("abc"))
    );

    ActiveRuleDto parent = new ActiveRuleDto().setId(4).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(4, session)).thenReturn(parent);

    when(profilesManager.ruleParamChanged(eq(1), eq(5), eq("format"), eq("abc"), eq((String) null), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.revertActiveRule(5, authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).update(argumentCaptor.capture(), eq(session));
    assertThat(argumentCaptor.getValue().getInheritance()).isEqualTo(ActiveRuleDto.INHERITED);

    verify(activeRuleDao).deleteParameter(103, session);

    verify(session, times(2)).commit();
    verify(profilesManager).ruleParamChanged(eq(1), eq(5), eq("format"), eq("abc"), eq((String) null), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
    verify(esActiveRule).save(eq(activeRule), anyListOf(ActiveRuleParamDto.class));
  }

  @Test
  public void revert_active_rule_with_param_to_create() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setInheritance(ActiveRuleDto.OVERRIDES).setParentId(4);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);

    ActiveRuleDto parent = new ActiveRuleDto().setId(4).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR);
    when(activeRuleDao.selectById(4, session)).thenReturn(parent);
    when(activeRuleDao.selectParamsByActiveRuleId(eq(4), eq(session))).thenReturn(newArrayList(
      new ActiveRuleParamDto().setId(101).setActiveRuleId(5).setKey("minimum").setValue("2"))
    );

    when(profilesManager.ruleParamChanged(eq(1), eq(5), eq("minimum"), eq((String) null), eq("2"), eq("Nicolas"))).thenReturn(new ProfilesManager.RuleInheritanceActions());

    operations.revertActiveRule(5, authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).update(argumentCaptor.capture(), eq(session));
    assertThat(argumentCaptor.getValue().getInheritance()).isEqualTo(ActiveRuleDto.INHERITED);

    ArgumentCaptor<ActiveRuleParamDto> paramCaptor = ArgumentCaptor.forClass(ActiveRuleParamDto.class);
    verify(activeRuleDao).insert(paramCaptor.capture(), eq(session));
    assertThat(paramCaptor.getValue().getKey()).isEqualTo("minimum");
    assertThat(paramCaptor.getValue().getValue()).isEqualTo("2");

    verify(session, times(2)).commit();
    verify(profilesManager).ruleParamChanged(eq(1), eq(5), eq("minimum"), eq((String) null), eq("2"), eq("Nicolas"));
    verify(esActiveRule).deleteActiveRules(anyListOf(Integer.class));
    verify(esActiveRule).bulkIndexActiveRuleIds(anyListOf(Integer.class), eq(session));
    verify(esActiveRule).save(eq(activeRule), anyListOf(ActiveRuleParamDto.class));
  }

  @Test
  public void no_revert_when_active_rule_do_not_override() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setInheritance(null);
    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);

    when(activeRuleDao.selectById(5, session)).thenReturn(activeRule);

    verifyZeroInteractions(activeRuleDao);
    verifyZeroInteractions(session);
    verifyZeroInteractions(profilesManager);
    verifyZeroInteractions(esActiveRule);
  }

  @Test
  public void update_active_rule_note_when_no_existing_note() throws Exception {
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR).setNoteCreatedAt(null).setNoteData(null);

    List<ActiveRuleParamDto> activeRuleParams = newArrayList(new ActiveRuleParamDto());
    when(activeRuleDao.selectParamsByActiveRuleId(eq(5), eq(session))).thenReturn(activeRuleParams);

    long now = System.currentTimeMillis();
    doReturn(now).when(system).now();

    operations.updateActiveRuleNote(activeRule, "My note", authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).update(argumentCaptor.capture(), eq(session));
    assertThat(argumentCaptor.getValue().getNoteData()).isEqualTo("My note");
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isEqualTo("nicolas");
    assertThat(argumentCaptor.getValue().getNoteCreatedAt().getTime()).isEqualTo(now);
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt().getTime()).isEqualTo(now);

    verify(session).commit();
    verify(esActiveRule).save(eq(activeRule), eq(activeRuleParams));
  }

  @Test
  public void update_active_rule_note_when_already_note() throws Exception {
    Date createdAt = DateUtils.parseDate("2013-12-20");
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR)
      .setNoteCreatedAt(createdAt).setNoteData("My previous note").setNoteUserLogin("nicolas");

    List<ActiveRuleParamDto> activeRuleParams = newArrayList(new ActiveRuleParamDto());
    when(activeRuleDao.selectParamsByActiveRuleId(eq(5), eq(session))).thenReturn(activeRuleParams);

    long now = System.currentTimeMillis();
    doReturn(now).when(system).now();

    operations.updateActiveRuleNote(activeRule, "My new note", MockUserSession.create().setLogin("guy").setName("Guy").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN));

    ArgumentCaptor<ActiveRuleDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).update(argumentCaptor.capture(), eq(session));
    assertThat(argumentCaptor.getValue().getNoteData()).isEqualTo("My new note");
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isEqualTo("nicolas");
    assertThat(argumentCaptor.getValue().getNoteCreatedAt()).isEqualTo(createdAt);
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt().getTime()).isEqualTo(now);

    verify(session).commit();
    verify(esActiveRule).save(eq(activeRule), eq(activeRuleParams));
  }

  @Test
  public void delete_active_rule_note() throws Exception {
    Date createdAt = DateUtils.parseDate("2013-12-20");
    ActiveRuleDto activeRule = new ActiveRuleDto().setId(5).setProfileId(1).setRuleId(10).setSeverity(Severity.MINOR)
      .setNoteData("My note").setNoteUserLogin("nicolas").setNoteCreatedAt(createdAt).setNoteUpdatedAt(createdAt);

    List<ActiveRuleParamDto> activeRuleParams = newArrayList(new ActiveRuleParamDto());
    when(activeRuleDao.selectParamsByActiveRuleId(eq(5), eq(session))).thenReturn(activeRuleParams);

    long now = System.currentTimeMillis();
    doReturn(now).when(system).now();

    operations.deleteActiveRuleNote(activeRule, authorizedUserSession);

    ArgumentCaptor<ActiveRuleDto> argumentCaptor = ArgumentCaptor.forClass(ActiveRuleDto.class);
    verify(activeRuleDao).update(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue().getNoteData()).isNull();
    assertThat(argumentCaptor.getValue().getNoteUserLogin()).isNull();
    assertThat(argumentCaptor.getValue().getNoteCreatedAt()).isNull();
    assertThat(argumentCaptor.getValue().getNoteUpdatedAt()).isNull();

    verify(session).commit();
    verify(esActiveRule).save(eq(activeRule), eq(activeRuleParams));
  }

}
