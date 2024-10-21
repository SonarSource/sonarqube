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
import { translate } from "../../../helpers/l10n";
import { Organization } from "../../../types/types";
import { DropdownMenu, IconChevronDown } from "@sonarsource/echoes-react";
import { NavBarTabLink } from "design-system";

interface Props {
  location: { pathname: string };
  organization: Organization;
}

const ADMIN_PATHS = [
  'edit',
  'groups',
  'permissions',
  'permission_templates',
  'projects_management',
  'webhooks'
];

export default function OrganizationNavigationAdministration({ location, organization }: Props) {
  const { adminPages = [] } = organization;
  const adminPathsWithExtensions = adminPages.map(e => `extension/${e.key}`).concat(ADMIN_PATHS);
  const adminActive = adminPathsWithExtensions.some(path =>
    location.pathname.endsWith(`organizations/${organization.kee}/${path}`)
  );

  return (
    <DropdownMenu.Root
      id="organization-nav"
      items={
        <>
          <DropdownMenu.ItemLink
            isMatchingFullPath
            to={`/organizations/${organization.kee}/edit`}
          >
            {translate('organization.settings')}
          </DropdownMenu.ItemLink>

          {adminPages.map(extension => (
            <DropdownMenu.ItemLink
              isMatchingFullPath
              to={`/organizations/${organization.kee}/extension/${extension.key}`}
              key={extension.key}
            >
              {extension.name}
            </DropdownMenu.ItemLink>
          ))}

          <DropdownMenu.ItemLink
            isMatchingFullPath
            to={`/organizations/${organization.kee}/groups`}
          >
            {translate('user_groups.page')}
          </DropdownMenu.ItemLink>

          <DropdownMenu.ItemLink
            isMatchingFullPath
            to={`/organizations/${organization.kee}/permissions`}
          >
            {translate('permissions.page')}
          </DropdownMenu.ItemLink>

          <DropdownMenu.ItemLink
            isMatchingFullPath
            to={`/organizations/${organization.kee}/permission_templates`}
          >
            {translate('permission_templates')}
          </DropdownMenu.ItemLink>

          <DropdownMenu.ItemLink
            isMatchingFullPath
            to={`/organizations/${organization.kee}/projects_management`}
          >
            {translate('projects_management')}
          </DropdownMenu.ItemLink>

          <DropdownMenu.ItemLink
            isMatchingFullPath
            to={`/organizations/${organization.kee}/webhooks`}
          >
            {translate('webhooks.page')}
          </DropdownMenu.ItemLink>
        </>
      }
    >
      <NavBarTabLink preventDefault text={translate('more')} withChevron to={{}} style={{fontSize: "14px!important", padding: 0}}/>
    </DropdownMenu.Root>
  );
}
