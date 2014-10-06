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
package org.sonar.api.batch.sensor.duplication;


import com.google.common.annotations.Beta;

/**
 * Experimental, do not use.
 * <p/>
 * This builder is used to define token on files. Tokens are later used to compute duplication.
 * Tokens should be declared in sequential order.
 * Example:
 * <code><pre>
 * DuplicationTokenBuilder tokenBuilder = context.duplicationTokenBuilder(inputFile)
 *  .addToken(1, "public")
 *  .addToken(1, "class")
 *  .addToken(1, "Foo")
 *  .addToken(1, "{")
 *  .addToken(2, "}")
 *  .done();
 * </pre></code>
 * @since 4.5
 */
@Beta
public interface DuplicationTokenBuilder {

  /**
   * Call this method to register a new token.
   * @param line Line number of the token. Line starts at 1.
   * @param image Text of the token.
   */
  DuplicationTokenBuilder addToken(int line, String image);

  /**
   * Call this method only once when your are done with defining tokens of the file.
   */
  void done();
}
