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
package org.sonar.db.purge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ComponentNewValue;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PurgeDaoWithAuditIT {

  private final System2 system2 = mock(System2.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private final AuditPersister auditPersister = mock(AuditPersister.class);
  private final PurgeDao underTestWithPersister = new PurgeDao(system2, auditPersister);

  @Test
  void delete_project_persist_audit_with_uuid_and_name() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ArgumentCaptor<ComponentNewValue> newValueCaptor = ArgumentCaptor.forClass(ComponentNewValue.class);
    underTestWithPersister.deleteProject(dbSession, project.uuid(), project.qualifier(), project.name(), project.getKey());

    verify(auditPersister).deleteComponent(any(DbSession.class), newValueCaptor.capture());
    ComponentNewValue componentNewValue = newValueCaptor.getValue();
    assertThat(componentNewValue)
      .extracting(ComponentNewValue::getComponentUuid, ComponentNewValue::getComponentName, ComponentNewValue::getComponentKey,
        ComponentNewValue::getQualifier)
      .containsExactly(project.uuid(), project.name(), project.getKey(), "TRK");
  }

}
