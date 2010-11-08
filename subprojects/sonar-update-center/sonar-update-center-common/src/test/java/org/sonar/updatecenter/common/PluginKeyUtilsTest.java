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
package org.sonar.updatecenter.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class PluginKeyUtilsTest {

  @Test
  public void shouldSanitizeMavenArtifactId() {
    assertThat(PluginKeyUtils.sanitize("sonar-test-plugin"), is("test"));
    assertThat(PluginKeyUtils.sanitize("test-sonar-plugin"), is("test"));
    assertThat(PluginKeyUtils.sanitize("test"), is("test"));

    assertThat(PluginKeyUtils.sanitize("sonar-test-foo-plugin"), is("testfoo"));
    assertThat(PluginKeyUtils.sanitize("test-foo-sonar-plugin"), is("testfoo"));
    assertThat(PluginKeyUtils.sanitize("test-foo"), is("testfoo"));
    assertThat(PluginKeyUtils.sanitize("keep.only-digits%12345&and*letters"), is("keeponlydigits12345andletters"));
    assertThat(PluginKeyUtils.sanitize("   remove whitespaces   "), is("removewhitespaces"));
  }

  @Test
  public void shouldBeValid() {
    assertThat(PluginKeyUtils.isValid("foo"), is(true));
    assertThat(PluginKeyUtils.isValid("sonarfooplugin"), is(true));
    assertThat(PluginKeyUtils.isValid("foo6"), is(true));
    assertThat(PluginKeyUtils.isValid("FOO6"), is(true));
  }

  @Test
  public void shouldNotBeValid() {
    assertThat(PluginKeyUtils.isValid(null), is(false));
    assertThat(PluginKeyUtils.isValid(""), is(false));
    assertThat(PluginKeyUtils.isValid("sonar-foo-plugin"), is(false));
    assertThat(PluginKeyUtils.isValid("foo.bar"), is(false));
    assertThat(PluginKeyUtils.isValid("  nowhitespaces   "), is(false));
    assertThat(PluginKeyUtils.isValid("no whitespaces"), is(false));
  }
}
