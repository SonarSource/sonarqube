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
package org.sonar.server.ruby;

import com.google.common.collect.ImmutableList;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlatformRubyBridgeTest {
  private static ScriptingContainer container = setupScriptingContainer();

  private RackBridge rackBridge = mock(RackBridge.class);
  private PlatformRubyBridge underTest;

  @Before
  public void setUp() throws Exception {
    when(rackBridge.getRubyRuntime()).thenReturn(container.getProvider().getRuntime());
    underTest = new PlatformRubyBridge(rackBridge);
  }

  /**
   * Creates a Ruby runtime which loading path includes the test resource directory where our Ruby test DatabaseVersion
   * is defined.
   */
  private static ScriptingContainer setupScriptingContainer() {
    try {
      ScriptingContainer container = new ScriptingContainer(LocalContextScope.CONCURRENT);
      URL resource = PlatformRubyBridge.class.getResource("database_version.rb");
      String dirPath = new File(resource.toURI()).getParentFile().getPath();
      container.setLoadPaths(ImmutableList.of(dirPath));

      return container;
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * unit test only makes sure the wrapping and method forwarding provided by JRuby works so building the
   * RubyDatabaseMigration object and calling its trigger method is enough as it would otherwise raise an exception
   */
  @Test
  public void testDatabaseMigration() {
    try {
      underTest.databaseMigration().trigger();
    } catch (RaiseException e) {
      throw new RuntimeException("Loading error with container loadPath " + container.getLoadPaths(), e);
    }
  }

}
