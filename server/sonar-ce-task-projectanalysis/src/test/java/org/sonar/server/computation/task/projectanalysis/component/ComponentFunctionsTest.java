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
package org.sonar.server.computation.task.projectanalysis.component;

import java.util.Random;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.ComponentFunctions.toReportRef;

public class ComponentFunctionsTest {

  private static final int SOME_INT = new Random().nextInt();

  @Test(expected = IllegalStateException.class)
  public void toReportRef_throws_ISE_if_Component_has_no_ReportAttributes() {
    toReportRef().apply(ViewsComponent.builder(VIEW, 1).build());
  }

  @Test
  public void toReportRef_returns_the_ref_of_the_Component() {
    assertThat(toReportRef().apply(ReportComponent.builder(PROJECT, SOME_INT).build())).isEqualTo(SOME_INT);
  }
}
