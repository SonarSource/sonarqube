/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.task.TaskExtension;
import org.sonar.scanner.bootstrap.ExtensionMatcher;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectScanContainerTest {

  @Test
  public void should_add_only_batch_extensions() {
    ExtensionMatcher filter = ProjectScanContainer.getScannerProjectExtensionsFilter();

    assertThat(filter.accept(new MyBatchExtension())).isTrue();
    assertThat(filter.accept(MyBatchExtension.class)).isTrue();

    assertThat(filter.accept(new MyProjectExtension())).isFalse();
    assertThat(filter.accept(MyProjectExtension.class)).isFalse();
    assertThat(filter.accept(new MyServerExtension())).isFalse();
    assertThat(filter.accept(MyServerExtension.class)).isFalse();
    assertThat(filter.accept(new MyTaskExtension())).isFalse();
    assertThat(filter.accept(MyTaskExtension.class)).isFalse();
  }

  @ScannerSide
  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  static class MyBatchExtension  {

  }

  @ScannerSide
  @InstantiationStrategy(InstantiationStrategy.PER_PROJECT)
  static class MyProjectExtension {

  }

  @ServerSide
  static class MyServerExtension  {

  }

  static class MyTaskExtension implements TaskExtension {

  }
}
