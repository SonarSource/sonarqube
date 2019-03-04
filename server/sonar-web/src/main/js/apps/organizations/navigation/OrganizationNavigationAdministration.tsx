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
import * as React from 'react';
import { Link } from 'react-router';
import * as classNames from 'classnames';
import { translate } from '../../../helpers/l10n';
import Dropdown from '../../../components/controls/Dropdown';
import DropdownIcon from '../../../components/icons-components/DropdownIcon';

interface Props {
  location: { pathname: string };
  organization: T.Organization;
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
    location.pathname.endsWith(`organizations/${organization.key}/${path}`)
  );

  return (
    <Dropdown
      overlay={
        <ul className="menu">
          <li>
            <Link activeClassName="active" to={`/organizations/${organization.key}/edit`}>
              {translate('organization.settings')}
            </Link>
          </li>
          {adminPages.map(extension => (
            <li key={extension.key}>
              <Link
                activeClassName="active"
                to={`/organizations/${organization.key}/extension/${extension.key}`}>
                {extension.name}
              </Link>
            </li>
          ))}
          <li>
            <Link activeClassName="active" to={`/organizations/${organization.key}/groups`}>
              {translate('user_groups.page')}
            </Link>
          </li>
          <li>
            <Link activeClassName="active" to={`/organizations/${organization.key}/permissions`}>
              {translate('permissions.page')}
            </Link>
          </li>
          <li>
            <Link
              activeClassName="active"
              to={`/organizations/${organization.key}/permission_templates`}>
              {translate('permission_templates')}
            </Link>
          </li>
          <li>
            <Link
              activeClassName="active"
              to={`/organizations/${organization.key}/projects_management`}>
              {translate('projects_management')}
            </Link>
          </li>
          <li>
            <Link activeClassName="active" to={`/organizations/${organization.key}/webhooks`}>
              {translate('webhooks.page')}
            </Link>
          </li>
        </ul>
      }
      tagName="li">
      <a
        className={classNames('dropdown-toggle', { active: adminActive })}
        href="#"
        id="organization-navigation-admin">
        {translate('layout.settings')}
        <DropdownIcon className="little-spacer-left" />
      </a>
    </Dropdown>
  );
}
