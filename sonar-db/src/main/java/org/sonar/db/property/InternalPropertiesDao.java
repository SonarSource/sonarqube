/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.property;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;

public class InternalPropertiesDao implements Dao {

  private static final int TEXT_VALUE_MAX_LENGTH = 4000;
  private static final Optional<String> OPTIONAL_OF_EMPTY_STRING = Optional.of("");

  private final System2 system2;

  public InternalPropertiesDao(System2 system2) {
    this.system2 = system2;
  }

  /**
   * Save a property which value is not empty.
   * <p>Value can't be {@code null} but can have any size except 0.</p>
   * 
   * @throws IllegalArgumentException if {@code key} or {@code value} is {@code null} or empty.
   * 
   * @see #saveAsEmpty(DbSession, String)
   */
  public void save(DbSession dbSession, String key, String value) {
    checkKey(key);
    checkArgument(value != null && !value.isEmpty(), "value can't be null nor empty");

    InternalPropertiesMapper mapper = getMapper(dbSession);
    mapper.deleteByKey(key);
    long now = system2.now();
    if (mustsBeStoredInClob(value)) {
      mapper.insertAsClob(key, value, now);
    } else {
      mapper.insertAsText(key, value, now);
    }
  }

  private static boolean mustsBeStoredInClob(String value) {
    return value.length() > TEXT_VALUE_MAX_LENGTH;
  }

  /**
   * Save a property which value is empty.
   */
  public void saveAsEmpty(DbSession dbSession, String key) {
    checkKey(key);

    InternalPropertiesMapper mapper = getMapper(dbSession);
    mapper.deleteByKey(key);
    mapper.insertAsEmpty(key, system2.now());
  }

  /**
   * No streaming of value
   */
  public Optional<String> selectByKey(DbSession dbSession, String key) {
    checkKey(key);

    InternalPropertiesMapper mapper = getMapper(dbSession);
    InternalPropertyDto res = mapper.selectAsText(key);
    if (res == null) {
      return Optional.empty();
    }
    if (res.isEmpty()) {
      return OPTIONAL_OF_EMPTY_STRING;
    }
    if (res.getValue() != null) {
      return Optional.of(res.getValue());
    }
    res = mapper.selectAsClob(key);
    if (res == null) {
      Loggers.get(InternalPropertiesDao.class)
        .debug("Internal property {} has been found in db but has neither text value nor is empty. " +
          "Still we couldn't be retrieved with clob value. Ignoring the property.", key);
      return Optional.empty();
    }
    return Optional.of(res.getValue());
  }

  private static void checkKey(@Nullable String key) {
    checkArgument(key != null && !key.isEmpty(), "key can't be null nor empty");
  }

  private static InternalPropertiesMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(InternalPropertiesMapper.class);
  }
}
