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
package org.sonar.scanner.scan;

import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.task.TaskExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectScanContainerTest {

  @Test
  public void should_add_only_batch_extensions() {
    ProjectScanContainer.BatchExtensionFilter filter = new ProjectScanContainer.BatchExtensionFilter();

    assertThat(filter.accept(new MyBatchExtension())).isTrue();
    assertThat(filter.accept(MyBatchExtension.class)).isTrue();

    assertThat(filter.accept(new MyProjectExtension())).isFalse();
    assertThat(filter.accept(MyProjectExtension.class)).isFalse();
    assertThat(filter.accept(new MyServerExtension())).isFalse();
    assertThat(filter.accept(MyServerExtension.class)).isFalse();
    assertThat(filter.accept(new MyTaskExtension())).isFalse();
    assertThat(filter.accept(MyTaskExtension.class)).isFalse();
  }

  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  static class MyBatchExtension implements BatchExtension {

  }

  static class MyProjectExtension implements BatchExtension {

  }

  static class MyServerExtension implements ServerExtension {

  }

  static class MyTaskExtension implements TaskExtension {

  }
}
