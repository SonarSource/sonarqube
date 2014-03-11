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
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.Project;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class BatchExtensionDictionnaryTest {

  private BatchExtensionDictionnary newSelector(BatchExtension... extensions) {
    ComponentContainer iocContainer = new ComponentContainer();
    for (BatchExtension extension : extensions) {
      iocContainer.addSingleton(extension);
    }
    return new BatchExtensionDictionnary(iocContainer);
  }

  @Test
  public void testGetFilteredExtensionWithExtensionMatcher() {
    final Sensor sensor1 = new FakeSensor(), sensor2 = new FakeSensor();

    BatchExtensionDictionnary selector = newSelector(sensor1, sensor2);
    Collection<Sensor> sensors = selector.select(Sensor.class, null, true, new ExtensionMatcher() {
      @Override
      public boolean accept(Object extension) {
        return extension.equals(sensor1);
      }
    });

    assertThat(sensors, hasItem(sensor1));
    assertEquals(1, sensors.size());
  }

  class FakeSensor implements Sensor {

    public void analyse(Project project, SensorContext context) {

    }

    public boolean shouldExecuteOnProject(Project project) {
      return true;
    }
  }
}
