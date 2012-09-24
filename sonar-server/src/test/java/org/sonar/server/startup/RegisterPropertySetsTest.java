/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertySet;
import org.sonar.api.config.PropertySetDefinitions;
import org.sonar.api.config.PropertySetTemplate;

import static org.mockito.Mockito.*;

public class RegisterPropertySetsTest {
  private RegisterPropertySets task;
  private PropertySetDefinitions propertySetDefinitions = mock(PropertySetDefinitions.class);
  private PropertySetTemplate firstTemplate = mock(PropertySetTemplate.class);
  private PropertySetTemplate secondTemplate = mock(PropertySetTemplate.class);
  private PropertySet firstPropertySet = mock(PropertySet.class);
  private PropertySet secondPropertySet = mock(PropertySet.class);

  @Before
  public void init() {
    task = new RegisterPropertySets(new PropertySetTemplate[]{firstTemplate, secondTemplate}, propertySetDefinitions);
  }

  @Test
  public void should_register_on_startup() {
    when(firstTemplate.getName()).thenReturn("first");
    when(secondTemplate.getName()).thenReturn("second");
    when(firstTemplate.createPropertySet()).thenReturn(firstPropertySet);
    when(secondTemplate.createPropertySet()).thenReturn(secondPropertySet);

    task.start();

    verify(propertySetDefinitions).register("first", firstPropertySet);
    verify(propertySetDefinitions).register("second", secondPropertySet);
  }
}
