/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.mediumtest.log;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Priority;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.scanner.repository.settings.GlobalSettingsLoader;
import org.springframework.beans.factory.UnsatisfiedDependencyException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExceptionHandlingMediumIT {

  private Batch batch;
  private static ErrorGlobalSettingsLoader loader;

  @BeforeClass
  public static void beforeClass() {
    loader = new ErrorGlobalSettingsLoader();
  }

  public void setUp(boolean verbose) {
    Batch.Builder builder = Batch.builder()
      .setEnableLoggingConfiguration(true)
      .addComponents(
        loader,
        new EnvironmentInformation("mediumTest", "1.0"));

    if (verbose) {
      builder.setGlobalProperties(Collections.singletonMap("sonar.verbose", "true"));
    }
    batch = builder.build();
  }

  @Test
  public void test() throws Exception {
    setUp(false);
    loader.withCause = false;

    assertThatThrownBy(() -> batch.execute())
      .isInstanceOf(MessageException.class)
      .hasMessage("Error loading settings");
  }

  @Test
  public void testWithCause() throws Exception {
    setUp(false);
    loader.withCause = true;

    assertThatThrownBy(() -> batch.execute())
      .isInstanceOf(MessageException.class)
      .hasMessage("Error loading settings")
      .hasCauseInstanceOf(Throwable.class)
      .hasRootCauseMessage("Code 401");
  }

  @Test
  public void testWithVerbose() {
    setUp(true);
    assertThatThrownBy(() -> batch.execute())
      .isInstanceOf(UnsatisfiedDependencyException.class)
      .hasMessageContaining("Error loading settings");
  }

  @Priority(1)
  private static class ErrorGlobalSettingsLoader implements GlobalSettingsLoader {
    boolean withCause = false;

    @Override
    public Map<String, String> loadGlobalSettings() {
      if (withCause) {
        IllegalStateException cause = new IllegalStateException("Code 401");
        throw MessageException.of("Error loading settings", cause);
      } else {
        throw MessageException.of("Error loading settings");
      }
    }
  }
}
