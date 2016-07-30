/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.app;

import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartupBarrierFactoryTest {

  private Props props = new Props(new Properties());
  private ProcessEntryPoint entryPoint = mock(ProcessEntryPoint.class);
  private StartupBarrierFactory underTest = new StartupBarrierFactory();

  @Before
  public void setUp() {
    when(entryPoint.getProps()).thenReturn(props);
  }

  @Test
  public void wait_for_web_server_in_standard_mode() {
    StartupBarrier barrier = underTest.create(entryPoint);

    assertThat(barrier).isInstanceOf(WebServerBarrier.class);
  }

  @Test
  public void do_not_wait_for_web_server_if_it_is_disabled() {
    props.set(ProcessProperties.CLUSTER_WEB_DISABLED, "true");
    StartupBarrier barrier = underTest.create(entryPoint);

    assertThat(barrier).isNotInstanceOf(WebServerBarrier.class);
    assertThat(barrier.waitForOperational()).isTrue();
  }
}
