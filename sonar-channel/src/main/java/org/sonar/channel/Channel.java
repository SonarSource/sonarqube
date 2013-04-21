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
package org.sonar.channel;

public abstract class Channel<O> {

  /**
   * Tries to consume the character stream at the current reading cursor position (provided by the {@link org.sonar.channel.CodeReader}). If
   * the character stream is consumed the method must return true and the OUTPUT object can be fed.
   * 
   * @param code
   *          the handle on the input character stream
   * @param output
   *          the OUTPUT that can be optionally fed by the Channel
   * @return false if the Channel doesn't want to consume the character stream, true otherwise.
   */
  public abstract boolean consume(CodeReader code, O output);
}
