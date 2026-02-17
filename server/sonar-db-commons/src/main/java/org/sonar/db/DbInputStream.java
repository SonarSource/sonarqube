/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.commons.io.IOUtils;
import org.sonar.api.internal.apachecommons.io.input.ProxyInputStream;

public class DbInputStream extends ProxyInputStream {
  private final PreparedStatement stmt;
  private final ResultSet rs;
  private final InputStream stream;

  public DbInputStream(PreparedStatement stmt, ResultSet rs, InputStream stream) {
    super(stream);
    this.stmt = stmt;
    this.rs = rs;
    this.stream = stream;
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(stream);
    DatabaseUtils.closeQuietly(rs);
    DatabaseUtils.closeQuietly(stmt);
  }
}
