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
package org.sonar.ce.task.projectanalysis.util.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.NoSuchElementException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ObjectInputStreamIteratorTest {

  @Test
  public void read_objects() throws Exception {
    ByteArrayOutputStream bytesOutput = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(bytesOutput);
    objectOutputStream.writeObject(new SimpleSerializable("first"));
    objectOutputStream.writeObject(new SimpleSerializable("second"));
    objectOutputStream.writeObject(new SimpleSerializable("third"));
    objectOutputStream.flush();
    objectOutputStream.close();

    ObjectInputStreamIterator<SimpleSerializable> it = new ObjectInputStreamIterator<>(new ByteArrayInputStream(bytesOutput.toByteArray()));
    assertThat(it.next().value).isEqualTo("first");
    assertThat(it.next().value).isEqualTo("second");
    assertThat(it.next().value).isEqualTo("third");
    try {
      it.next();
      fail();
    } catch (NoSuchElementException expected) {

    }
  }

  @Test
  public void test_error() throws Exception {
    ByteArrayOutputStream bytesOutput = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(bytesOutput);
    objectOutputStream.writeObject(new SimpleSerializable("first"));
    objectOutputStream.writeBoolean(false);
    objectOutputStream.flush();
    objectOutputStream.close();

    ObjectInputStreamIterator<SimpleSerializable> it = new ObjectInputStreamIterator<>(new ByteArrayInputStream(bytesOutput.toByteArray()));
    assertThat(it.next().value).isEqualTo("first");
    try {
      it.next();
      fail();
    } catch (RuntimeException expected) {

    }
  }

  static class SimpleSerializable implements Serializable {
    String value;

    public SimpleSerializable() {

    }

    public SimpleSerializable(String value) {
      this.value = value;
    }
  }
}
