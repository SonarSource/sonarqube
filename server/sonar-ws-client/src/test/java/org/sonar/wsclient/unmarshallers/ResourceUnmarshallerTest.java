/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Resource;

import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResourceUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void singleResource() {
    String json = loadFile("/resources/single-resource.json");
    assertSonar(new ResourceUnmarshaller().toModel(json));

    List<Resource> resources = new ResourceUnmarshaller().toModels(json);
    assertThat(resources.size(), is(1));
    assertSonar(resources.get(0));
  }

  @Test
  public void singleResourceWithMeasures() {
    Resource resource = new ResourceUnmarshaller().toModel(loadFile("/resources/single-resource-with-measures.json"));
    assertSonar(resource);

    assertThat(resource.getMeasures().size(), is(2));
    assertThat(resource.getMeasureIntValue("lines"), is(47798));
    assertThat(resource.getMeasureIntValue("ncloc"), is(27066));
    assertThat(resource.getMeasureIntValue("unknown"), nullValue());
  }

  @Test
  public void manyResources() {
    List<Resource> resources = new ResourceUnmarshaller().toModels(loadFile("/resources/many-resources.json"));

    assertThat(resources.size(), is(19));
    for (Resource resource : resources) {
      assertThat(resource.getKey(), not(nullValue()));
      assertThat(resource.getId(), not(nullValue()));
      assertThat(resource.getMeasures().isEmpty(), is(true));
    }
  }

  @Test
  public void manyResourcesWithMeasures() {
    List<Resource> resources = new ResourceUnmarshaller().toModels(loadFile("/resources/many-resources-with-measures.json"));

    assertThat(resources.size(), is(17));
    for (Resource resource : resources) {
      assertThat(resource.getKey(), not(nullValue()));
      assertThat(resource.getId(), not(nullValue()));
      assertThat(resource.getMeasures().size(), is(2));
    }
  }

  private void assertSonar(Resource resource) {
    assertThat(resource.getId(), is(48569));
    assertThat(resource.getKey(), is("org.codehaus.sonar:sonar"));
    assertThat(resource.getName(), is("Sonar"));
    assertThat(resource.getScope(), is("PRJ"));
    assertThat(resource.getQualifier(), is("TRK"));
    assertThat(resource.getLanguage(), is("java"));
    assertThat(resource.getDescription(), is("Embrace Quality"));
    assertThat(resource.getDate(), not(nullValue()));
    assertThat(resource.getCreationDate(), not(nullValue()));
  }
}
