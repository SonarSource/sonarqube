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
package org.sonar.db.profiling;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Assert;
import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InvocationUtilsTest {

  @Test
  public void should_return_result() throws Throwable {
    String toString = "toString";
    Object target = mock(Object.class);
    when(target.toString()).thenReturn(toString);

    assertThat(InvocationUtils.invokeQuietly(target, Object.class.getMethod("toString"), new Object[0])).isEqualTo(toString);
  }

  @Test
  public void should_throw_declared_exception() throws Throwable {
    Connection target = mock(Connection.class);
    String failSql = "any sql";
    when(target.prepareStatement(failSql)).thenThrow(new SQLException("Expected"));
    Method prepareStatement = Connection.class.getMethod("prepareStatement", String.class);

    Assert.assertThrows(SQLException.class, () -> InvocationUtils.invokeQuietly(target, prepareStatement, new Object[] {failSql}));
  }

  @Test
  public void only_static_methods() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(InvocationUtils.class)).isTrue();

  }
}
