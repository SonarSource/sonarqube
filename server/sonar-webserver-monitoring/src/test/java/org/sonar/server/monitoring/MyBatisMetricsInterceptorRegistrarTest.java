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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.DefaultMyBatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MyBatisMetricsInterceptorRegistrarTest {

  private final DefaultMyBatis myBatis = mock();
  private final ServerMonitoringMetrics metrics = mock();
  private final MapSettings settings = new MapSettings();
  private final MyBatisMetricsInterceptorRegistrar underTest = new MyBatisMetricsInterceptorRegistrar(myBatis, metrics, settings.asConfig());

  @Test
  public void start_addsMyBatisMetricsInterceptorToMyBatis() {
    underTest.start();

    ArgumentCaptor<org.apache.ibatis.plugin.Interceptor> captor = ArgumentCaptor.forClass(org.apache.ibatis.plugin.Interceptor.class);
    verify(myBatis).addInterceptor(captor.capture());
    assertThat(captor.getValue()).isInstanceOf(MyBatisMetricsInterceptor.class);
  }

  @Test
  public void stop_isNoOp() {
    underTest.stop();

    verifyNoMoreInteractions(myBatis);
  }
}
