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
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.PermissionTemplateDao;
import org.sonar.core.permission.PermissionTemplateDto;
import org.sonar.core.template.LoadedTemplateDao;
import org.sonar.core.template.LoadedTemplateDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDao;
import org.sonar.server.platform.PersistentSettings;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class RegisterPermissionTemplatesTest {

  private PersistentSettings settings;
  private LoadedTemplateDao loadedTemplateDao;
  private PermissionTemplateDao permissionTemplateDao;
  private UserDao userDao;

  @Before
  public void setUp() {
    settings = mock(PersistentSettings.class);
    loadedTemplateDao = mock(LoadedTemplateDao.class);
    permissionTemplateDao = mock(PermissionTemplateDao.class);
    userDao = mock(UserDao.class);
  }

  @Test
  public void should_insert_and_register_default_permission_template() throws Exception {
    LoadedTemplateDto expectedTemplate = new LoadedTemplateDto().setKey(PermissionTemplateDto.DEFAULT.getKee())
      .setType(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE);
    PermissionTemplateDto permissionTemplate = PermissionTemplateDto.DEFAULT.setId(1L);

    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE, PermissionTemplateDto.DEFAULT.getKee()))
      .thenReturn(0);
    when(permissionTemplateDao.createPermissionTemplate(PermissionTemplateDto.DEFAULT.getName(), PermissionTemplateDto.DEFAULT.getDescription(), null))
      .thenReturn(permissionTemplate);
    when(userDao.selectGroupByName(DefaultGroups.ADMINISTRATORS)).thenReturn(new GroupDto().setId(1L));
    when(userDao.selectGroupByName(DefaultGroups.USERS)).thenReturn(new GroupDto().setId(2L));

    RegisterPermissionTemplates initializer = new RegisterPermissionTemplates(loadedTemplateDao, permissionTemplateDao, userDao, settings);
    initializer.start();

    verify(loadedTemplateDao).insert(argThat(Matches.template(expectedTemplate)));
    verify(permissionTemplateDao).createPermissionTemplate(PermissionTemplateDto.DEFAULT.getName(), PermissionTemplateDto.DEFAULT.getDescription(), null);
    verify(permissionTemplateDao).addGroupPermission(1L, 1L, UserRole.ADMIN);
    verify(permissionTemplateDao).addGroupPermission(1L, 1L, UserRole.ISSUE_ADMIN);
    verify(permissionTemplateDao).addGroupPermission(1L, null, UserRole.USER);
    verify(permissionTemplateDao).addGroupPermission(1L, null, UserRole.CODEVIEWER);
    verifyNoMoreInteractions(permissionTemplateDao);
    verify(settings).saveProperty(RegisterPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY, PermissionTemplateDto.DEFAULT.getKee());
  }

  @Test
  public void should_skip_insertion_and_registration() throws Exception {
    when(loadedTemplateDao.countByTypeAndKey(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE, PermissionTemplateDto.DEFAULT.getKee()))
      .thenReturn(1);

    RegisterPermissionTemplates initializer = new RegisterPermissionTemplates(loadedTemplateDao, permissionTemplateDao, userDao, settings);
    initializer.start();

    verifyZeroInteractions(permissionTemplateDao, settings);
    verify(loadedTemplateDao, never()).insert(any(LoadedTemplateDto.class));
  }

  @Test
  public void should_reference_TRK_template_as_default_when_present() throws Exception {
    when(settings.getString(RegisterPermissionTemplates.DEFAULT_PROJECTS_TEMPLATE_PROPERTY)).thenReturn("my_projects_template");

    LoadedTemplateDto expectedTemplate = new LoadedTemplateDto().setKey(PermissionTemplateDto.DEFAULT.getKee())
      .setType(LoadedTemplateDto.PERMISSION_TEMPLATE_TYPE);

    RegisterPermissionTemplates initializer = new RegisterPermissionTemplates(loadedTemplateDao, permissionTemplateDao, userDao, settings);
    initializer.start();

    verify(loadedTemplateDao).insert(argThat(Matches.template(expectedTemplate)));
    verify(settings).saveProperty(RegisterPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY, "my_projects_template");
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
