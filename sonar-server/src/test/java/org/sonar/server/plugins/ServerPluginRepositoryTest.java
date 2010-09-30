/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ServerPluginRepositoryTest {
  @Test
  public void shouldRegisterServerExtensions() {
    ServerPluginRepository repository = new ServerPluginRepository();

    // check classes
    assertThat(repository.shouldRegisterExtension(null, "foo", FakeBatchExtension.class), is(false));
    assertThat(repository.shouldRegisterExtension(null, "foo", FakeServerExtension.class), is(true));
    assertThat(repository.shouldRegisterExtension(null, "foo", String.class), is(false));

    // check objects
    assertThat(repository.shouldRegisterExtension(null, "foo", new FakeBatchExtension()), is(false));
    assertThat(repository.shouldRegisterExtension(null, "foo", new FakeServerExtension()), is(true));
    assertThat(repository.shouldRegisterExtension(null, "foo", "foo"), is(false));
  }

  public static class FakeBatchExtension implements BatchExtension {

  }

  public static class FakeServerExtension implements ServerExtension {

  }
}
