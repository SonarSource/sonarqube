/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.setting.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Settings.GenerateSecretKeyWsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GenerateSecretKeyActionWithPersisterIT {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setSystemAdministrator();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private DbClient dbClient = db.getDbClient();
  private MapSettings settings = new MapSettings();
  private GenerateSecretKeyAction underTest = new GenerateSecretKeyAction(dbClient, settings, userSession, auditPersister);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void generateValidSecretKeyIsPersisted() {
    call();

    verify(auditPersister).generateSecretKey(any());
  }

  private GenerateSecretKeyWsResponse call() {
    return ws.newRequest()
      .setMethod("GET")
      .executeProtobuf(GenerateSecretKeyWsResponse.class);
  }

}
