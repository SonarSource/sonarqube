/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import OrganizationNavigationHeader from './OrganizationNavigationHeader';
import OrganizationNavigationMenu from './OrganizationNavigationMenu';
import OrganizationNavigationMeta from './OrganizationNavigationMeta';
import { rawSizes } from '../../../app/theme';
import ContextNavBar from "../../../components/ui/ContextNavBar";
import { Organization } from "../../../types/types";

interface Props {
  location: { pathname: string };
  organization: Organization;
  userOrganizations: Organization[];
}

export default function OrganizationNavigation({
                                                 location,
                                                 organization,
                                                 userOrganizations
                                               }: Props) {

  const { contextNavHeightRaw } = rawSizes;

  return (
      <ContextNavBar height={contextNavHeightRaw} id="context-navigation">
        <div className="navbar-context-justified">
          <OrganizationNavigationHeader
              organization={organization}
              organizations={userOrganizations}
          />
          <OrganizationNavigationMeta
              organization={organization}
          />
        </div>
        <OrganizationNavigationMenu
            location={location}
            organization={organization}
        />
      </ContextNavBar>
  );
}
