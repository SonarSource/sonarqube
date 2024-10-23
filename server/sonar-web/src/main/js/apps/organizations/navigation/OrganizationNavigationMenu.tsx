/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import OrganizationNavigationExtensions from './OrganizationNavigationExtensions';
import OrganizationNavigationAdministration from './OrganizationNavigationAdministration';
import { getQualityGatesUrl } from '../../../helpers/urls';
import { translate } from "../../../helpers/l10n";
import { Organization } from "../../../types/types";
import { NavBarTabLink, NavBarTabs } from "design-system";

interface OwnProps {
  location: { pathname: string };
  organization: Organization;
}

export default function OrganizationNavigationMenu({ location, organization }: OwnProps) {
  const { actions = {} } = organization;
  return (
    <NavBarTabs>
      <NavBarTabLink to={`/organizations/${organization.kee}/projects`} text={translate('projects')} />
      <NavBarTabLink
          to={{
            pathname: `/organizations/${organization.kee}/issues`,
            search: new URLSearchParams({ resolved: 'false' }).toString()
          }}
          text={translate('issues.facet.mode.count')}
      />
      <NavBarTabLink to={`/organizations/${organization.kee}/quality_profiles`} text={translate('quality_profiles.page')} />
      <NavBarTabLink to={`/organizations/${organization.kee}/rules`} text={translate('coding_rules.page')} />
      <NavBarTabLink to={getQualityGatesUrl(organization.kee)} text={translate('quality_gates.page')} />
      <NavBarTabLink to={`/organizations/${organization.kee}/members`} text={translate('organization.members.page')} />
      <OrganizationNavigationExtensions location={location} organization={organization}/>
      {actions.admin && (
        <OrganizationNavigationAdministration location={location} organization={organization}/>
      )}
    </NavBarTabs>
  );
}
