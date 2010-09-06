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
package org.sonar.wsclient.unmarshallers;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.wsclient.services.Property;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PropertyUnmarshallerTest {
  @Test
  public void toModel() throws IOException {
    Property property = new PropertyUnmarshaller().toModel("[]");
    assertThat(property, nullValue());

    property = new PropertyUnmarshaller().toModel(loadFile("/properties/single.json"));
    assertThat(property.getKey(), is("myprop"));
    assertThat(property.getValue(), is("myvalue"));
  }

  @Test
  public void toModels() throws IOException {
    Collection<Property> properties = new PropertyUnmarshaller().toModels("[]");
    assertThat(properties.size(), is(0));

    properties = new PropertyUnmarshaller().toModels(loadFile("/properties/single.json"));
    assertThat(properties.size(), is(1));

    properties = new PropertyUnmarshaller().toModels(loadFile("/properties/many.json"));
    assertThat(properties.size(), is(3));
  }

  private static String loadFile(String path) throws IOException {
    return IOUtils.toString(PropertyUnmarshallerTest.class.getResourceAsStream(path));
  }
}
