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
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class DistributedCEQueueStatusImplTest extends CommonCEQueueStatusImplTest {
  private DistributedCEQueueStatusImpl underTest = new DistributedCEQueueStatusImpl(getDbClient(), mock(System2.class));

  public DistributedCEQueueStatusImplTest() {
    super(mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS));
  }

  @Override
  protected CEQueueStatusImpl getUnderTest() {
    return underTest;
  }

  @Test
  public void getPendingCount_returns_0_without_querying_database() {
    assertThat(underTest.getPendingCount()).isZero();

    verifyZeroInteractions(getDbClient());
  }
}
