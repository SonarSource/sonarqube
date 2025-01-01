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

import { DropdownMenu } from '@sonarsource/echoes-react';
import { translate } from '../../../../helpers/l10n';
import { sortBy } from "lodash";
import OrganizationListItem from "../../../../apps/organizations/components/OrganizationListItem";
import { Organization } from "../../../../types/types";

type GlobalNavUserMenuProps = {
  userOrganizations: Organization[];
}

export function GlobalNavUserMenu({ userOrganizations }: GlobalNavUserMenuProps) {
  return (
    <>
      <DropdownMenu.ItemLink isMatchingFullPath to="/account">
        {translate('my_account.page')}
      </DropdownMenu.ItemLink>

      <DropdownMenu.Separator/>

      <DropdownMenu.ItemLink to="/account/organizations">
        {translate('my_organizations')}
      </DropdownMenu.ItemLink>

      {sortBy(userOrganizations, org => org.name.toLowerCase()).map(organization => (
        <DropdownMenu.ItemButton key={organization.kee}>
          <OrganizationListItem organization={organization} />
        </DropdownMenu.ItemButton>
      ))}

      <DropdownMenu.Separator/>

      <DropdownMenu.ItemLink to="/sessions/logout">
        {translate('layout.logout')}
      </DropdownMenu.ItemLink>
    </>
  );
}
