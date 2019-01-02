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
package org.sonar.ce.task.log;

import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.sonar.ce.task.CeTask;
import org.sonar.process.logging.LogbackHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.log.CeTaskLogging.MDC_CE_TASK_UUID;

public class CeTaskLoggingTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private LogbackHelper helper = new LogbackHelper();
  private CeTaskLogging underTest = new CeTaskLogging();

  @After
  public void resetLogback() throws JoranException {
    helper.resetFromXml("/logback-test.xml");
  }

  @After
  public void cleanMDC() {
    MDC.clear();
  }

  @Test
  public void initForTask_stores_task_uuid_in_MDC() {
    String uuid = "ce_task_uuid";

    underTest.initForTask(createCeTask(uuid));

    assertThat(MDC.get(MDC_CE_TASK_UUID)).isEqualTo(uuid);
  }

  private CeTask createCeTask(String uuid) {
    CeTask ceTask = Mockito.mock(CeTask.class);
    when(ceTask.getUuid()).thenReturn(uuid);
    return ceTask;
  }

  @Test
  public void clearForTask_removes_task_uuid_from_MDC() {
    MDC.put(MDC_CE_TASK_UUID, "some_value");

    underTest.clearForTask();

    assertThat(MDC.get(MDC_CE_TASK_UUID)).isNull();
  }

}
