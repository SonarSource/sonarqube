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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

public class UpsertImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void setBatchSize_throws_IAE_if_value_is_negative() throws Exception {
    UpsertImpl underTest = create();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("size must be positive. Got -1");

    underTest.setBatchSize(-1);
  }

  @Test
  public void setBatchSize_accepts_zero() throws Exception {
    UpsertImpl underTest = create();

    underTest.setBatchSize(0);

    assertThat(underTest.getMaxBatchSize()).isEqualTo(0);
  }

  @Test
  public void setBatchSize_accepts_strictly_positive_value() throws Exception {
    UpsertImpl underTest = create();

    underTest.setBatchSize(42);

    assertThat(underTest.getMaxBatchSize()).isEqualTo(42);
  }

  @Test
  public void maxBatchSize_is_250_by_default() throws Exception {
    UpsertImpl underTest = create();

    assertThat(underTest.getMaxBatchSize()).isEqualTo(250);
  }

  private UpsertImpl create() throws Exception {
    return UpsertImpl.create(mock(Connection.class), "sql");
  }
}
