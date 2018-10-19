/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.alm;

import java.util.Objects;
import java.util.Optional;
import org.assertj.core.api.AbstractAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.ALM.BITBUCKETCLOUD;
import static org.sonar.db.alm.ALM.GITHUB;

public class AlmAppInstallDaoTest {

  private static final String A_UUID = "abcde1234";
  private static final String A_UUID_2 = "xyz789";
  private static final String EMPTY_STRING = "";
  private static final String A_OWNER = "my_org_id";
  private static final String ANOTHER_OWNER = "another_org";
  private static final long DATE = 1_600_000_000_000L;
  private static final long DATE_LATER = 1_700_000_000_000L;
  private static final String AN_INSTALL = "some install id";
  private static final String OTHER_INSTALL = "other install id";

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private DbSession dbSession = db.getSession();
  private UuidFactory uuidFactory = mock(UuidFactory.class);
  private AlmAppInstallDao underTest = new AlmAppInstallDao(system2, uuidFactory);

  @Test
  public void selectByOwnerId() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, AN_INSTALL);

    assertThat(underTest.selectByOwnerId(dbSession, GITHUB, A_OWNER).get())
      .extracting(AlmAppInstallDto::getUuid, AlmAppInstallDto::getAlm, AlmAppInstallDto::getInstallId, AlmAppInstallDto::getOwnerId,
        AlmAppInstallDto::getCreatedAt, AlmAppInstallDto::getUpdatedAt)
      .contains(A_UUID, GITHUB, A_OWNER, AN_INSTALL, DATE, DATE);

    assertThat(underTest.selectByOwnerId(dbSession, BITBUCKETCLOUD, A_OWNER)).isNotPresent();
    assertThat(underTest.selectByOwnerId(dbSession, GITHUB, "Unknown owner")).isNotPresent();
  }

  @Test
  public void selectByOwner_throws_NPE_when_alm_is_null() {
    expectAlmNPE();

    underTest.selectByOwnerId(dbSession, null, A_OWNER);
  }

  @Test
  public void selectByOwner_throws_IAE_when_owner_id_is_null() {
    expectOwnerIdNullOrEmptyIAE();

    underTest.selectByOwnerId(dbSession, GITHUB, null);
  }

  @Test
  public void selectByOwner_throws_IAE_when_owner_id_is_empty() {
    expectOwnerIdNullOrEmptyIAE();

    underTest.selectByOwnerId(dbSession, GITHUB, EMPTY_STRING);
  }

  @Test
  public void selectByInstallationId() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, AN_INSTALL);

    assertThat(underTest.selectByInstallationId(dbSession, GITHUB, AN_INSTALL).get())
      .extracting(AlmAppInstallDto::getUuid, AlmAppInstallDto::getAlm, AlmAppInstallDto::getInstallId, AlmAppInstallDto::getOwnerId,
        AlmAppInstallDto::getCreatedAt, AlmAppInstallDto::getUpdatedAt)
      .contains(A_UUID, GITHUB, A_OWNER, AN_INSTALL, DATE, DATE);

    assertThat(underTest.selectByInstallationId(dbSession, GITHUB, "unknown installation")).isEmpty();
    assertThat(underTest.selectByInstallationId(dbSession, BITBUCKETCLOUD, AN_INSTALL)).isEmpty();
  }

  @Test
  public void insert_throws_NPE_if_alm_is_null() {
    expectAlmNPE();

    underTest.insertOrUpdate(dbSession, null, A_OWNER, true, AN_INSTALL);
  }

  @Test
  public void insert_throws_IAE_if_owner_id_is_null() {
    expectOwnerIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, null, true, AN_INSTALL);
  }

  @Test
  public void insert_throws_IAE_if_owner_id_is_empty() {
    expectOwnerIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, EMPTY_STRING, true, AN_INSTALL);
  }

  @Test
  public void insert_throws_IAE_if_install_id_is_null() {
    expectInstallIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, null);
  }

  @Test
  public void insert_throws_IAE_if_install_id_is_empty() {
    expectInstallIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, EMPTY_STRING);
  }

  @Test
  public void insert() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, AN_INSTALL);

    assertThatAlmAppInstall(GITHUB, A_OWNER)
      .hasInstallId(AN_INSTALL)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);
  }

  @Test
  public void delete() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, AN_INSTALL);

    assertThatAlmAppInstall(GITHUB, A_OWNER)
      .hasInstallId(AN_INSTALL)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);

    underTest.delete(dbSession, GITHUB, A_OWNER);
    assertThatAlmAppInstall(GITHUB, A_OWNER).doesNotExist();
  }

  @Test
  public void delete_does_not_fail() {
    assertThatAlmAppInstall(GITHUB, A_OWNER).doesNotExist();

    underTest.delete(dbSession, GITHUB, A_OWNER);
  }

  @Test
  public void update() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, AN_INSTALL);

    when(system2.now()).thenReturn(DATE_LATER);
    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, OTHER_INSTALL);

    assertThatAlmAppInstall(GITHUB, A_OWNER)
      .hasInstallId(OTHER_INSTALL)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE_LATER);
  }

  @Test
  public void putMultiple() {
    when(system2.now()).thenReturn(DATE);
    when(uuidFactory.create())
      .thenReturn(A_UUID)
      .thenReturn(A_UUID_2);
    underTest.insertOrUpdate(dbSession, GITHUB, A_OWNER, true, AN_INSTALL);
    underTest.insertOrUpdate(dbSession, GITHUB, ANOTHER_OWNER, false, OTHER_INSTALL);

    assertThatAlmAppInstall(GITHUB, A_OWNER)
      .hasInstallId(AN_INSTALL)
      .hasOwnerUser(true)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);

    assertThatAlmAppInstall(GITHUB, ANOTHER_OWNER)
      .hasInstallId(OTHER_INSTALL)
      .hasOwnerUser(false)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);
  }

  private void expectAlmNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("alm can't be null");
  }

  private void expectOwnerIdNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("ownerId can't be null nor empty");
  }

  private void expectInstallIdNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("installId can't be null nor empty");
  }

  private AlmAppInstallAssert assertThatAlmAppInstall(ALM alm, String ownerId) {
    return new AlmAppInstallAssert(db, dbSession, alm, ownerId);
  }

  private static class AlmAppInstallAssert extends AbstractAssert<AlmAppInstallAssert, AlmAppInstallDto> {

    private AlmAppInstallAssert(DbTester dbTester, DbSession dbSession, ALM alm, String ownerId) {
      super(asAlmAppInstall(dbTester, dbSession, alm, ownerId), AlmAppInstallAssert.class);
    }

    private static AlmAppInstallDto asAlmAppInstall(DbTester db, DbSession dbSession, ALM alm, String ownerId) {
      Optional<AlmAppInstallDto> almAppInstall = db.getDbClient().almAppInstallDao().selectByOwnerId(dbSession, alm, ownerId);
      return almAppInstall.orElse(null);
    }

    public void doesNotExist() {
      isNull();
    }

    AlmAppInstallAssert hasInstallId(String expected) {
      isNotNull();

      if (!Objects.equals(actual.getInstallId(), expected)) {
        failWithMessage("Expected ALM App Install to have column INSTALL_ID to be <%s> but was <%s>", expected, actual.getInstallId());
      }
      return this;
    }

    AlmAppInstallAssert hasOwnerUser(boolean expected) {
      isNotNull();

      if (!Objects.equals(actual.isOwnerUser(), expected)) {
        failWithMessage("Expected ALM App Install to have column IS_OWNER_USER to be <%s> but was <%s>", expected, actual.isOwnerUser());
      }
      return this;
    }

    AlmAppInstallAssert hasCreatedAt(long expected) {
      isNotNull();

      if (!Objects.equals(actual.getCreatedAt(), expected)) {
        failWithMessage("Expected ALM App Install to have column CREATED_AT to be <%s> but was <%s>", expected, actual.getCreatedAt());
      }

      return this;
    }

    AlmAppInstallAssert hasUpdatedAt(long expected) {
      isNotNull();

      if (!Objects.equals(actual.getUpdatedAt(), expected)) {
        failWithMessage("Expected ALM App Install to have column UPDATED_AT to be <%s> but was <%s>", expected, actual.getUpdatedAt());
      }

      return this;
    }

  }

}
