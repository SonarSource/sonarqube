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
import { NavLink } from "react-router-dom";
import { NavBarTabs } from "design-system";

interface OwnProps {
  location: { pathname: string };
  organization: Organization;
}

export default function OrganizationNavigationMenu({ location, organization }: OwnProps) {
  const { actions = {} } = organization;
  return (
      <NavBarTabs className="navbar-context-tabs">
        <li>
          <NavLink to={`/organizations/${organization.kee}/projects`}>
            {translate('projects')}
          </NavLink>
        </li>
        <li>
          <NavLink
              to={{
                pathname: `/organizations/${organization.kee}/issues`,
                search: new URLSearchParams({ resolved: 'false' }).toString()
              }}>
            {translate('issues.facet.mode.count')}
          </NavLink>
        </li>
        <li>
          <NavLink to={`/organizations/${organization.kee}/quality_profiles`}>
            {translate('quality_profiles.page')}
          </NavLink>
        </li>
        <li>
          <NavLink to={`/organizations/${organization.kee}/rules`}>
            {translate('coding_rules.page')}
          </NavLink>
        </li>
        <li>
          <NavLink to={getQualityGatesUrl(organization.kee)}>
            {translate('quality_gates.page')}
          </NavLink>
        </li>
        <li>
          <NavLink to={`/organizations/${organization.kee}/members`}>
            {translate('organization.members.page')}
          </NavLink>
        </li>
        <OrganizationNavigationExtensions location={location} organization={organization}/>
        {actions.admin && (
          <OrganizationNavigationAdministration location={location} organization={organization}/>
        )}
      </NavBarTabs>
  );
}
