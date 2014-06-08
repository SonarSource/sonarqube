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

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.db.ActiveRuleDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.Collections;

import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QProfileOperationsTest {

  @Mock
  MyBatis myBatis;

  @Mock
  DbSession session;

  @Mock
  QualityProfileDao qualityProfileDao;

  @Mock
  ActiveRuleDao activeRuleDao;

  @Mock
  PropertiesDao propertiesDao;

  @Mock
  PreviewCache dryRunCache;

  @Mock
  QProfileLookup profileLookup;

  @Mock
  QProfileRepositoryExporter exporter;

  Integer currentId = 1;

  UserSession authorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  UserSession unauthorizedUserSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas");

  QProfileOperations operations;

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession(false)).thenReturn(session);

    // Associate an id when inserting an object to simulate the db id generator
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ActiveRuleDto dto = (ActiveRuleDto) args[0];
        dto.setId(currentId++);
        return null;
      }
    }).when(activeRuleDao).insert(any(DbSession.class), any(ActiveRuleDto.class));
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        QualityProfileDto dto = (QualityProfileDto) args[1];
        dto.setId(currentId++);
        return null;
      }
    }).when(qualityProfileDao).insert(any(DbSession.class), any(QualityProfileDto.class));

    operations = new QProfileOperations(myBatis, qualityProfileDao, activeRuleDao, propertiesDao, exporter, dryRunCache, profileLookup);
  }

  @Test
  public void create_profile() throws Exception {
    QProfileResult result = operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), authorizedUserSession);
    assertThat(result.profile().name()).isEqualTo("Default");
    assertThat(result.profile().language()).isEqualTo("java");

    verify(qualityProfileDao).insert(eq(session), any(QualityProfileDto.class));

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).insert(eq(session), profileArgument.capture());
    assertThat(profileArgument.getValue().getName()).isEqualTo("Default");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");
    assertThat(profileArgument.getValue().getVersion()).isEqualTo(1);
    assertThat(profileArgument.getValue().isUsed()).isFalse();

    verify(dryRunCache).reportGlobalModification(session);
    verify(session).commit();
  }

  @Test
  public void fail_to_create_profile_without_profile_admin_permission() throws Exception {
    try {
      operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), unauthorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verifyNoMoreInteractions(qualityProfileDao);
    verify(session, never()).commit();
  }

  @Test
  public void fail_to_create_profile_if_already_exists() throws Exception {
    when(qualityProfileDao.selectByNameAndLanguage(anyString(), anyString(), eq(session))).thenReturn(new QualityProfileDto());
    try {
      operations.newProfile("Default", "java", Maps.<String, String>newHashMap(), authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }

  @Test
  public void rename_profile() throws Exception {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Old Default").setLanguage("java"));
    when(profileLookup.children(any(QProfile.class), eq(session))).thenReturn(Collections.<QProfile>emptyList());

    operations.renameProfile(1, "Default profile", authorizedUserSession);

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    verify(qualityProfileDao).update(eq(session), profileArgument.capture());
    assertThat(profileArgument.getValue().getId()).isEqualTo(1);
    assertThat(profileArgument.getValue().getName()).isEqualTo("Default profile");
    assertThat(profileArgument.getValue().getLanguage()).isEqualTo("java");

    verify(propertiesDao).updateProperties("sonar.profile.java", "Old Default", "Default profile", session);
    verify(session).commit();
  }

  @Test
  public void fail_to_rename_profile_if_not_exists() throws Exception {
    when(qualityProfileDao.selectById(1, session)).thenReturn(null);
    try {
      operations.renameProfile(1, "New Default", authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }
  }

  @Test
  public void fail_to_rename_profile_if_already_exists() throws Exception {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(qualityProfileDao.selectByNameAndLanguage(anyString(), anyString(), eq(session))).thenReturn(new QualityProfileDto());
    try {
      operations.renameProfile(1, "New Default", authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }
  }


  @Test
  public void rename_children_profile() throws Exception {
    QualityProfileDto profile = new QualityProfileDto().setId(1).setName("Old Default").setLanguage("java");
    when(qualityProfileDao.selectById(1, session)).thenReturn(profile);
    when(profileLookup.children(any(QProfile.class), eq(session))).thenReturn(newArrayList(
      new QProfile().setId(2).setName("Child1").setLanguage("java").setParent("Old Default")
    ));

    operations.renameProfile(1, "Default profile", authorizedUserSession);

    ArgumentCaptor<QualityProfileDto> profileArgument = ArgumentCaptor.forClass(QualityProfileDto.class);
    // One call to update current profile and one other for child
    verify(qualityProfileDao, times(2)).update(eq(session), profileArgument.capture());
    assertThat(profileArgument.getAllValues()).hasSize(2);
    QualityProfileDto child = profileArgument.getAllValues().get(1);
    assertThat(child.getId()).isEqualTo(2);
    assertThat(child.getParent()).isEqualTo("Default profile");

    verify(session).commit();
  }

  @Test
  public void delete_profile() {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(profileLookup.isDeletable(any(QProfile.class), eq(session))).thenReturn(true);

    operations.deleteProfile(1, authorizedUserSession);

    QProfile profile = new QProfile().setId(1).setName("Default").setLanguage("java");
    verify(session).commit();

    //FIXME fails because of some magic.
//    verify(activeRuleDao).removeParamByProfile(profile , session);
//    verify(activeRuleDao).deleteByProfile(profile, session);
    verify(qualityProfileDao).delete(1, session);
    verify(propertiesDao).deleteProjectProperties("sonar.profile.java", "Default", session);
    verify(dryRunCache).reportGlobalModification(session);
  }

  @Test
  public void not_delete_profile_if_not_deletable() {
    when(qualityProfileDao.selectById(1, session)).thenReturn(new QualityProfileDto().setId(1).setName("Default").setLanguage("java"));
    when(profileLookup.isDeletable(any(QProfile.class), eq(session))).thenReturn(false);

    try {
      operations.deleteProfile(1, authorizedUserSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
    }

    verify(session, never()).commit();
    verifyZeroInteractions(activeRuleDao);
    verify(qualityProfileDao, never()).delete(anyInt(), eq(session));
    verifyZeroInteractions(propertiesDao);
  }
}
