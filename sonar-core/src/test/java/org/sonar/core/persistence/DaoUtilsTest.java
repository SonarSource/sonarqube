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
package org.sonar.core.persistence;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class DaoUtilsTest {

  @Test
  public void list_all_dao_classes() {
    List<Class> daoClasses = DaoUtils.getDaoClasses();

    assertThat(daoClasses).isNotEmpty();
  }

  @Test
  public void repeatCondition() throws Exception {
    assertThat(DaoUtils.repeatCondition("uuid=?", 1, "or")).isEqualTo("uuid=?");
    assertThat(DaoUtils.repeatCondition("uuid=?", 3, "or")).isEqualTo("uuid=? or uuid=? or uuid=?");
  }

  @Test
  public void execute_large_inputs() throws Exception {
    List<Integer> inputs = newArrayList();
    List<String> expectedOutputs = newArrayList();
    for (int i = 0; i < 2010; i++) {
      inputs.add(i);
      expectedOutputs.add(Integer.toString(i));
    }

    List<String> outputs = DaoUtils.executeLargeInputs(inputs, new Function<List<Integer>, List<String>>() {
      @Override
      public List<String> apply(List<Integer> input) {
        // Check that each partition is only done on 1000 elements max
        assertThat(input.size()).isLessThanOrEqualTo(1000);
        return newArrayList(Iterables.transform(input, new Function<Integer, String>() {
          @Override
          public String apply(Integer input) {
            return Integer.toString(input);
          }
        }));
      }
    });

    assertThat(outputs).isEqualTo(expectedOutputs);
  }

  @Test
  public void execute_large_inputs_on_empty_list() throws Exception {
    List<String> outputs = DaoUtils.executeLargeInputs(Collections.<Integer>emptyList(), new Function<List<Integer>, List<String>>() {
      @Override
      public List<String> apply(List<Integer> input) {
        fail("No partition should be made on empty list");
        return Collections.emptyList();
      }
    });

    assertThat(outputs).isEmpty();
  }
}
