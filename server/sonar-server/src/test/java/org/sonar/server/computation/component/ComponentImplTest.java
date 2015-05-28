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
package org.sonar.server.computation.component;

import java.util.Collections;
import org.junit.Test;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentImplTest {
  private ComponentImpl component = new ComponentImpl(BatchReport.Component.newBuilder().build(), Collections.<Component>emptyList());

  @Test(expected = UnsupportedOperationException.class)
  public void getUuid_throws_UOE_if_uuid_has_not_been_set_yet() {
    component.getUuid();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void getKey_throws_UOE_if_uuid_has_not_been_set_yet() {
    component.getKey();
  }

  @Test
  public void verify_setUuid() {
    String uuid = "toto";
    assertThat(component.setUuid(uuid).getUuid()).isEqualTo(uuid);
  }

  @Test
  public void verify_setKey() {
    String key = "toto";
    assertThat(component.setKey(key).getKey()).isEqualTo(key);
  }

  @Test
  public void convertType() {
    for (Constants.ComponentType componentType : Constants.ComponentType.values()) {
      assertThat(ComponentImpl.convertType(componentType)).isEqualTo(Component.Type.valueOf(componentType.name()));
    }
  }
}
