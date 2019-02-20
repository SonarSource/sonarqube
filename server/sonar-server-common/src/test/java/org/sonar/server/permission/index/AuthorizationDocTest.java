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
package org.sonar.server.permission.index;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.server.es.Index;
import org.sonar.server.es.IndexType;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

@RunWith(DataProviderRunner.class)
public class AuthorizationDocTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void idOf_returns_argument_with_a_prefix() {
    String s = randomAlphabetic(12);

    assertThat(AuthorizationDoc.idOf(s)).isEqualTo("auth_" + s);
  }

  @Test
  public void idOf_fails_with_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("projectUuid can't be null");

    AuthorizationDoc.idOf(null);
  }

  @Test
  public void projectUuidOf_fails_with_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);

    AuthorizationDoc.projectUuidOf(null);
  }

  @Test
  public void projectUuidOf_returns_substring_if_starts_with_id_prefix() {
    assertThat(AuthorizationDoc.projectUuidOf("auth_")).isEmpty();

    String id = randomAlphabetic(1 + new Random().nextInt(10));
    assertThat(AuthorizationDoc.projectUuidOf("auth_" + id)).isEqualTo(id);
  }

  @Test
  public void projectUuidOf_returns_argument_if_does_not_starts_with_id_prefix() {
    String id = randomAlphabetic(1 + new Random().nextInt(10));
    assertThat(AuthorizationDoc.projectUuidOf(id)).isEqualTo(id);
    assertThat(AuthorizationDoc.projectUuidOf("")).isEqualTo("");
  }

  @Test
  public void getId_fails_with_NPE_if_IndexPermissions_has_null_projectUuid() {
    IndexPermissions dto = new IndexPermissions(null, null);
    IndexType.IndexMainType mainType = IndexType.main(Index.simple("foo"), "bar");
    AuthorizationDoc underTest = AuthorizationDoc.fromDto(mainType, dto);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("projectUuid can't be null");

    underTest.getId();
  }

  @Test
  @UseDataProvider("dtos")
  public void getId_returns_projectUuid_with_a_prefix(IndexPermissions dto) {
    AuthorizationDoc underTest = AuthorizationDoc.fromDto(IndexType.main(Index.simple("foo"), "bar"), dto);

    assertThat(underTest.getId()).isEqualTo("auth_" + dto.getProjectUuid());
  }

  @Test
  @UseDataProvider("dtos")
  public void getRouting_returns_projectUuid(IndexPermissions dto) {
    AuthorizationDoc underTest = AuthorizationDoc.fromDto(IndexType.main(Index.simple("foo"), "bar"), dto);

    assertThat(underTest.getRouting()).contains(dto.getProjectUuid());
  }

  @Test
  public void fromDto_of_allowAnyone_is_false_and_no_user_nor_group() {
    IndexPermissions underTest = new IndexPermissions(randomAlphabetic(3), randomAlphabetic(4));

    AuthorizationDoc doc = AuthorizationDoc.fromDto(IndexType.main(Index.simple("foo"), "bar"), underTest);

    boolean auth_allowAnyone = doc.getField("auth_allowAnyone");
    assertThat(auth_allowAnyone).isFalse();
    List<Integer> userIds = doc.getField("auth_userIds");
    assertThat(userIds).isEmpty();
    List<Integer> groupIds = doc.getField("auth_groupIds");
    assertThat(groupIds).isEmpty();
  }

  @Test
  public void fromDto_defines_userIds_and_groupIds_if_allowAnyone_is_false() {
    IndexPermissions underTest = new IndexPermissions(randomAlphabetic(3), randomAlphabetic(4));
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(underTest::addUserId);
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(underTest::addGroupId);

    AuthorizationDoc doc = AuthorizationDoc.fromDto(IndexType.main(Index.simple("foo"), "bar"), underTest);

    boolean auth_allowAnyone = doc.getField("auth_allowAnyone");
    assertThat(auth_allowAnyone).isFalse();
    List<Integer> userIds = doc.getField("auth_userIds");
    assertThat(userIds).isEqualTo(underTest.getUserIds());
    List<Integer> groupIds = doc.getField("auth_groupIds");
    assertThat(groupIds).isEqualTo(underTest.getGroupIds());
  }

  @Test
  public void fromDto_ignores_userIds_and_groupIds_if_allowAnyone_is_true() {
    IndexPermissions underTest = new IndexPermissions(randomAlphabetic(3), randomAlphabetic(4));
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(underTest::addUserId);
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(underTest::addGroupId);
    underTest.allowAnyone();

    AuthorizationDoc doc = AuthorizationDoc.fromDto(IndexType.main(Index.simple("foo"), "bar"), underTest);

    boolean auth_allowAnyone = doc.getField("auth_allowAnyone");
    assertThat(auth_allowAnyone).isTrue();
    try {
      doc.getField("auth_userIds");
      fail("should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field auth_userIds not specified in query options");
    }
    try {
      doc.getField("auth_groupIds");
      fail("should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Field auth_groupIds not specified in query options");
    }
  }

  @DataProvider
  public static Object[][] dtos() {
    IndexPermissions allowAnyone = new IndexPermissions(randomAlphabetic(3), randomAlphabetic(4));
    allowAnyone.allowAnyone();
    IndexPermissions someUserIds = new IndexPermissions(randomAlphabetic(3), randomAlphabetic(4));
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(someUserIds::addUserId);
    IndexPermissions someGroupIds = new IndexPermissions(randomAlphabetic(3), randomAlphabetic(4));
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(someGroupIds::addGroupId);
    IndexPermissions someGroupIdAndUserIs = new IndexPermissions(randomAlphabetic(3), randomAlphabetic(4));
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(someGroupIdAndUserIs::addUserId);
    IntStream.range(0, 1 + new Random().nextInt(5)).forEach(someGroupIdAndUserIs::addGroupId);
    return new Object[][] {
      {allowAnyone},
      {someUserIds},
      {someGroupIds},
      {someGroupIdAndUserIs}
    };
  }
}
