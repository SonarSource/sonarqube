/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.permission.ws.template;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.template.PermissionTemplateDao;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.permission.RequestValidator.MSG_TEMPLATE_WITH_SAME_NAME;

public class UpdateTemplateActionTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final PermissionTemplateDao permissionTemplateDao = mock(PermissionTemplateDao.class);
  private final UserSession userSession = mock(UserSession.class);
  private final PermissionWsSupport wsSupport = mock(PermissionWsSupport.class);
  private final Request request = mock(Request.class);
  private final UpdateTemplateAction underTest = new UpdateTemplateAction(dbClient, userSession, mock(System2.class), wsSupport);

  @Test
  public void return_bad_request_when_template_is_renamed_concurrently() {
    configureTemplateUpdate();
    when(permissionTemplateDao.selectByName(dbSession, "Finance"))
      .thenReturn(null, new PermissionTemplateDto().setName("Finance"));
    when(permissionTemplateDao.update(any(), any())).thenThrow(new IllegalStateException("unique constraint violation"));

    assertThatThrownBy(() -> underTest.handle(request, mock(Response.class)))
      .isInstanceOf(BadRequestException.class)
      .hasMessage(String.format(MSG_TEMPLATE_WITH_SAME_NAME, "Finance"));

    verify(dbSession).rollback();
  }

  @Test
  public void propagate_update_failure_when_template_was_not_renamed_concurrently() {
    configureTemplateUpdate();
    IllegalStateException failure = new IllegalStateException("database unavailable");
    when(permissionTemplateDao.selectByName(dbSession, "Finance")).thenReturn(null);
    when(permissionTemplateDao.update(any(), any())).thenThrow(failure);

    assertThatThrownBy(() -> underTest.handle(request, mock(Response.class)))
      .isSameAs(failure);

    verify(dbSession).rollback();
  }

  private void configureTemplateUpdate() {
    PermissionTemplateDto template = new PermissionTemplateDto().setUuid("template-uuid").setName("Original name");
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.permissionTemplateDao()).thenReturn(permissionTemplateDao);
    when(wsSupport.findTemplate(any(), any())).thenReturn(template);
    when(userSession.checkLoggedIn()).thenReturn(userSession);
    when(userSession.checkPermission(GlobalPermission.ADMINISTER)).thenReturn(userSession);
    when(request.mandatoryParam("id")).thenReturn(template.getUuid());
    when(request.param("name")).thenReturn("Finance");
  }
}
