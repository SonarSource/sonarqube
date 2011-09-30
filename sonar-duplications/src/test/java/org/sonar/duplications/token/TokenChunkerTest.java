/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.duplications.token;

import org.junit.Test;

public class TokenChunkerTest {

  /**
   * In fact this test does not guarantee that we will be able to consume even more great comments,
   * because {@link org.sonar.channel.CodeBuffer} does not expand dynamically - see issue SONAR-2632.
   * But at least guarantees that we able to consume source files from JDK 1.6,
   * because buffer capacity has been increased in comparison with default value,
   * which is {@link org.sonar.channel.CodeReaderConfiguration#DEFAULT_BUFFER_CAPACITY}.
   */
  @Test(timeout = 5000)
  public void shouldConsumeBigComments() {
    int capacity = 80000;
    StringBuilder sb = new StringBuilder(capacity);
    sb.append("/");
    for (int i = 3; i < capacity; i++) {
      sb.append('*');
    }
    sb.append("/");
    TokenChunker chunker = TokenChunker.builder().token("/.*/", "LITERAL").build();
    chunker.chunk(sb.toString());
  }

}
