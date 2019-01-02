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
package org.sonar.ce.task.projectanalysis.qualityprofile;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class QProfileStatusRepositoryImplTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private QProfileStatusRepositoryImpl underTest;

  @Before
  public void setUp() {
    underTest = new QProfileStatusRepositoryImpl();
  }

  @Test
  @UseDataProvider("qualityProfileStatuses")
  public void get_return_optional_of_status(QProfileStatusRepository.Status status) {
    underTest.register("key", status);

    assertThat(underTest.get("key")).isEqualTo(Optional.of(status));
  }

  @Test
  @UseDataProvider("qualityProfileStatuses")
  public void get_return_empty_for_qp_not_registered(QProfileStatusRepository.Status status) {
    underTest.register("key", status);

    assertThat(underTest.get("other_key")).isEqualTo(Optional.empty());
  }

  @Test
  public void get_return_empty_for_null_qp_key() {
    assertThat(underTest.get(null)).isEqualTo(Optional.empty());
  }

  @Test
  @UseDataProvider("qualityProfileStatuses")
  public void register_fails_with_NPE_if_qpKey_is_null(QProfileStatusRepository.Status status) {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("qpKey can't be null");

    underTest.register(null, status);
  }

  @Test
  public void register_fails_with_NPE_if_status_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    underTest.register("key", null);
  }

  @Test
  @UseDataProvider("qualityProfileStatuses")
  public void register_fails_with_ISE_if_qp_is_already_registered(QProfileStatusRepository.Status status) {
    underTest.register("key", status);
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Quality Profile 'key' is already registered");

    underTest.register("key", status);
  }

  @DataProvider
  public static Object[][] qualityProfileStatuses() {
    return Stream.of(QProfileStatusRepository.Status.values())
      .map(s -> new Object[] {s})
      .toArray(Object[][]::new);
  }
}
