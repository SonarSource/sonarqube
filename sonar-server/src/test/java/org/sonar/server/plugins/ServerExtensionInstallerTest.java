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
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.ServerExtension;
import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class ServerExtensionInstallerTest {
  @Test
  public void shouldRegisterServerExtensions() {
//    ExtensionRegistrar repository = new ExtensionRegistrar(mock(PluginRepository.class));
//
//    ComponentContainer container = new ComponentContainer();
//    repository.registerExtensions(container, Arrays.<Plugin>asList(new FakePlugin(Arrays.<Class>asList(FakeBatchExtension.class, FakeServerExtension.class))));
//
//    assertThat(container.getComponentsByType(Extension.class).size(), is(1));
//    assertThat(container.getComponentsByType(FakeServerExtension.class).size(), is(1));
//    assertThat(container.getComponentsByType(FakeBatchExtension.class).size(), is(0));
  }

  @Test
  public void shouldInvokeServerExtensionProviders() {
//    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(mock(PluginDeployer.class));
//
//    ComponentContainer container = new ComponentContainer();
//    repository.registerExtensions(container, Arrays.<Plugin>asList(new FakePlugin(Arrays.<Class>asList(FakeExtensionProvider.class))));
//
//    assertThat(container.getComponentsByType(Extension.class).size(), is(2));// provider + FakeServerExtension
//    assertThat(container.getComponentsByType(FakeServerExtension.class).size(), is(1));
//    assertThat(container.getComponentsByType(FakeBatchExtension.class).size(), is(0));
  }

  @Test
  public void shouldNotSupportProvidersOfProviders() {
//    DefaultServerPluginRepository repository = new DefaultServerPluginRepository(mock(PluginDeployer.class));
//
//    ComponentContainer container = new ComponentContainer();
//    repository.registerExtensions(container, Arrays.<Plugin>asList(new FakePlugin(Arrays.<Class>asList(SuperExtensionProvider.class))));
//
//    assertThat(container.getComponentsByType(FakeBatchExtension.class).size(), is(0));
//    assertThat(container.getComponentsByType(FakeServerExtension.class).size(), is(0));
  }

  public static class FakePlugin extends SonarPlugin {
    private List<Class> extensions;

    public FakePlugin(List<Class> extensions) {
      this.extensions = extensions;
    }

    public List<Class> getExtensions() {
      return extensions;
    }
  }

  public static class FakeBatchExtension implements BatchExtension {

  }

  public static class FakeServerExtension implements ServerExtension {

  }

  public static class FakeExtensionProvider extends ExtensionProvider implements ServerExtension {

    @Override
    public Object provide() {
      return Arrays.<Object> asList(FakeBatchExtension.class, FakeServerExtension.class);
    }
  }

  public static class SuperExtensionProvider extends ExtensionProvider implements ServerExtension {

    @Override
    public Object provide() {
      return FakeExtensionProvider.class;
    }
  }
}
