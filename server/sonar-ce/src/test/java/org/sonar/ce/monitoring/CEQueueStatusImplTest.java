/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.monitoring;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.ce.CeQueueDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CEQueueStatusImplTest extends CommonCEQueueStatusImplTest {
  private CEQueueStatusImpl underTest = new CEQueueStatusImpl(getDbClient());

  public CEQueueStatusImplTest() {
    super(mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS));
  }

  @Override
  protected CEQueueStatusImpl getUnderTest() {
    return underTest;
  }

  @Test
  public void count_Pending_from_database() {
    when(getDbClient().ceQueueDao().countByStatus(any(), eq(CeQueueDto.Status.PENDING))).thenReturn(42);

    assertThat(underTest.getPendingCount()).isEqualTo(42);
  }
}
