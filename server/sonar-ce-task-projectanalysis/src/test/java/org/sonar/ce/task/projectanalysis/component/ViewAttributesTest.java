/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Test;
import org.sonar.db.component.ComponentQualifiers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.ce.task.projectanalysis.component.ViewAttributes.Type.APPLICATION;
import static org.sonar.ce.task.projectanalysis.component.ViewAttributes.Type.PORTFOLIO;

public class ViewAttributesTest {

  private ViewAttributes underTest;

  @Test
  public void create_portfolio() {
    underTest = new ViewAttributes(PORTFOLIO);

    assertThat(underTest.getType()).isEqualTo(PORTFOLIO);
    assertThat(underTest.getType().getQualifier()).isEqualTo(ComponentQualifiers.VIEW);
  }

  @Test
  public void create_application() {
    underTest = new ViewAttributes(APPLICATION);

    assertThat(underTest.getType()).isEqualTo(APPLICATION);
    assertThat(underTest.getType().getQualifier()).isEqualTo(ComponentQualifiers.APP);
  }

  @Test
  public void type_from_qualifier() {
    assertThat(ViewAttributes.Type.fromQualifier(ComponentQualifiers.VIEW)).isEqualTo(PORTFOLIO);
    assertThat(ViewAttributes.Type.fromQualifier(ComponentQualifiers.APP)).isEqualTo(APPLICATION);
  }

  @Test
  public void fail_if_unknown_view_qualifier() {
    assertThatThrownBy(() -> ViewAttributes.Type.fromQualifier(ComponentQualifiers.PROJECT))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Qualifier 'TRK' is not supported");
  }
}
