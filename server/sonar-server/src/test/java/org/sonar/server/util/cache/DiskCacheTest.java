/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.util.cache;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;

import java.io.ObjectOutputStream;
import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class DiskCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void write_and_read() throws Exception {
    DiskCache<String> cache = new DiskCache<>(temp.newFile(), System2.INSTANCE);
    try (CloseableIterator<String> traverse = cache.traverse()) {
      assertThat(traverse).isEmpty();
    }

    cache.newAppender()
      .append("foo")
      .append("bar")
      .close();
    try (CloseableIterator<String> traverse = cache.traverse()) {
      assertThat(traverse).containsExactly("foo", "bar");
    }
  }

  @Test
  public void fail_if_file_is_not_writable() throws Exception {
    try {
      new DiskCache<>(temp.newFolder(), System2.INSTANCE);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("Fail to write into file");
    }
  }

  @Test
  public void fail_to_serialize() throws Exception {
    class Unserializable implements Serializable {
      private void writeObject(ObjectOutputStream out) {
        throw new UnsupportedOperationException("expected error");
      }
    }
    DiskCache<Serializable> cache = new DiskCache<>(temp.newFile(), System2.INSTANCE);
    try {
      cache.newAppender().append(new Unserializable());
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("expected error");
    }
  }
}
