/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.jpa.dialect;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.SonarException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @since 1.12
 */
public final class DialectRepository {

  private DialectRepository() {
  }

  private static List<Dialect> builtInDialects = getSupportedDialects();

  public static Dialect find(final String dialectId, final String jdbcConnectionUrl) {
    Dialect match = StringUtils.isNotEmpty(dialectId) ? findById(dialectId) : findByJdbcUrl(jdbcConnectionUrl);
    if (match == null) {
      throw new SonarException("Unable to determine database dialect to use within sonar with dialect " + dialectId + " jdbc url " + jdbcConnectionUrl);
    }
    return match;
  }

  private static Dialect findByJdbcUrl(final String jdbcConnectionUrl) {
    Dialect match = findDialect(builtInDialects, new Predicate<Dialect>() {
      public boolean apply(Dialect dialect) {
        return dialect.matchesJdbcURL(StringUtils.trimToEmpty(jdbcConnectionUrl));
      }
    });
    return match;
  }

  private static Dialect findById(final String dialectId) {
    Dialect match = findDialect(builtInDialects, new Predicate<Dialect>() {
      public boolean apply(Dialect dialect) {
        return dialect.getId().equals(dialectId);
      }
    });
    // maybe a class name if no match
    match = match == null ? getDialectByClassname(dialectId) : match;
    return match;
  }

  private static Dialect findDialect(Collection<Dialect> dialects, Predicate<Dialect> predicate) {
    try {
      return Iterators.find(dialects.iterator(), predicate);
    } catch (NoSuchElementException ex) {
      return null;
    }
  }

  private static Dialect getDialectByClassname(String dialectId) {
    try {
      Class<? extends Dialect> dialectClass = (Class<? extends Dialect>) DialectRepository.class.getClassLoader().loadClass(dialectId);
      return dialectClass.newInstance();
    } catch (ClassNotFoundException e) {
      // dialectId was not a class name :)
    } catch (Exception e) {
      throw new SonarException("Unable to instanciate dialect class", e);
    }
    return null;
  }

  private static List<Dialect> getSupportedDialects() {
    return Arrays.asList(new Derby(), new HsqlDb(), new MySql(), new Oracle(), new PostgreSql(), new MsSql());
  }
}
