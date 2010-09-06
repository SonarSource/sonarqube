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
package org.sonar.server.database;

import org.junit.Test;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.sql.DataSource;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UniqueDatasourceFactoryTest {

  @Test
  public void testGetObjectInstance() throws Exception {
    Reference ref = mock(Reference.class);
    Name name = mock(Name.class);
    Context ctx = mock(Context.class);

    when(ref.getClassName()).thenReturn(DataSource.class.getName());

    UniqueDatasourceFactory factory = new UniqueDatasourceFactory();
    DataSource ds = (DataSource) factory.getObjectInstance(ref, name, ctx, new Hashtable());
    DataSource ds2 = (DataSource) factory.getObjectInstance(ref, name, ctx, new Hashtable());
    assertNotNull(ds);
    assertNotNull(ds2);
    // must be the same memory reference
    assertTrue(ds == ds2);

    // new factory instance
    factory = new UniqueDatasourceFactory();
    DataSource ds3 = (DataSource) factory.getObjectInstance(ref, name, ctx, new Hashtable());
    assertNotNull(ds3);
    assertTrue(ds == ds3);
    assertTrue(ds == ds2);

  }

}
