/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.computation.queue;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskTest {

  @Test
  public void build() {
    CeTask.Builder builder = new CeTask.Builder();
    builder.setType("TYPE_1");
    builder.setUuid("UUID_1");
    builder.setSubmitterLogin("LOGIN_1");
    builder.setComponentKey("COMPONENT_KEY_1");
    builder.setComponentUuid("COMPONENT_UUID_1");
    builder.setComponentName("The component");
    CeTask task = builder.build();

    assertThat(task.getType()).isEqualTo("TYPE_1");
    assertThat(task.getUuid()).isEqualTo("UUID_1");
    assertThat(task.getSubmitterLogin()).isEqualTo("LOGIN_1");
    assertThat(task.getComponentKey()).isEqualTo("COMPONENT_KEY_1");
    assertThat(task.getComponentUuid()).isEqualTo("COMPONENT_UUID_1");
    assertThat(task.getComponentName()).isEqualTo("The component");
  }

  @Test
  public void equals_and_hashCode_on_uuid() {
    CeTask.Builder builder1 = new CeTask.Builder().setType("TYPE_1").setUuid("UUID_1");
    CeTask task1 = builder1.build();
    CeTask task1bis = builder1.build();
    CeTask task2 = new CeTask.Builder().setType("TYPE_1").setUuid("UUID_2").build();

    assertThat(task1.equals(task1)).isTrue();
    assertThat(task1.equals(task1bis)).isTrue();
    assertThat(task1.equals(task2)).isFalse();
    assertThat(task1.hashCode()).isEqualTo(task1.hashCode());
    assertThat(task1.hashCode()).isEqualTo(task1bis.hashCode());
  }
}
