/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class DirectoryTest {

  @Test
  public void shouldNotStartOrEndBySlash() {
    Resource dir = new Directory("      /foo/bar/  ");
    assertThat(dir.getKey(), is("foo/bar"));
    assertThat(dir.getName(), is("foo/bar"));
  }

  @Test
  public void rootDirectory() {
    assertThat(new Directory(null).getKey(), is(Directory.ROOT));
    assertThat(new Directory("").getKey(), is(Directory.ROOT));
    assertThat(new Directory("   ").getKey(), is(Directory.ROOT));
  }

  @Test
  public void backSlashesShouldBeReplacedBySlashes() {
    Resource dir = new Directory("  foo\\bar\\     ");
    assertThat(dir.getKey(), is("foo/bar"));
    assertThat(dir.getName(), is("foo/bar"));
  }

  @Test
  public void directoryHasNoParents() {
    Resource dir = new Directory("foo/bar");
    assertThat(dir.getParent(), nullValue());
  }

  @Test
  public void shouldHaveOnlyOneLevelOfDirectory() {
    assertThat(new Directory("one/two/third").getParent(), nullValue());
    assertThat(new Directory("one").getParent(), nullValue());
  }

  @Test
  public void parseDirectoryKey() {
    assertThat(Directory.parseKey("/foo/bar"), is("foo/bar"));
  }

  @Test
  public void matchExclusionPatterns() {
    assertThat(new Directory("one/two/third").matchFilePattern("one/two/*.java"), is(false));
    assertThat(new Directory("one/two/third").matchFilePattern("false"), is(false));
    assertThat(new Directory("one/two/third").matchFilePattern("two/one/**"), is(false));
    assertThat(new Directory("one/two/third").matchFilePattern("other*/**"), is(false));

    assertThat(new Directory("one/two/third").matchFilePattern("one*/**"), is(true));
    assertThat(new Directory("one/two/third").matchFilePattern("one/t?o/**"), is(true));
    assertThat(new Directory("one/two/third").matchFilePattern("**/*"), is(true));
    assertThat(new Directory("one/two/third").matchFilePattern("**"), is(true));
    assertThat(new Directory("one/two/third").matchFilePattern("one/two/*"), is(true));
    assertThat(new Directory("one/two/third").matchFilePattern("/one/two/*"), is(true));
    assertThat(new Directory("one/two/third").matchFilePattern("one/**"), is(true));
  }
}
