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
package org.sonar.server.computation.component;

import java.util.Random;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ComponentFunctions.toKey;
import static org.sonar.server.computation.component.ComponentFunctions.toReportRef;

public class ComponentFunctionsTest {

  public static final int SOME_INT = new Random().nextInt();
  public static final String SOME_KEY = "some key";

  @Test(expected = NullPointerException.class)
  public void toReportRef_throws_NPE_if_Component_is_null() {
    toReportRef().apply(null);
  }

  @Test(expected = IllegalStateException.class)
  public void toReportRef_throws_ISE_if_Component_has_no_ReportAttributes() {
    toReportRef().apply(ViewsComponent.builder(VIEW, 1).build());
  }

  @Test
  public void toReportRef_returns_the_ref_of_the_Component() {
    assertThat(toReportRef().apply(ReportComponent.builder(PROJECT, SOME_INT).build())).isEqualTo(SOME_INT);
  }

  @Test(expected = NullPointerException.class)
  public void toKey_if_Component_is_null() {
    toKey().apply(null);
  }

  @Test
  public void toKey_returns_the_key_of_the_Component() {
    assertThat(toKey().apply(ReportComponent.builder(MODULE, -63).setKey(SOME_KEY).build())).isEqualTo(SOME_KEY);
    assertThat(toKey().apply(ViewsComponent.builder(SUBVIEW, SOME_INT).build())).isEqualTo(String.valueOf(SOME_INT));
  }
}
