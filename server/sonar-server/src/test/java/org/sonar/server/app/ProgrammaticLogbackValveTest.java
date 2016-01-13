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
package org.sonar.server.app;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.apache.catalina.Container;
import org.junit.AfterClass;
import org.junit.Test;
import org.sonar.process.LogbackHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProgrammaticLogbackValveTest {

  @AfterClass
  public static void resetLogback() throws Exception {
    new LogbackHelper().resetFromXml("/logback-test.xml");
  }

  @Test
  public void startInternal() throws Exception {
    ProgrammaticLogbackValve valve = new ProgrammaticLogbackValve();
    valve.setContainer(mock(Container.class));
    LogbackHelper helper = new LogbackHelper();
    ConsoleAppender<IAccessEvent> appender = helper.newConsoleAppender(valve, "CONSOLE", "combined", null);
    valve.addAppender(appender);

    valve.start();
    assertThat(valve.isStarted()).isTrue();

    valve.stop();
    assertThat(valve.isStarted()).isFalse();
  }

}
