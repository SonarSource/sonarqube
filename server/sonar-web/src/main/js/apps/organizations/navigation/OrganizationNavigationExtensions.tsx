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
import { DropdownMenu } from "@sonarsource/echoes-react";
import { NavBarTabLink } from "design-system";

interface Props {
  location: { pathname: string };
  organization: Organization;
}

export default function OrganizationNavigationExtensions({ location, organization }: Props) {
  let extensions = organization.pages || [];
  if (extensions.length === 0) {
    return null;
  }

  // removing request error extenstion link.
  extensions = extensions.filter((e) => {
    return e.name !== "Request Error"
  });

  return (
    <DropdownMenu.Root
      id="organization-nav-extensions"
      items={
        <>
          {extensions.map(extension => (
            <DropdownMenu.ItemLink
              key={extension.key}
              isMatchingFullPath
              to={`/organizations/${organization.kee}/extension/${extension.key}`}
            >
              {extension.name}
            </DropdownMenu.ItemLink>
          ))}

          <DropdownMenu.ItemLink
            key="policy-results"
            isMatchingFullPath
            to={`/organizations/${organization.kee}/policy-results`}
          >
            Policy Results
          </DropdownMenu.ItemLink>
        </>
      }
    >
      <NavBarTabLink preventDefault text={translate('more')} withChevron to={{}} />
    </DropdownMenu.Root>
  );
}
