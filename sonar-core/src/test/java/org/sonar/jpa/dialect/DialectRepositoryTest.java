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

import org.junit.Test;
import static org.junit.Assert.*;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.utils.SonarException;

public class DialectRepositoryTest {
  
  @Test
  public void testFindById() {
    Dialect d = DialectRepository.find(DatabaseProperties.DIALECT_MYSQL, null);
    assertEquals(MySql.class, d.getClass());
  }
  
  @Test
  public void testFindByJdbcUrl() {
    Dialect d = DialectRepository.find(null, "jdbc:mysql:foo:bar");
    assertEquals(MySql.class, d.getClass());
  }
  
  @Test
  public void testFindClassName() {
    Dialect d = DialectRepository.find(TestDialect.class.getName(), null);
    assertEquals(TestDialect.class, d.getClass());
  }
  
  @Test(expected=SonarException.class)
  public void testFindNoMatch() {
    DialectRepository.find("foo", "bar");
  }
  
  public static class TestDialect implements Dialect {
    public boolean matchesJdbcURL(String jdbcConnectionURL) {
      return false;
    }
    
    public String getId() {
      return "testDialect";
    }
    
    public Class<? extends org.hibernate.dialect.Dialect> getHibernateDialectClass() {
      return null;
    }
    
    public String getActiveRecordDialectCode() {
      return null;
    }
  }

}
