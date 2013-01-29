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
package org.sonar.core.component;

import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.File;

import static org.fest.assertions.Assertions.assertThat;

public class ResourceComponentTest {
  @Test
  public void snapshot_id_should_be_optional() {
    ResourceComponent component = new ResourceComponent(new File("foo.c"), new Snapshot());

    assertThat(component.snapshotId()).isNull();
  }

  @Test
  public void snapshot_id_should_be_set() {
    Snapshot snapshot = new Snapshot();
    snapshot.setId(123);
    ResourceComponent component = new ResourceComponent(new File("foo.c"), snapshot);

    assertThat(component.snapshotId()).isEqualTo(123);
  }

  @Test
  public void should_use_effective_key() {
    File file = new File("foo.c");
    file.setEffectiveKey("myproject:path/to/foo.c");
    ResourceComponent component = new ResourceComponent(file);

    assertThat(component.key()).isEqualTo("myproject:path/to/foo.c");
  }
}
