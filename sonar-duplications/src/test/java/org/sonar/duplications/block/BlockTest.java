/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.duplications.block;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class BlockTest {

  @Test
  public void fieldsTest() {
    String fileName = "someFile";
    int statementIndex = 4;
    ByteArray hash = new ByteArray(12345);
    Block tuple = new Block(fileName, hash, statementIndex, 0, 10);
    assertThat(tuple.getResourceId(), equalTo(fileName));
    assertThat(tuple.getIndexInFile(), equalTo(statementIndex));
    assertEquals(tuple.getBlockHash(), hash);
  }

  @Test
  public void tupleEqualsTest() {
    Block tuple1 = new Block("somefile", new ByteArray(123), 1, 1, 10);
    Block tuple2 = new Block("somefile", new ByteArray(123), 1, 1, 10);
    Block tupleArr = new Block("somefile", new ByteArray(333), 1, 1, 10);
    Block tupleIndex = new Block("somefile", new ByteArray(123), 2, 1, 10);
    Block tupleName = new Block("other", new ByteArray(123), 1, 1, 10);

    assertTrue(tuple1.equals(tuple2));
    assertThat(tuple1.toString(), is(tuple2.toString()));

    assertFalse(tuple1.equals(tupleArr));
    assertThat(tuple1.toString(), not(equalTo(tupleArr.toString())));

    assertFalse(tuple1.equals(tupleIndex));
    assertThat(tuple1.toString(), not(equalTo(tupleIndex.toString())));

    assertFalse(tuple1.equals(tupleName));
    assertThat(tuple1.toString(), not(equalTo(tupleName.toString())));
  }

  @Test
  public void hashCodeTest() {
    String[] files = {"file1", "file2"};
    int[] unitIndexes = {1, 2};
    ByteArray[] arrays = {new ByteArray(123), new ByteArray(321)};

    // fileName is in hashCode()
    int defaultTupleHashCode = new Block(files[0], arrays[0], unitIndexes[0], 1, 10).hashCode();
    int fileNameTupleHashCode = new Block(files[1], arrays[0], unitIndexes[0], 1, 10).hashCode();
    assertThat(defaultTupleHashCode, not(equalTo(fileNameTupleHashCode)));

    // statementIndex is in hashCode()
    int indexTupleHashCode = new Block(files[0], arrays[0], unitIndexes[1], 1, 10).hashCode();
    assertThat(defaultTupleHashCode, not(equalTo(indexTupleHashCode)));

    // sequenceHash is in hashCode()
    int sequenceHashTupleHashCode = new Block(files[0], arrays[1], unitIndexes[0], 1, 10).hashCode();
    assertThat(defaultTupleHashCode, not(equalTo(sequenceHashTupleHashCode)));
  }
}
