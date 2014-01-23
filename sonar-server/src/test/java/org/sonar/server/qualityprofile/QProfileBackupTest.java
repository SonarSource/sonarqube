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
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.profiles.XMLProfileSerializer;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.io.Reader;
import java.io.Writer;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QProfileBackupTest {

  @Mock
  DatabaseSessionFactory sessionFactory;

  @Mock
  DatabaseSession hibernateSession;

  @Mock
  MyBatis myBatis;

  @Mock
  SqlSession session;

  @Mock
  XMLProfileParser xmlProfileParser;

  @Mock
  XMLProfileSerializer xmlProfileSerializer;;

  @Mock
  QProfileLookup qProfileLookup;

  @Mock
  ESActiveRule esActiveRule;

  @Mock
  PreviewCache dryRunCache;

  QProfileBackup backup;

  UserSession userSession = MockUserSession.create().setLogin("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  @Before
  public void setUp() throws Exception {
    when(myBatis.openSession()).thenReturn(session);
    when(sessionFactory.getSession()).thenReturn(hibernateSession);

    backup = new QProfileBackup(sessionFactory, xmlProfileParser, xmlProfileSerializer, myBatis, qProfileLookup, esActiveRule, dryRunCache);
  }

  @Test
  public void backup() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getId()).thenReturn(1);
    when(hibernateSession.getSingleResult(any(Class.class), eq("id"), eq(1))).thenReturn(profile);

    backup.backupProfile(new QProfile().setId(1));

    verify(xmlProfileSerializer).write(eq(profile), any(Writer.class));
  }

  @Test
  public void restore() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", false, userSession);

    assertThat(result.profile()).isNotNull();
    verify(hibernateSession).saveWithoutFlush(profile);
    verify(esActiveRule).bulkIndexProfile(anyInt(), eq(session));
    verify(dryRunCache).reportGlobalModification(session);
    verify(session).commit();
  }

  @Test
  public void fail_to_restore_without_profile_admin_permission() throws Exception {
    try {
      backup.restore("<xml/>", false, MockUserSession.create().setLogin("nicolas").setName("Nicolas"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ForbiddenException.class);
    }
    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(esActiveRule);
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void fail_to_restore_if_profile_already_exist() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(RulesProfile.create("Default", "java"));
    when(qProfileLookup.profile(anyInt())).thenReturn(new QProfile().setId(1));

    try {
      backup.restore("<xml/>", false, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("The profile [name=Default,language=java] already exists. Please delete it before restoring.");
    }

    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(esActiveRule);
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void restore_should_delete_existing_profile() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);

    RulesProfile existingProfile = mock(RulesProfile.class);
    when(existingProfile.getId()).thenReturn(1);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(existingProfile);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", true, userSession);

    assertThat(result.profile()).isNotNull();
    verify(hibernateSession).removeWithoutFlush(eq(existingProfile));
    verify(esActiveRule).deleteActiveRulesFromProfile(eq(1));
    verify(esActiveRule).bulkIndexProfile(anyInt(), eq(session));
    verify(dryRunCache).reportGlobalModification(session);
    verify(session).commit();
  }

  @Test
  public void restore_should_add_warnings_and_infos_from_xml_parsing() throws Exception {
    final RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ValidationMessages validationMessages = (ValidationMessages) args[1];
        validationMessages.addInfoText("an info message");
        validationMessages.addWarningText("a warning message");
        return profile;
      }
    }).when(xmlProfileParser).parse(any(Reader.class), any(ValidationMessages.class));

    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", true, userSession);

    assertThat(result.profile()).isNotNull();
    assertThat(result.warnings()).isNotEmpty();
    assertThat(result.infos()).isNotEmpty();
  }

  @Test
  public void restore_should_fail_if_errors_when_parsing_xml() throws Exception {
    final RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ValidationMessages validationMessages = (ValidationMessages) args[1];
        validationMessages.addErrorText("error!");
        return profile;
      }
    }).when(xmlProfileParser).parse(any(Reader.class), any(ValidationMessages.class));

    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    try {
      backup.restore("<xml/>", false, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Fail to restore profile");
      BadRequestException badRequestException = (BadRequestException) e;
      assertThat(badRequestException.errors()).hasSize(1);
      assertThat(badRequestException.errors().get(0).text()).isEqualTo("error!");
    }

    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(esActiveRule);
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void do_not_restore_if_xml_is_empty() throws Exception {
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(null);
    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(new QProfile().setId(1));

    QProfileResult result = backup.restore("<xml/>", false, userSession);

    assertThat(result.profile()).isNull();
    verify(hibernateSession, never()).saveWithoutFlush(any(RulesProfile.class));
    verifyZeroInteractions(esActiveRule);
    verifyZeroInteractions(dryRunCache);
  }

  @Test
  public void do_not_restore_if_new_profile_is_null_after_import() throws Exception {
    RulesProfile profile = mock(RulesProfile.class);
    when(profile.getName()).thenReturn("Default");
    when(profile.getLanguage()).thenReturn("java");
    when(profile.getId()).thenReturn(1);
    when(xmlProfileParser.parse(any(Reader.class), any(ValidationMessages.class))).thenReturn(profile);

    when(hibernateSession.getSingleResult(any(Class.class), eq("name"), eq("Default"), eq("language"), eq("java"))).thenReturn(null);
    when(qProfileLookup.profile(anyInt(), eq(session))).thenReturn(null);

    try {
      backup.restore("<xml/>", false, userSession);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Restore of the profile has failed.");
    }

    verifyZeroInteractions(esActiveRule);
    verifyZeroInteractions(dryRunCache);
  }
}
