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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtensionUtilsTest {

  @Test
  public void shouldBeBatchInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.PER_BATCH)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.PER_BATCH)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_BATCH)).isFalse();
  }

  @Test
  public void shouldBeProjectInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(BatchService.class, InstantiationStrategy.PER_PROJECT)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new BatchService(), InstantiationStrategy.PER_PROJECT)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_PROJECT)).isTrue();
  }

  @Test
  public void testIsBatchSide() {
    assertThat(ExtensionUtils.isBatchSide(BatchService.class)).isTrue();
    assertThat(ExtensionUtils.isBatchSide(new BatchService())).isTrue();
    assertThat(ExtensionUtils.isBatchSide(DeprecatedBatchService.class)).isTrue();

    assertThat(ExtensionUtils.isBatchSide(ServerService.class)).isFalse();
    assertThat(ExtensionUtils.isBatchSide(new ServerService())).isFalse();
    assertThat(ExtensionUtils.isBatchSide(new WebServerService())).isFalse();
    assertThat(ExtensionUtils.isBatchSide(new ComputeEngineService())).isFalse();
  }

  @BatchSide
  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class BatchService {

  }

  public static class DeprecatedBatchService implements BatchComponent {

  }

  @BatchSide
  @InstantiationStrategy(InstantiationStrategy.PER_PROJECT)
  public static class ProjectService {

  }

  @BatchSide
  public static class DefaultService {

  }

  @ServerSide
  public static class ServerService {

  }

  @ServerSide
  public static class WebServerService {

  }

  @ComputeEngineSide
  public static class ComputeEngineService {

  }

}
