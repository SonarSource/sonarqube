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
import * as classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';

interface Props {
  location: { pathname: string };
  organization: T.Organization;
}

export default function OrganizationNavigationExtensions({ location, organization }: Props) {
  const extensions = organization.pages || [];
  if (extensions.length === 0) {
    return null;
  }
  const active = extensions.some(
    extension =>
      location.pathname === `/organizations/${organization.key}/extension/${extension.key}`
  );

  return (
    <Dropdown
      overlay={
        <ul className="menu">
          {extensions.map(extension => (
            <li key={extension.key}>
              <Link
                activeClassName="active"
                to={`/organizations/${organization.key}/extension/${extension.key}`}>
                {extension.name}
              </Link>
            </li>
          ))}
        </ul>
      }
      tagName="li">
      <a
        className={classNames('dropdown-toggle', { active })}
        href="#"
        id="organization-navigation-more">
        {translate('more')}
        <DropdownIcon className="little-spacer-left" />
      </a>
    </Dropdown>
  );
}
