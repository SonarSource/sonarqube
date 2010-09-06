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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ArtifactTest {

  @Test
  public void compare() {
    Artifact a = new FakeArtifact("a");
    Artifact b = new FakeArtifact("b");
    Artifact c = new Plugin("c");

    List<Artifact> list = Arrays.asList(b, a, c);
    Collections.sort(list);
    assertThat(list.get(0), is(a));
    assertThat(list.get(1), is(b));
    assertThat(list.get(2), is(c));
  }

  @Test
  public void sortReleases() {
    FakeArtifact artifact = new FakeArtifact("fake");
    artifact.addRelease(Version.create("2.0"));
    artifact.addRelease(Version.create("1.1"));
    artifact.addRelease(Version.create("1.5"));

    Iterator<Release> it = artifact.getReleases().iterator();
    assertThat(it.next().getVersion().getName(), is("1.1"));
    assertThat(it.next().getVersion().getName(), is("1.5"));
    assertThat(it.next().getVersion().getName(), is("2.0"));
  }

  @Test
  public void equals() {
    FakeArtifact foo = new FakeArtifact("foo");
    assertTrue(foo.equals(new FakeArtifact("foo")));
    assertTrue(foo.equals(foo));
    assertFalse(foo.equals(new FakeArtifact("bar")));
  }
}

class FakeArtifact extends Artifact {

  protected FakeArtifact(String key) {
    super(key);
  }
}