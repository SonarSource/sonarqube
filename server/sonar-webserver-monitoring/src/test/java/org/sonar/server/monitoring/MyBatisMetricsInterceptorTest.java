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
package org.sonar.server.monitoring;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Invocation;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.doubleThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MyBatisMetricsInterceptorTest {

  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final MapSettings settings = new MapSettings();
  private MyBatisMetricsInterceptor underTest;

  @Before
  public void before() {
    underTest = new MyBatisMetricsInterceptor(metrics, settings.asConfig());
  }

  @Test
  public void intercept_whenEnabled_recordsDurationWithMapperMethodId() throws Throwable {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    MappedStatement mappedStatement = mock(MappedStatement.class);
    when(mappedStatement.getId()).thenReturn("org.sonar.db.user.UserMapper.selectByUuid");
    Invocation invocation = mock(Invocation.class);
    when(invocation.getArgs()).thenReturn(new Object[] {mappedStatement, new Object()});
    when(invocation.proceed()).thenReturn("result");

    Object result = underTest.intercept(invocation);

    assertThat(result).isEqualTo("result");
    verify(metrics, times(1)).observeDbQueryDuration(doubleThat(d -> d >= 0), eq("UserMapper.selectByUuid"));
  }

  @Test
  public void intercept_whenEnabled_recordsDurationEvenWhenProceedThrows() throws Throwable {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    MappedStatement mappedStatement = mock(MappedStatement.class);
    when(mappedStatement.getId()).thenReturn("org.sonar.db.user.UserMapper.failing");
    Invocation invocation = mock(Invocation.class);
    when(invocation.getArgs()).thenReturn(new Object[] {mappedStatement, new Object()});
    when(invocation.proceed()).thenThrow(new RuntimeException("boom"));

    assertThatThrownBy(() -> underTest.intercept(invocation))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("boom");

    verify(metrics, times(1)).observeDbQueryDuration(doubleThat(d -> d >= 0), eq("UserMapper.failing"));
  }

  @Test
  public void intercept_whenEnabled_doesNotRecordWhenFirstArgIsNotMappedStatement() throws Throwable {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    Invocation invocation = mock(Invocation.class);
    when(invocation.getArgs()).thenReturn(new Object[] {"not a mapped statement"});
    when(invocation.proceed()).thenReturn(null);

    underTest.intercept(invocation);

    verify(metrics, never()).observeDbQueryDuration(anyDouble(), anyString());
  }

  @Test
  public void intercept_whenDisabled_recordsNothing() throws Throwable {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), false);
    MappedStatement mappedStatement = mock(MappedStatement.class);
    Invocation invocation = mock(Invocation.class);
    when(invocation.getArgs()).thenReturn(new Object[] {mappedStatement, new Object()});
    when(invocation.proceed()).thenReturn("result");

    Object result = underTest.intercept(invocation);

    assertThat(result).isEqualTo("result");
    verify(metrics, never()).observeDbQueryDuration(anyDouble(), anyString());
  }

  @Test
  public void shortMapperMethod_stripsPackagePrefix() {
    assertThat(MyBatisMetricsInterceptor.shortMapperMethod("org.sonar.db.notification.NotificationQueueMapper.delete"))
      .isEqualTo("NotificationQueueMapper.delete");
    assertThat(MyBatisMetricsInterceptor.shortMapperMethod("UserMapper.selectByUuid"))
      .isEqualTo("UserMapper.selectByUuid");
    assertThat(MyBatisMetricsInterceptor.shortMapperMethod("noDots")).isEqualTo("noDots");
  }

  @Test
  public void intercept_whenPropertyMissing_defaultsToEnabled() throws Throwable {
    MappedStatement mappedStatement = mock(MappedStatement.class);
    when(mappedStatement.getId()).thenReturn("org.sonar.db.user.UserMapper.selectByUuid");
    Invocation invocation = mock(Invocation.class);
    when(invocation.getArgs()).thenReturn(new Object[] {mappedStatement, new Object()});
    when(invocation.proceed()).thenReturn("result");

    underTest.intercept(invocation);

    verify(metrics, times(1)).observeDbQueryDuration(doubleThat(d -> d >= 0), eq("UserMapper.selectByUuid"));
  }
}
