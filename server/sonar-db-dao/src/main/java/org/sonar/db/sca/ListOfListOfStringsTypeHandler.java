/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.sca;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class ListOfListOfStringsTypeHandler extends BaseTypeHandler<List<List<String>>> {
  private static final Gson GSON = new Gson();
  private static final Type type = new TypeToken<List<List<String>>>() {
  }.getType();

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, List<List<String>> parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, GSON.toJson(parameter));
  }

  @Override
  public List<List<String>> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return GSON.fromJson(rs.getString(columnName), type);
  }

  @Override
  public List<List<String>> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return GSON.fromJson(rs.getString(columnIndex), type);
  }

  @Override
  public List<List<String>> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return GSON.fromJson(cs.getString(columnIndex), type);
  }
}
