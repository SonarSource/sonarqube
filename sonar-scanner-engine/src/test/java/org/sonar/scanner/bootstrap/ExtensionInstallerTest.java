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

import java.util.Arrays;
import org.apache.commons.lang.ClassUtils;
import org.junit.Test;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.PluginInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtensionInstallerTest {

  private MapSettings settings = new MapSettings();
  private ScannerPluginRepository pluginRepository = mock(ScannerPluginRepository.class);

  private static Plugin newPluginInstance(final Object... extensions) {
    return desc -> desc.addExtensions(Arrays.asList(extensions));
  }

  @Test
  public void should_filter_extensions_to_install() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("foo")));
    when(pluginRepository.getPluginInstance("foo")).thenReturn(newPluginInstance(Foo.class, Bar.class));

    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(mock(SonarRuntime.class), pluginRepository, settings.asConfig());
    installer.install(container, new FooMatcher());

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNull();
  }

  @Test
  public void should_execute_extension_provider() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("foo")));
    when(pluginRepository.getPluginInstance("foo")).thenReturn(newPluginInstance(new FooProvider(), new BarProvider()));
    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(mock(SonarRuntime.class), pluginRepository, settings.asConfig());

    installer.install(container, new FooMatcher());

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNull();
  }

  @Test
  public void should_provide_list_of_extensions() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("foo")));
    when(pluginRepository.getPluginInstance("foo")).thenReturn(newPluginInstance(new FooBarProvider()));
    ComponentContainer container = new ComponentContainer();
    ExtensionInstaller installer = new ExtensionInstaller(mock(SonarRuntime.class), pluginRepository, settings.asConfig());

    installer.install(container, new TrueMatcher());

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNotNull();
  }

  private static class FooMatcher implements ExtensionMatcher {
    public boolean accept(Object extension) {
      return extension.equals(Foo.class) || ClassUtils.isAssignable(Foo.class, extension.getClass()) || ClassUtils.isAssignable(FooProvider.class, extension.getClass());
    }
  }

  private static class TrueMatcher implements ExtensionMatcher {
    public boolean accept(Object extension) {
      return true;
    }
  }

  @ScannerSide
  public static class Foo {

  }

  @ScannerSide
  public static class Bar {

  }

  @ScannerSide
  public static class FooProvider extends ExtensionProvider {
    @Override
    public Object provide() {
      return new Foo();
    }
  }

  @ScannerSide
  public static class BarProvider extends ExtensionProvider {
    @Override
    public Object provide() {
      return new Bar();
    }
  }

  @ScannerSide
  public static class FooBarProvider extends ExtensionProvider {
    @Override
    public Object provide() {
      return Arrays.asList(new Foo(), new Bar());
    }
  }

}
