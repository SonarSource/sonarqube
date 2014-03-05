/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.technicaldebt;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.technicaldebt.server.internal.DefaultCharacteristic;
import org.sonar.api.utils.internal.WorkDuration;
import org.sonar.api.utils.internal.WorkDurationFactory;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtManager;

import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class DebtServiceTest {

  private static final int HOURS_IN_DAY = 8;
  DebtFormatter debtFormatter = mock(DebtFormatter.class);
  DefaultTechnicalDebtManager finder = mock(DefaultTechnicalDebtManager.class);

  DebtService service;

  @Before
  public void setUp() throws Exception {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.HOURS_IN_DAY, HOURS_IN_DAY);
    service = new DebtService(debtFormatter, finder, new WorkDurationFactory(settings));
  }

  @Test
  public void format() {
    WorkDuration technicalDebt = WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.MINUTES, HOURS_IN_DAY);
    service.format(technicalDebt);
    verify(debtFormatter).format(any(Locale.class), eq(technicalDebt));
  }

  @Test
  public void to_technical_debt() {
    assertThat(service.toTechnicalDebt("500")).isEqualTo(WorkDuration.createFromValueAndUnit(5, WorkDuration.UNIT.HOURS, HOURS_IN_DAY));
  }

  @Test
  public void find_root_characteristics() {
    List<Characteristic> rootCharacteristics = newArrayList();
    when(finder.findRootCharacteristics()).thenReturn(rootCharacteristics);
    assertThat(service.findRootCharacteristics()).isEqualTo(rootCharacteristics);
  }

  @Test
  public void find_requirement_by_rule_id() {
    service.findRequirementByRuleId(1);
    verify(finder).findRequirementByRuleId(1);
  }

  @Test
  public void find_characteristic() {
    Characteristic characteristic = new DefaultCharacteristic();
    when(finder.findCharacteristicById(1)).thenReturn(characteristic);
    assertThat(service.findCharacteristic(1)).isEqualTo(characteristic);
  }

}
