/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.qualityprofile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRuleParamDtoTest {

  @Test
  public void groupByKey() {
    assertThat(ActiveRuleParamDto.groupByKey(Collections.emptyList())).isEmpty();

    Collection<ActiveRuleParamDto> dtos = Arrays.asList(
      new ActiveRuleParamDto().setKey("foo"), new ActiveRuleParamDto().setKey("bar")
      );
    Map<String, ActiveRuleParamDto> group = ActiveRuleParamDto.groupByKey(dtos);
    assertThat(group.keySet()).containsOnly("foo", "bar");
  }
}
