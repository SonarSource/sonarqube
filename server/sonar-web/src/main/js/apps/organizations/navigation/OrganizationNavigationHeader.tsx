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
import { Button, DropdownMenu, DropdownMenuAlign, IconChevronDown } from "@sonarsource/echoes-react";
import { NavBarTabLink, Popup, PopupPlacement, PopupZLevel, TopBar } from "design-system";
import FocusOutHandler from "../../../components/controls/FocusOutHandler";
import EscKeydownHandler from "../../../components/controls/EscKeydownHandler";
import OutsideClickHandler from "../../../components/controls/OutsideClickHandler";

export interface Props {
  organization: Organization;
  organizations: Organization[];
}

export default function OrganizationNavigationHeader({ organization, organizations }: Props) {

  const [isMenuOpen, setIsMenuOpen] = React.useState(false);

  const other = organizations.filter(o => o.kee !== organization.kee);

  console.info("isMenuOpen", isMenuOpen);

  return (
    <div>
      <div className="sw-flex sw-items-center">
        <OrganizationAvatar organization={organization}/>

        {other.length ? (
          <DropdownMenu.Root
            isOpen={isMenuOpen}
            align={DropdownMenuAlign.Start}
            className="sw-p-3"
            items={
              <>
                {sortBy(other, org => org.name.toLowerCase()).map(organization => (
                  <OrganizationListItem
                    key={organization.kee}
                    organization={organization}
                    onClick={() => setIsMenuOpen(false)}
                  />
                ))}
              </>
            }
          >
            <Button
              className="sw-max-w-abs-800 sw-px-3 sw-outline-none"
              onClick={() => {
                setIsMenuOpen(!isMenuOpen);
              }}
              isDisabled={organizations.length === 1}
              aria-expanded={isMenuOpen}
              aria-haspopup="menu"
              suffix={<IconChevronDown/>}
              style={{ border: "none" }}
            >
              {organization.name}
            </Button>
          </DropdownMenu.Root>
        ) : (
          <span className="spacer-left">{organization.name}</span>
        )}
      </div>

      {organization.description != null && (
        <div className="navbar-context-description">
          <p className="text-limited text-top" title={organization.description}>
            {organization.description}
          </p>
        </div>
      )}
    </div>
  );
}
