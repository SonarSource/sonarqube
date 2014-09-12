/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process.monitor;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.process.ProcessMXBean;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RmiJmxConnectorTest {

  @Test
  public void throw_exception_on_timeout() throws Exception {
    RmiJmxConnector connector = new RmiJmxConnector();
    ProcessRef ref = mock(ProcessRef.class);
    ProcessMXBean mxBean = mock(ProcessMXBean.class);
    connector.register(ref, mxBean);

    when(mxBean.isReady()).thenAnswer(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
        Thread.sleep(Long.MAX_VALUE);
        return null;
      }
    });

    try {
      connector.isReady(ref, 5L);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail send JMX request");
    }
  }
}
