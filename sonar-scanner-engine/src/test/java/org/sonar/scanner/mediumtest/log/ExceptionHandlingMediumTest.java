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
package org.sonar.scanner.mediumtest.log;

import java.util.Collections;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrapper.Batch;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.scanner.repository.settings.GlobalSettingsLoader;

public class ExceptionHandlingMediumTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

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
      builder.setBootstrapProperties(Collections.singletonMap("sonar.verbose", "true"));
    }
    batch = builder.build();
  }

  @Test
  public void test() throws Exception {
    setUp(false);
    loader.withCause = false;
    thrown.expect(MessageException.class);
    thrown.expectMessage("Error loading settings");
    thrown.expectCause(CoreMatchers.nullValue(Throwable.class));

    batch.execute();
  }

  @Test
  public void testWithCause() throws Exception {
    setUp(false);
    loader.withCause = true;

    thrown.expect(MessageException.class);
    thrown.expectMessage("Error loading settings");
    thrown.expectCause(new TypeSafeMatcher<Throwable>() {
      @Override
      public void describeTo(Description description) {
      }

      @Override
      protected boolean matchesSafely(Throwable item) {
        return item instanceof IllegalStateException && item.getMessage().equals("Code 401");
      }
    });

    batch.execute();
  }

  @Test
  public void testWithVerbose() throws Exception {
    setUp(true);
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to load component class");
    batch.execute();
  }

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
