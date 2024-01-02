/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.plugin;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PluginNewValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.db.plugin.PluginDto.Type.EXTERNAL;

public class PluginDaoWithPersisterTest {
  private final AuditPersister auditPersister = mock(AuditPersister.class);

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE, auditPersister);

  private final ArgumentCaptor<PluginNewValue> newValueCaptor = ArgumentCaptor.forClass(PluginNewValue.class);
  private final DbSession session = db.getSession();

  private PluginDao underTest = db.getDbClient().pluginDao();

  @Test
  public void insert() {
    PluginDto dto = getPluginDto();
    underTest.insert(session, dto);

    verify(auditPersister).addPlugin(eq(session), newValueCaptor.capture());
    PluginNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PluginNewValue::getPluginUuid, PluginNewValue::getKee,
        PluginNewValue::getBasePluginKey, PluginNewValue::getType)
      .containsExactly(dto.getUuid(), dto.getKee(), dto.getBasePluginKey(), dto.getType().name());
    assertThat(newValue.toString()).contains("pluginUuid");
  }

  @Test
  public void update() {
    PluginDto dto = getPluginDto();
    underTest.update(session, dto);

    verify(auditPersister).updatePlugin(eq(session), newValueCaptor.capture());
    PluginNewValue newValue = newValueCaptor.getValue();
    assertThat(newValue)
      .extracting(PluginNewValue::getPluginUuid, PluginNewValue::getKee,
        PluginNewValue::getBasePluginKey, PluginNewValue::getType)
      .containsExactly(dto.getUuid(), dto.getKee(), dto.getBasePluginKey(), dto.getType().name());
    assertThat(newValue.toString()).contains("pluginUuid");
  }

  private PluginDto getPluginDto() {
    return new PluginDto()
      .setUuid("plugin_uuid")
      .setKee("plugin_kee")
      .setBasePluginKey("plugin_base_key")
      .setFileHash("plugin_file_hash")
      .setType(EXTERNAL)
      .setCreatedAt(1L)
      .setUpdatedAt(2L);
  }
}
