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

package org.sonar.server.util.cache;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.server.util.CloseableIterator;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DiskCacheByIdTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void write_and_read() throws Exception {
    DiskCacheById<String> cache = new DiskCacheById<>(temp.newFolder(), System2.INSTANCE);
    int ref = 10;
    cache.newAppender(ref)
      .append("foo")
      .append("bar")
      .close();
    try (CloseableIterator<String> traverse = cache.traverse(ref)) {
      assertThat(traverse).containsExactly("foo", "bar");
    }
  }

  @Test
  public void return_empty_on_unwritten_element() throws Exception {
    DiskCacheById<String> cache = new DiskCacheById<>(temp.newFolder(), System2.INSTANCE);
    try (CloseableIterator<String> traverse = cache.traverse(10)) {
      assertThat(traverse).isEmpty();
    }
  }

  @Test
  public void fail_if_directory_is_not_writable() throws Exception {
    try {
      DiskCacheById<String> cache = new DiskCacheById<>(temp.newFile(), System2.INSTANCE);
      cache.newAppender(10)
        .append("foo")
        .close();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("Fail to open file");
    }
  }

  @Test
  public void fail_to_serialize() throws Exception {
    class Unserializable implements Serializable {
      private void writeObject(ObjectOutputStream out) throws IOException {
        throw new UnsupportedOperationException("expected error");
      }
    }
    DiskCacheById<Serializable> cache = new DiskCacheById<>(temp.newFolder(), System2.INSTANCE);
    try {
      cache.newAppender(1).append(new Unserializable());
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("expected error");
    }
  }

}
