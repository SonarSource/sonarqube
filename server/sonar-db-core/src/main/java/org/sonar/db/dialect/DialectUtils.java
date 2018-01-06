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
package org.sonar.db.dialect;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.MessageException;

public final class DialectUtils {

  private static final Dialect[] DIALECTS = new Dialect[] {new H2(), new MySql(), new Oracle(), new PostgreSql(), new MsSql()};

  private DialectUtils() {
    // only static stuff
  }
  
  public static Dialect find(final String dialectId, final String jdbcConnectionUrl) {
    Dialect match = StringUtils.isNotBlank(dialectId) ? findById(dialectId) : findByJdbcUrl(jdbcConnectionUrl);
    if (match == null) {
      throw MessageException.of("Unable to determine database dialect to use within sonar with dialect " + dialectId + " jdbc url " + jdbcConnectionUrl);
    }
    return match;
  }

  @CheckForNull
  private static Dialect findByJdbcUrl(final String jdbcConnectionUrl) {
    return findDialect(dialect -> dialect != null && dialect.matchesJdbcURL(StringUtils.trimToEmpty(jdbcConnectionUrl)));
  }

  @CheckForNull
  private static Dialect findById(final String dialectId) {
    return findDialect(dialect -> dialect != null && dialect.getId().equals(dialectId));
  }

  @CheckForNull
  private static Dialect findDialect(Predicate<Dialect> predicate) {
    try {
      return Iterators.find(Iterators.forArray(DIALECTS), predicate);
    } catch (NoSuchElementException ex) {
      return null;
    }
  }
}
