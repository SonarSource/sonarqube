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
package org.sonar.server.computation.task.projectanalysis.duplication;

import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicateTest {
  @Test
  public void duplicate_implementations_are_not_equals_to_each_other_even_if_TextBlock_is_the_same() {
    TextBlock textBlock = new TextBlock(1, 2);

    InnerDuplicate innerDuplicate = new InnerDuplicate(textBlock);
    InProjectDuplicate inProjectDuplicate = new InProjectDuplicate(ReportComponent.builder(Component.Type.FILE, 1).build(), textBlock);
    CrossProjectDuplicate crossProjectDuplicate = new CrossProjectDuplicate("file key", textBlock);

    assertThat(innerDuplicate.equals(inProjectDuplicate)).isFalse();
    assertThat(innerDuplicate.equals(crossProjectDuplicate)).isFalse();
    assertThat(inProjectDuplicate.equals(crossProjectDuplicate)).isFalse();
  }
}
