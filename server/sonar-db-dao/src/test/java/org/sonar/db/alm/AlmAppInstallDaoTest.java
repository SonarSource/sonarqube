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
package org.sonar.db.alm;

import java.util.Objects;
import java.util.Optional;
import org.assertj.core.api.AbstractAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.ALM.BITBUCKETCLOUD;
import static org.sonar.db.alm.ALM.GITHUB;

public class AlmAppInstallDaoTest {

  private static final String A_UUID = "abcde1234";
  private static final String A_UUID_2 = "xyz789";
  private static final String EMPTY_STRING = "";
  private static final String AN_ORGANIZATION_ALM_ID = "my_org_id";
  private static final String ANOTHER_ORGANIZATION_ALM_ID = "another_org";
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
  public void selectByUuid() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    String userUuid = Uuids.createFast();
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, userUuid);

    assertThat(underTest.selectByUuid(dbSession, A_UUID).get())
      .extracting(AlmAppInstallDto::getUuid, AlmAppInstallDto::getAlm, AlmAppInstallDto::getInstallId, AlmAppInstallDto::getOrganizationAlmId, AlmAppInstallDto::getUserExternalId,
        AlmAppInstallDto::getCreatedAt, AlmAppInstallDto::getUpdatedAt)
      .contains(A_UUID, GITHUB, AN_ORGANIZATION_ALM_ID, AN_INSTALL, userUuid, DATE, DATE);

    assertThat(underTest.selectByUuid(dbSession, "foo")).isNotPresent();
  }

  @Test
  public void selectByOrganizationAlmId() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, null);

    assertThat(underTest.selectByOrganizationAlmId(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID).get())
      .extracting(AlmAppInstallDto::getUuid, AlmAppInstallDto::getAlm, AlmAppInstallDto::getInstallId, AlmAppInstallDto::getOrganizationAlmId,
        AlmAppInstallDto::getCreatedAt, AlmAppInstallDto::getUpdatedAt)
      .contains(A_UUID, GITHUB, AN_ORGANIZATION_ALM_ID, AN_INSTALL, DATE, DATE);

    assertThat(underTest.selectByOrganizationAlmId(dbSession, BITBUCKETCLOUD, AN_ORGANIZATION_ALM_ID)).isNotPresent();
    assertThat(underTest.selectByOrganizationAlmId(dbSession, GITHUB, "Unknown owner")).isNotPresent();
  }

  @Test
  public void selectByOwner_throws_NPE_when_alm_is_null() {
    expectAlmNPE();

    underTest.selectByOrganizationAlmId(dbSession, null, AN_ORGANIZATION_ALM_ID);
  }

  @Test
  public void selectByOwner_throws_IAE_when_organization_alm_id_is_null() {
    expectOrganizationAlmIdNullOrEmptyIAE();

    underTest.selectByOrganizationAlmId(dbSession, GITHUB, null);
  }

  @Test
  public void selectByOwner_throws_IAE_when_organization_alm_id_is_empty() {
    expectOrganizationAlmIdNullOrEmptyIAE();

    underTest.selectByOrganizationAlmId(dbSession, GITHUB, EMPTY_STRING);
  }

  @Test
  public void selectByInstallationId() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, Uuids.createFast());

    assertThat(underTest.selectByInstallationId(dbSession, GITHUB, AN_INSTALL).get())
      .extracting(AlmAppInstallDto::getUuid, AlmAppInstallDto::getAlm, AlmAppInstallDto::getInstallId, AlmAppInstallDto::getOrganizationAlmId,
        AlmAppInstallDto::getCreatedAt, AlmAppInstallDto::getUpdatedAt)
      .contains(A_UUID, GITHUB, AN_ORGANIZATION_ALM_ID, AN_INSTALL, DATE, DATE);

    assertThat(underTest.selectByInstallationId(dbSession, GITHUB, "unknown installation")).isEmpty();
    assertThat(underTest.selectByInstallationId(dbSession, BITBUCKETCLOUD, AN_INSTALL)).isEmpty();
  }

  @Test
  public void selectUnboundByUserExternalId() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    AlmAppInstallDto almAppInstall1 = db.alm().insertAlmAppInstall(app -> app.setUserExternalId(user1.getExternalId()));
    AlmAppInstallDto almAppInstall2 = db.alm().insertAlmAppInstall(app -> app.setUserExternalId(user1.getExternalId()));
    AlmAppInstallDto almAppInstall3 = db.alm().insertAlmAppInstall(app -> app.setUserExternalId(user2.getExternalId()));
    db.alm().insertOrganizationAlmBinding(organization1, almAppInstall1, true);
    db.alm().insertOrganizationAlmBinding(organization2, almAppInstall3, true);

    assertThat(underTest.selectUnboundByUserExternalId(dbSession, user1.getExternalId()))
      .extracting(AlmAppInstallDto::getUuid)
      .containsExactlyInAnyOrder(almAppInstall2.getUuid());
    assertThat(underTest.selectUnboundByUserExternalId(dbSession, user2.getExternalId())).isEmpty();
  }

  @Test
  public void selectByOrganization() {
    OrganizationDto organization = db.organizations().insert();
    db.getDbClient().almAppInstallDao().insertOrUpdate(db.getSession(), ALM.GITHUB, "the-owner", false, "123456", null);
    // could be improved, insertOrUpdate should return the DTO with its uuid
    Optional<AlmAppInstallDto> install = db.getDbClient().almAppInstallDao().selectByOrganizationAlmId(db.getSession(), ALM.GITHUB, "the-owner");
    db.getDbClient().organizationAlmBindingDao().insert(db.getSession(), organization, install.get(), "xxx", "xxx", true);
    db.commit();

    assertThat(underTest.selectByOrganization(db.getSession(), GITHUB, organization).get().getUuid()).isEqualTo(install.get().getUuid());
    assertThat(underTest.selectByOrganization(db.getSession(), BITBUCKETCLOUD, organization)).isEmpty();
    assertThat(underTest.selectByOrganization(db.getSession(), GITHUB, new OrganizationDto().setUuid("other-organization"))).isEmpty();
  }

  @Test
  public void insert_throws_NPE_if_alm_is_null() {
    expectAlmNPE();

    underTest.insertOrUpdate(dbSession, null, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, null);
  }

  @Test
  public void insert_throws_IAE_if_organization_alm_id_is_null() {
    expectOrganizationAlmIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, null, true, AN_INSTALL, null);
  }

  @Test
  public void insert_throws_IAE_if_organization_alm_id_is_empty() {
    expectOrganizationAlmIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, EMPTY_STRING, true, AN_INSTALL, null);
  }

  @Test
  public void insert_throws_IAE_if_install_id_is_null() {
    expectInstallIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, null, null);
  }

  @Test
  public void insert_throws_IAE_if_install_id_is_empty() {
    expectInstallIdNullOrEmptyIAE();

    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, EMPTY_STRING, null);
  }

  @Test
  public void insert() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    String userUuid = Uuids.createFast();
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, userUuid);

    assertThatAlmAppInstall(GITHUB, AN_ORGANIZATION_ALM_ID)
      .hasInstallId(AN_INSTALL)
      .hasUserExternalId(userUuid)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);
  }

  @Test
  public void delete() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, null);

    underTest.delete(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID);

    assertThatAlmAppInstall(GITHUB, AN_ORGANIZATION_ALM_ID).doesNotExist();
  }

  @Test
  public void delete_does_not_fail() {
    assertThatAlmAppInstall(GITHUB, AN_ORGANIZATION_ALM_ID).doesNotExist();

    underTest.delete(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID);
  }

  @Test
  public void update() {
    when(uuidFactory.create()).thenReturn(A_UUID);
    when(system2.now()).thenReturn(DATE);
    String userExternalId1 = randomAlphanumeric(10);
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, userExternalId1);

    when(system2.now()).thenReturn(DATE_LATER);
    String userExternalId2 = randomAlphanumeric(10);
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, OTHER_INSTALL, userExternalId2);

    assertThatAlmAppInstall(GITHUB, AN_ORGANIZATION_ALM_ID)
      .hasInstallId(OTHER_INSTALL)
      .hasUserExternalId(userExternalId2)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE_LATER);
  }

  @Test
  public void putMultiple() {
    when(system2.now()).thenReturn(DATE);
    when(uuidFactory.create())
      .thenReturn(A_UUID)
      .thenReturn(A_UUID_2);
    String userExternalId1 = randomAlphanumeric(10);
    String userExternalId2 = randomAlphanumeric(10);
    underTest.insertOrUpdate(dbSession, GITHUB, AN_ORGANIZATION_ALM_ID, true, AN_INSTALL, userExternalId1);
    underTest.insertOrUpdate(dbSession, GITHUB, ANOTHER_ORGANIZATION_ALM_ID, false, OTHER_INSTALL, userExternalId2);

    assertThatAlmAppInstall(GITHUB, AN_ORGANIZATION_ALM_ID)
      .hasInstallId(AN_INSTALL)
      .hasOwnerUser(true)
      .hasUserExternalId(userExternalId1)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);

    assertThatAlmAppInstall(GITHUB, ANOTHER_ORGANIZATION_ALM_ID)
      .hasInstallId(OTHER_INSTALL)
      .hasOwnerUser(false)
      .hasUserExternalId(userExternalId2)
      .hasCreatedAt(DATE)
      .hasUpdatedAt(DATE);
  }

  private void expectAlmNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("alm can't be null");
  }

  private void expectOrganizationAlmIdNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("organizationAlmId can't be null nor empty");
  }

  private void expectInstallIdNullOrEmptyIAE() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("installId can't be null nor empty");
  }

  private AlmAppInstallAssert assertThatAlmAppInstall(ALM alm, String organizationAlmId) {
    return new AlmAppInstallAssert(db, dbSession, alm, organizationAlmId);
  }

  private static class AlmAppInstallAssert extends AbstractAssert<AlmAppInstallAssert, AlmAppInstallDto> {

    private AlmAppInstallAssert(DbTester dbTester, DbSession dbSession, ALM alm, String organizationAlmId) {
      super(asAlmAppInstall(dbTester, dbSession, alm, organizationAlmId), AlmAppInstallAssert.class);
    }

    private static AlmAppInstallDto asAlmAppInstall(DbTester db, DbSession dbSession, ALM alm, String organizationAlmId) {
      Optional<AlmAppInstallDto> almAppInstall = db.getDbClient().almAppInstallDao().selectByOrganizationAlmId(dbSession, alm, organizationAlmId);
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

    AlmAppInstallAssert hasUserExternalId(String expected) {
      isNotNull();

      if (!Objects.equals(actual.getUserExternalId(), expected)) {
        failWithMessage("Expected ALM App Install to have column USER_EXTERNAL_ID to be <%s> but was <%s>", expected, actual.getUserExternalId());
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
