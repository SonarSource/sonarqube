/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.measure;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.computation.task.projectanalysis.api.measurecomputer.MeasureComputerWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class MeasureComputersHolderImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private MeasureComputersHolderImpl underTest = new MeasureComputersHolderImpl();

  @Test
  public void get_measure_computers() {
    MeasureComputerWrapper measureComputer = mock(MeasureComputerWrapper.class);
    underTest.setMeasureComputers(Collections.singletonList(measureComputer));

    assertThat(underTest.getMeasureComputers()).containsOnly(measureComputer);
  }

  @Test
  public void get_measure_computers_throws_ISE_if_not_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Measure computers have not been initialized yet");

    underTest.getMeasureComputers();
  }

  @Test
  public void set_measure_computers_supports_empty_arg_is_empty() {
    underTest.setMeasureComputers(ImmutableList.of());

    assertThat(underTest.getMeasureComputers()).isEmpty();
  }

  @Test
  public void set_measure_computers_throws_ISE_if_already_initialized() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Measure computers have already been initialized");

    MeasureComputerWrapper measureComputer = mock(MeasureComputerWrapper.class);
    underTest.setMeasureComputers(Collections.singletonList(measureComputer));
    underTest.setMeasureComputers(Collections.singletonList(measureComputer));
  }

}
