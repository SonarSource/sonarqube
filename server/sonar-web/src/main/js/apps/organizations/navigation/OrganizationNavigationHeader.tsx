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
import { sortBy } from 'lodash';
import { Organization } from "../../../types/types";
import OrganizationAvatar from "../components/OrganizationAvatar";
import OrganizationListItem from "../components/OrganizationListItem";
import { DropdownMenu } from "@sonarsource/echoes-react";
import { NavBarTabLink } from "design-system";

export interface Props {
  organization: Organization;
  organizations: Organization[];
}

export default function OrganizationNavigationHeader({ organization, organizations }: Props) {
  const other = organizations.filter(o => o.kee !== organization.kee);

  return (
      <header className="navbar-context-header">
        <OrganizationAvatar organization={organization}/>
        {other.length ? (
          <DropdownMenu.Root
            items={
              <>
                {sortBy(other, org => org.name.toLowerCase()).map(organization => (
                  <DropdownMenu.ItemLink isMatchingFullPath to="/admin/settings">
                    <OrganizationListItem key={organization.kee} organization={organization}/>
                  </DropdownMenu.ItemLink>
                ))}
              </>
            }
          >
            <NavBarTabLink
              aria-haspopup="menu"
              active={this.isSecurityActive()}
              to={{}}
              text={organization.name}
              withChevron
            />
          </DropdownMenu.Root>
        ) : (
          <span className="spacer-left">{organization.name}</span>
        )}
        {organization.description != null && (
          <div className="navbar-context-description">
            <p className="text-limited text-top" title={organization.description}>
              {organization.description}
            </p>
          </div>
        )}
      </header>
  );
}
