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

import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Test;

import static ch.qos.logback.core.spi.FilterReply.ACCEPT;
import static ch.qos.logback.core.spi.FilterReply.DENY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.log.CeLogging.MDC_CE_ACTIVITY_FLAG;

public class CeActivityLogAcceptFilterTest {
  private static final Object UNUSED = "";

  private CeActivityLogAcceptFilter underTest = new CeActivityLogAcceptFilter();

  @After
  public void tearDown() {
    MDC.clear();
  }

  @Test
  public void rejects_logs_when_property_is_not_set_in_MDC() {
    assertThat(underTest.decide(UNUSED)).isEqualTo(DENY);
  }

  @Test
  public void accepts_logs_when_property_is_neither_ce_only_nor_all_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "bla");
    assertThat(underTest.decide(UNUSED)).isEqualTo(ACCEPT);
  }

  @Test
  public void accepts_logs_when_property_is_ce_only_not_in_lowercase_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "Ce_ONly");
    assertThat(underTest.decide(UNUSED)).isEqualTo(ACCEPT);
  }

  @Test
  public void accepts_logs_when_property_is_ce_only_in_lowercase_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "ce_only");
    assertThat(underTest.decide(UNUSED)).isEqualTo(ACCEPT);
  }

  @Test
  public void accepts_logs_when_property_is_all_in_lowercase_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "all");
    assertThat(underTest.decide(UNUSED)).isEqualTo(ACCEPT);
  }

  @Test
  public void accepts_logs_when_property_is_all_not_in_lowercase_in_MDC() {
    MDC.put(MDC_CE_ACTIVITY_FLAG, "aLl");
    assertThat(underTest.decide(UNUSED)).isEqualTo(ACCEPT);
  }

}
