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

public class FileTest {

  @Test
  public void trimKeyAndName() {
    File file = new File("   foo/bar/  ", "  toto.sql  ");
    assertThat(file.getKey(), is("foo/bar/toto.sql"));
    assertThat(file.getLongName(), is("foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is("foo/bar"));
    assertThat(file.getScope(), is(Resource.SCOPE_ENTITY));
    assertThat(file.getQualifier(), is(Resource.QUALIFIER_FILE));
  }

  @Test
  public void parentIsDirectory() {
    File file = new File("   foo/bar/", "toto.sql  ");
    assertThat(file.getKey(), is("foo/bar/toto.sql"));
    assertThat(file.getLongName(), is("foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is("foo/bar"));
    assertThat(ResourceUtils.isSpace(file.getParent()), is(true));
  }

  @Test
  public void rootFilesHaveParent() {
    File file = new File((String) null, "toto.sql");
    assertThat(file.getKey(), is("toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is(Directory.ROOT));

    file = new File("", "toto.sql");
    assertThat(file.getKey(), is("toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is(Directory.ROOT));

    file = new File("toto.sql");
    assertThat(file.getKey(), is("toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is(Directory.ROOT));
  }

  @Test
  public void newFileByKey() {
    File file = new File("toto.sql");
    assertThat(file.getKey(), is("toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getLongName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is(Directory.ROOT));
    assertThat(file.getScope(), is(Resource.SCOPE_ENTITY));
    assertThat(file.getQualifier(), is(Resource.QUALIFIER_FILE));

    file = new File("foo/bar/toto.sql");
    assertThat(file.getKey(), is("foo/bar/toto.sql"));
    assertThat(file.getLongName(), is("foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is("foo/bar"));

    file = new File("/foo/bar/toto.sql ");
    assertThat(file.getKey(), is("foo/bar/toto.sql"));
    assertThat(file.getLongName(), is("foo/bar/toto.sql"));
    assertThat(file.getName(), is("toto.sql"));
    assertThat(file.getParent().getKey(), is("foo/bar"));
  }

  @Test
  public void setLanguage() {
    File file = new File(Java.INSTANCE, "Foo.java");
    assertThat(file.getLanguage(), is((Language) Java.INSTANCE));

    file = new File(Java.INSTANCE, "org/sonar", "Foo.java");
    assertThat(file.getLanguage(), is((Language) Java.INSTANCE));
    assertThat(file.getParent().getLanguage(), nullValue());
  }

  @Test
  public void matchAntPatterns() {
    assertThat(new File("one/two/foo.sql").matchFilePattern("one/two/*.java"), is(false));
    assertThat(new File("one/two/foo.sql").matchFilePattern("false"), is(false));
    assertThat(new File("one/two/foo.sql").matchFilePattern("two/one/**"), is(false));
    assertThat(new File("one/two/foo.sql").matchFilePattern("other*/**"), is(false));

    assertThat(new File("one/two/foo.sql").matchFilePattern("one*/**/*.sql"), is(true));
    assertThat(new File("one/two/foo.sql").matchFilePattern("one/t?o/**/*"), is(true));
    assertThat(new File("one/two/foo.sql").matchFilePattern("**/*"), is(true));
    assertThat(new File("one/two/foo.sql").matchFilePattern("one/two/*"), is(true));
    assertThat(new File("one/two/foo.sql").matchFilePattern("/one/two/*"), is(true));
    assertThat(new File("one/two/foo.sql").matchFilePattern("one/**"), is(true));
  }
}
