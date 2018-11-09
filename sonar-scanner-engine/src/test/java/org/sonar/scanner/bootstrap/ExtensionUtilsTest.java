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
package org.sonar.scanner.bootstrap;

import org.junit.Test;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtensionUtilsTest {

  @Test
  public void shouldBeBatchInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultScannerService.class, InstantiationStrategy.PER_BATCH)).isFalse();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultScannerService(), InstantiationStrategy.PER_BATCH)).isFalse();

  }

  @Test
  public void shouldBeProjectInstantiationStrategy() {
    assertThat(ExtensionUtils.isInstantiationStrategy(ProjectService.class, InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new ProjectService(), InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultService.class, InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultService(), InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(DefaultScannerService.class, InstantiationStrategy.PER_PROJECT)).isTrue();
    assertThat(ExtensionUtils.isInstantiationStrategy(new DefaultScannerService(), InstantiationStrategy.PER_PROJECT)).isTrue();

  }

  @Test
  public void testIsScannerSide() {
    assertThat(ExtensionUtils.isDeprecatedScannerSide(ScannerService.class)).isTrue();

    assertThat(ExtensionUtils.isDeprecatedScannerSide(ServerService.class)).isFalse();
    assertThat(ExtensionUtils.isDeprecatedScannerSide(new ServerService())).isFalse();
    assertThat(ExtensionUtils.isDeprecatedScannerSide(new WebServerService())).isFalse();
    assertThat(ExtensionUtils.isDeprecatedScannerSide(new ComputeEngineService())).isFalse();
  }

  @ScannerSide
  @InstantiationStrategy(InstantiationStrategy.PER_BATCH)
  public static class ScannerService {

  }

  @ScannerSide
  @InstantiationStrategy(InstantiationStrategy.PER_PROJECT)
  public static class ProjectService {

  }

  @ScannerSide
  public static class DefaultService {

  }
  
  @ScannerSide
  public static class DefaultScannerService {

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
