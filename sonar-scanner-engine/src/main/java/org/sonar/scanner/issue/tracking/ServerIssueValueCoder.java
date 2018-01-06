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
package org.sonar.scanner.issue.tracking;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;
import java.io.IOException;
import org.sonar.scanner.protocol.input.ScannerInput.ServerIssue;

public class ServerIssueValueCoder implements ValueCoder {

  @Override
  public void put(Value value, Object object, CoderContext context) {
    ServerIssue issue = (ServerIssue) object;
    value.putByteArray(issue.toByteArray());
  }

  @Override
  public Object get(Value value, Class<?> clazz, CoderContext context) {
    try {
      return ServerIssue.parseFrom(value.getByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read issue from cache", e);
    }
  }

}
