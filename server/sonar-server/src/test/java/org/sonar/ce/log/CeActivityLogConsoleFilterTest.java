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
package org.sonar.ce.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.core.spi.FilterReply.ACCEPT;
import static ch.qos.logback.core.spi.FilterReply.DENY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sonar.ce.log.CeLogging.MDC_CE_ACTIVITY_FLAG;

public class CeActivityLogConsoleFilterTest {
  private ILoggingEvent loggingEvent = Mockito.mock(ILoggingEvent.class);

  private CeActivityLogConsoleFilter underTest = new CeActivityLogConsoleFilter();

  @Before
  public void setUp() throws Exception {
    when(loggingEvent.getLevel()).thenReturn(INFO);
  }

  @After
  public void tearDown() {
    MDC.clear();
  }

  @Test
  public void accepts_logs_when_property_is_not_set_in_MDC() {
    assertThat(underTest.decide(loggingEvent)).isEqualTo(ACCEPT);
  }

  @Test
  public void rejects_logs_when_property_is_set_to_anything_but_all_in_lowercase_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "bla");
    assertThat(underTest.decide(loggingEvent)).isEqualTo(DENY);
  }

  @Test
  public void rejects_logs_when_property_is_set_to_all_not_in_lowercase_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "AlL");
    assertThat(underTest.decide(loggingEvent)).isEqualTo(DENY);
  }

  @Test
  public void accepts_logs_when_property_is_all_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "all");
    assertThat(underTest.decide(loggingEvent)).isEqualTo(ACCEPT);
  }

  @Test
  public void rejects_logs_when_property_is_ce_only_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "ce_only");
    assertThat(underTest.decide(loggingEvent)).isEqualTo(DENY);
  }

  @Test
  public void accepts_logs_when_property_is_all_in_lowercase_in_MDC() {
    when(loggingEvent.getLevel()).thenReturn(DEBUG);

    MDC.put(MDC_CE_ACTIVITY_FLAG, "all");
    assertThat(underTest.decide(loggingEvent)).isEqualTo(ACCEPT);
  }

}
