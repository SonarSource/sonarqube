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
import * as React from 'react';
import { Link } from 'react-router';
import OrganizationNavigationExtensions from './OrganizationNavigationExtensions';
import OrganizationNavigationAdministration from './OrganizationNavigationAdministration';
import { Organization } from '../../../app/types';
import NavBarTabs from '../../../components/nav/NavBarTabs';
import { translate } from '../../../helpers/l10n';
import { getQualityGatesUrl } from '../../../helpers/urls';

interface Props {
  location: { pathname: string };
  organization: Organization;
}

export default function OrganizationNavigationMenu({ location, organization }: Props) {
  return (
    <NavBarTabs className="navbar-context-tabs">
      <li>
        <Link to={`/organizations/${organization.key}/projects`} activeClassName="active">
          {translate('projects.page')}
        </Link>
      </li>
      <li>
        <Link
          to={{
            pathname: `/organizations/${organization.key}/issues`,
            query: { resolved: 'false' }
          }}
          activeClassName="active">
          {translate('issues.page')}
        </Link>
      </li>
      <li>
        <Link to={`/organizations/${organization.key}/quality_profiles`} activeClassName="active">
          {translate('quality_profiles.page')}
        </Link>
      </li>
      <li>
        <Link to={`/organizations/${organization.key}/rules`} activeClassName="active">
          {translate('coding_rules.page')}
        </Link>
      </li>
      <li>
        <Link to={getQualityGatesUrl(organization.key)} activeClassName="active">
          {translate('quality_gates.page')}
        </Link>
      </li>
      <li>
        <Link to={`/organizations/${organization.key}/members`} activeClassName="active">
          {translate('organization.members.page')}
        </Link>
      </li>
      <OrganizationNavigationExtensions location={location} organization={organization} />
      {organization.canAdmin && (
        <OrganizationNavigationAdministration location={location} organization={organization} />
      )}
    </NavBarTabs>
  );
}
