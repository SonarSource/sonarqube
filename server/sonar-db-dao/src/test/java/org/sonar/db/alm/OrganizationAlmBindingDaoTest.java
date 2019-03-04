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

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.alm.ALM.BITBUCKETCLOUD;
import static org.sonar.db.alm.ALM.GITHUB;

public class OrganizationAlmBindingDaoTest {

  private static final long NOW = 1_600_000_000_000L;

  private System2 system2 = new TestSystem2().setNow(NOW);

  @Rule
  public DbTester db = DbTester.create(system2);

  private UuidFactory uuidFactory = mock(UuidFactory.class);

  private OrganizationAlmBindingDao underTest = new OrganizationAlmBindingDao(system2, uuidFactory);

  @Test
  public void selectByOrganization() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    OrganizationAlmBindingDto dto = db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);

    Optional<OrganizationAlmBindingDto> result = underTest.selectByOrganization(db.getSession(), organization);

    assertThat(result).isPresent();
    assertThat(result.get())
      .extracting(OrganizationAlmBindingDto::getUuid, OrganizationAlmBindingDto::getOrganizationUuid, OrganizationAlmBindingDto::getAlmAppInstallUuid,
        OrganizationAlmBindingDto::getUrl, OrganizationAlmBindingDto::getAlm,
        OrganizationAlmBindingDto::getUserUuid, OrganizationAlmBindingDto::getCreatedAt)
      .containsExactlyInAnyOrder(dto.getUuid(), organization.getUuid(), dto.getAlmAppInstallUuid(),
        dto.getUrl(), GITHUB,
        dto.getUserUuid(), NOW);
  }

  @Test
  public void selectByOrganization_returns_empty_when_organization_is_not_bound_to_installation() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);
    // No binding on other installation
    OrganizationDto otherOrganization = db.organizations().insert();

    Optional<OrganizationAlmBindingDto> result = underTest.selectByOrganization(db.getSession(), otherOrganization);

    assertThat(result).isEmpty();
  }

  @Test
  public void selectByOrganizationUuid() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    OrganizationAlmBindingDto dto = db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);

    assertThat(underTest.selectByOrganizationUuid(db.getSession(), organization.getUuid()).get())
      .extracting(OrganizationAlmBindingDto::getUuid, OrganizationAlmBindingDto::getOrganizationUuid, OrganizationAlmBindingDto::getAlmAppInstallUuid,
        OrganizationAlmBindingDto::getUrl, OrganizationAlmBindingDto::getAlm,
        OrganizationAlmBindingDto::getUserUuid, OrganizationAlmBindingDto::getCreatedAt)
      .containsExactlyInAnyOrder(dto.getUuid(), organization.getUuid(), dto.getAlmAppInstallUuid(),
        dto.getUrl(), GITHUB,
        dto.getUserUuid(), NOW);

    assertThat(underTest.selectByOrganizationUuid(db.getSession(), "unknown")).isNotPresent();
  }

  @Test
  public void selectByOrganizations() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationAlmBindingDto organizationAlmBinding1 = db.alm().insertOrganizationAlmBinding(organization1, db.alm().insertAlmAppInstall(), true);
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationAlmBindingDto organizationAlmBinding2 = db.alm().insertOrganizationAlmBinding(organization2, db.alm().insertAlmAppInstall(), true);
    OrganizationDto organizationNotBound = db.organizations().insert();

    assertThat(underTest.selectByOrganizations(db.getSession(), asList(organization1, organization2, organizationNotBound)))
      .extracting(OrganizationAlmBindingDto::getUuid, OrganizationAlmBindingDto::getOrganizationUuid)
      .containsExactlyInAnyOrder(
        tuple(organizationAlmBinding1.getUuid(), organization1.getUuid()),
        tuple(organizationAlmBinding2.getUuid(), organization2.getUuid()));

    assertThat(underTest.selectByOrganizations(db.getSession(), singletonList(organizationNotBound))).isEmpty();
  }

  @Test
  public void selectByAlmAppInstall() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    OrganizationAlmBindingDto dto = db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);

    Optional<OrganizationAlmBindingDto> result = underTest.selectByAlmAppInstall(db.getSession(), almAppInstall);

    assertThat(result.get())
      .extracting(OrganizationAlmBindingDto::getUuid, OrganizationAlmBindingDto::getOrganizationUuid, OrganizationAlmBindingDto::getAlmAppInstallUuid,
        OrganizationAlmBindingDto::getUrl, OrganizationAlmBindingDto::getAlm,
        OrganizationAlmBindingDto::getUserUuid, OrganizationAlmBindingDto::getCreatedAt)
      .containsExactlyInAnyOrder(dto.getUuid(), organization.getUuid(), dto.getAlmAppInstallUuid(),
        dto.getUrl(), GITHUB,
        dto.getUserUuid(), NOW);
  }

  @Test
  public void selectByAlmAppInstall_returns_empty_when_installation_is_not_bound_to_organization() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);
    // No binding on other organization
    AlmAppInstallDto otherAlmAppInstall = db.alm().insertAlmAppInstall();

    Optional<OrganizationAlmBindingDto> result = underTest.selectByAlmAppInstall(db.getSession(), otherAlmAppInstall);

    assertThat(result).isEmpty();
  }

  @Test
  public void selectByOrganizationAlmIds() {
    AlmAppInstallDto gitHubInstall1 = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    OrganizationAlmBindingDto organizationAlmBinding1 = db.alm().insertOrganizationAlmBinding(db.organizations().insert(), gitHubInstall1, true);
    AlmAppInstallDto gitHubInstall2 = db.alm().insertAlmAppInstall(a -> a.setAlm(GITHUB));
    OrganizationAlmBindingDto organizationAlmBinding2 = db.alm().insertOrganizationAlmBinding(db.organizations().insert(), gitHubInstall2, true);
    AlmAppInstallDto bitBucketInstall = db.alm().insertAlmAppInstall(a -> a.setAlm(BITBUCKETCLOUD));
    OrganizationAlmBindingDto organizationAlmBinding3 = db.alm().insertOrganizationAlmBinding(db.organizations().insert(), bitBucketInstall, true);

    List<OrganizationAlmBindingDto> result = underTest.selectByOrganizationAlmIds(db.getSession(), GITHUB,
      asList(gitHubInstall1.getOrganizationAlmId(), gitHubInstall2.getOrganizationAlmId(), bitBucketInstall.getOrganizationAlmId(), "unknown"));

    assertThat(result).extracting(OrganizationAlmBindingDto::getUuid)
      .containsExactlyInAnyOrder(organizationAlmBinding1.getUuid(), organizationAlmBinding2.getUuid());
  }

  @Test
  public void insert() {
    when(uuidFactory.create()).thenReturn("ABCD");
    OrganizationDto organization = db.organizations().insert();
    UserDto user = db.users().insertUser();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();

    underTest.insert(db.getSession(), organization, almAppInstall, "http://myorg.com", user.getUuid(), true);

    assertThat(underTest.selectByOrganizationUuid(db.getSession(), organization.getUuid()).get())
      .extracting(OrganizationAlmBindingDto::getUuid, OrganizationAlmBindingDto::getOrganizationUuid, OrganizationAlmBindingDto::getAlmAppInstallUuid,
        OrganizationAlmBindingDto::getAlm,
        OrganizationAlmBindingDto::getUrl, OrganizationAlmBindingDto::getUserUuid, OrganizationAlmBindingDto::isMembersSyncEnable, OrganizationAlmBindingDto::getCreatedAt)
      .containsExactlyInAnyOrder("ABCD", organization.getUuid(), almAppInstall.getUuid(), GITHUB, "http://myorg.com", user.getUuid(), true, NOW);
  }

  @Test
  public void deleteByOrganization() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);
    OrganizationDto otherOrganization = db.organizations().insert();
    AlmAppInstallDto otherAlmAppInstall = db.alm().insertAlmAppInstall();
    db.alm().insertOrganizationAlmBinding(otherOrganization, otherAlmAppInstall, true);

    underTest.deleteByOrganization(db.getSession(), organization);

    assertThat(underTest.selectByOrganization(db.getSession(), organization)).isNotPresent();
    assertThat(underTest.selectByOrganization(db.getSession(), otherOrganization)).isPresent();
  }

  @Test
  public void deleteByAlmAppInstall() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);
    OrganizationDto otherOrganization = db.organizations().insert();
    AlmAppInstallDto otherAlmAppInstall = db.alm().insertAlmAppInstall();
    db.alm().insertOrganizationAlmBinding(otherOrganization, otherAlmAppInstall, true);

    underTest.deleteByAlmAppInstall(db.getSession(), almAppInstall);

    assertThat(underTest.selectByOrganization(db.getSession(), organization)).isNotPresent();
    assertThat(underTest.selectByOrganization(db.getSession(), otherOrganization)).isPresent();
  }

  @Test
  public void updateMembersSync() {
    OrganizationDto organization = db.organizations().insert();
    AlmAppInstallDto almAppInstall = db.alm().insertAlmAppInstall();
    OrganizationAlmBindingDto orgAlmBindingDto = db.alm().insertOrganizationAlmBinding(organization, almAppInstall, true);

    underTest.updateMembersSync(db.getSession(), orgAlmBindingDto, false);

    assertThat(db.getDbClient().organizationAlmBindingDao().selectByOrganization(db.getSession(), organization).get().isMembersSyncEnable()).isFalse();
  }
}
