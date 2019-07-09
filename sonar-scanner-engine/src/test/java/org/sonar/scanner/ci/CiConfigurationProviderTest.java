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
package org.sonar.scanner.ci;

import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CiConfigurationProviderTest {

  private MapSettings cli = new MapSettings();
  private CiConfigurationProvider underTest = new CiConfigurationProvider();

  @Test
  public void empty_configuration_if_no_ci_vendors() {
    CiConfiguration CiConfiguration = underTest.provide(cli.asConfig(), new CiVendor[0]);

    assertThat(CiConfiguration.getScmRevision()).isEmpty();
  }

  @Test
  public void empty_configuration_if_no_ci_detected() {
    CiConfiguration ciConfiguration = underTest.provide(cli.asConfig(), new CiVendor[] {new DisabledCiVendor("vendor1"), new DisabledCiVendor("vendor2")});

    assertThat(ciConfiguration.getScmRevision()).isEmpty();
  }

  @Test
  public void configuration_defined_by_ci_vendor() {
    CiConfiguration ciConfiguration = underTest.provide(cli.asConfig(), new CiVendor[] {new DisabledCiVendor("vendor1"), new EnabledCiVendor("vendor2")});

    assertThat(ciConfiguration.getScmRevision()).hasValue(EnabledCiVendor.SHA);
  }

  @Test
  public void fail_if_multiple_ci_vendor_are_detected() {
    Throwable thrown = catchThrowable(() -> underTest.provide(cli.asConfig(), new CiVendor[] {new EnabledCiVendor("vendor1"), new EnabledCiVendor("vendor2")}));

    assertThat(thrown)
      .isInstanceOf(MessageException.class)
      .hasMessage("Multiple CI environments are detected: [vendor1, vendor2]. Please check environment variables or set property sonar.ci.autoconfig.disabled to true.");
  }

  @Test
  public void empty_configuration_if_auto_configuration_is_disabled() {
    cli.setProperty("sonar.ci.autoconfig.disabled", true);
    CiConfiguration ciConfiguration = underTest.provide(cli.asConfig(), new CiVendor[] {new EnabledCiVendor("vendor1")});

    assertThat(ciConfiguration.getScmRevision()).isEmpty();
  }

  private static class DisabledCiVendor implements CiVendor {
    private final String name;

    private DisabledCiVendor(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isDetected() {
      return false;
    }

    @Override
    public CiConfiguration loadConfiguration() {
      throw new IllegalStateException("should not be called");
    }
  }

  private static class EnabledCiVendor implements CiVendor {
    private static final String SHA = "abc12df";
    private final String name;

    private EnabledCiVendor(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isDetected() {
      return true;
    }

    @Override
    public CiConfiguration loadConfiguration() {
      return new CiConfigurationImpl(SHA);
    }
  }
}
