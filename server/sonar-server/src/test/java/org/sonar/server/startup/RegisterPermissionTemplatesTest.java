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

package org.sonar.server.startup;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.loadedtemplate.LoadedTemplateDao;
import org.sonar.db.loadedtemplate.LoadedTemplateDto;
import org.sonar.db.permission.PermissionTemplateDao;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDao;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDao;
import org.sonar.server.permission.DefaultPermissionTemplates;
import org.sonar.server.platform.PersistentSettings;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;

public class RegisterPermissionTemplatesTest {

  private PersistentSettings settings;
  private LoadedTemplateDao loadedTemplateDao;
  private PermissionTemplateDao permissionTemplateDao;
  private DbClient dbClient;
  private UserDao userDao;
  private GroupDao groupDao;

  @Before
  public void setUp() {
    settings = mock(PersistentSettings.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);
    permissionTemplateDao = mock(PermissionTemplateDao.class);
    userDao = mock(UserDao.class);
    groupDao = mock(GroupDao.class);

    dbClient = mock(DbClient.class);
    when(dbClient.permissionTemplateDao()).thenReturn(permissionTemplateDao);
    when(dbClient.loadedTemplateDao()).thenReturn(loadedTemplateDao);
    when(dbClient.userDao()).thenReturn(userDao);
    when(dbClient.groupDao()).thenReturn(groupDao);
  }

  @Test
  public void should_insert_and_register_default_permission_template() {
    LoadedTemplateDto expectedTemplate = new LoadedTemplateDto().setKey(DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid())
      .setType(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE);
    PermissionTemplateDto permissionTemplate = DefaultPermissionTemplates.DEFAULT_TEMPLATE.setId(1L);

    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE, DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid()))
      .thenReturn(0);
    when(permissionTemplateDao.insert(any(DbSession.class), eq(DefaultPermissionTemplates.DEFAULT_TEMPLATE)))
      .thenReturn(permissionTemplate);
    when(groupDao.selectByName(any(DbSession.class), eq(DefaultGroups.ADMINISTRATORS))).thenReturn(new GroupDto().setId(1L));
    when(groupDao.selectByName(any(DbSession.class), eq(DefaultGroups.USERS))).thenReturn(new GroupDto().setId(2L));

    RegisterPermissionTemplates initializer = new RegisterPermissionTemplates(dbClient, settings);
    initializer.start();

    verify(loadedTemplateDao).insert(argThat(Matches.template(expectedTemplate)));
    verify(permissionTemplateDao).insert(any(DbSession.class), eq(DefaultPermissionTemplates.DEFAULT_TEMPLATE));
    verify(permissionTemplateDao).insertGroupPermission(1L, 1L, UserRole.ADMIN);
    verify(permissionTemplateDao).insertGroupPermission(1L, 1L, UserRole.ISSUE_ADMIN);
    verify(permissionTemplateDao).insertGroupPermission(1L, null, UserRole.USER);
    verify(permissionTemplateDao).insertGroupPermission(1L, null, UserRole.CODEVIEWER);
    verifyNoMoreInteractions(permissionTemplateDao);
    verify(settings).saveProperty(DEFAULT_TEMPLATE_PROPERTY, DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid());
  }

  @Test
  public void should_skip_insertion_and_registration() {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE, DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid()))
      .thenReturn(1);

    RegisterPermissionTemplates initializer = new RegisterPermissionTemplates(dbClient, settings);
    initializer.start();

    verifyZeroInteractions(permissionTemplateDao);
    verify(loadedTemplateDao, never()).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_reference_TRK_template_as_default_when_present() {
    when(settings.getString(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT))).thenReturn("my_projects_template");

    LoadedTemplateDto expectedTemplate = new LoadedTemplateDto().setKey(DefaultPermissionTemplates.DEFAULT_TEMPLATE.getUuid())
      .setType(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE);

    RegisterPermissionTemplates initializer = new RegisterPermissionTemplates(dbClient, settings);
    initializer.start();

    verify(loadedTemplateDao).insert(argThat(Matches.template(expectedTemplate)));
    verify(settings).saveProperty(DEFAULT_TEMPLATE_PROPERTY, "my_projects_template");
    verifyZeroInteractions(permissionTemplateDao);
  }

  private static class Matches extends BaseMatcher<LoadedTemplateDto> {

    private final LoadedTemplateDto referenceTemplate;

    private Matches(LoadedTemplateDto referenceTemplate) {
      this.referenceTemplate = referenceTemplate;
    }

    static Matches template(LoadedTemplateDto referenceTemplate) {
      return new Matches(referenceTemplate);
    }

    @Override
    public boolean matches(Object o) {
      if (o != null && o instanceof LoadedTemplateDto) {
        LoadedTemplateDto otherTemplate = (LoadedTemplateDto) o;
        return referenceTemplate.getKey().equals(otherTemplate.getKey())
          && referenceTemplate.getType().equals(otherTemplate.getType());
      }
      return false;
    }

    @Override
    public void describeTo(Description description) {
    }
  }
}
