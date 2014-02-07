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

import org.junit.Test;
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.technicaldebt.server.internal.DefaultCharacteristic;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtManager;

import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class DebtServiceTest {

  DebtFormatter debtFormatter = mock(DebtFormatter.class);
  DefaultTechnicalDebtManager finder = mock(DefaultTechnicalDebtManager.class);
  DebtService service = new DebtService(debtFormatter, finder);

  @Test
  public void format() {
    WorkDayDuration technicalDebt = WorkDayDuration.of(5, 0, 0);
    service.format(technicalDebt);
    verify(debtFormatter).format(any(Locale.class), eq(technicalDebt));
  }

  @Test
  public void to_technical_debt() {
    assertThat(service.toTechnicalDebt("500")).isEqualTo(WorkDayDuration.of(0, 5, 0));
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
