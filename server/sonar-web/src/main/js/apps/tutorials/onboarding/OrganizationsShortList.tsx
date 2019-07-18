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
import { sortBy } from 'lodash';
import * as React from 'react';
import { Link } from 'react-router';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import OrganizationsShortListItem from './OrganizationsShortListItem';

export interface Props {
  organizations: T.Organization[];
  onClick: VoidFunction;
}

export default function OrganizationsShortList({ onClick, organizations }: Props) {
  if (organizations.length === 0) {
    return null;
  }

  const organizationsShown = sortBy(organizations, organization =>
    organization.name.toLocaleLowerCase()
  ).slice(0, 3);

  return (
    <div>
      <ul>
        {organizationsShown.map(organization => (
          <li key={organization.key}>
            <OrganizationsShortListItem onClick={onClick} organization={organization} />
          </li>
        ))}
      </ul>
      {organizations.length > 3 && (
        <div className="big-spacer-top">
          <span className="big-spacer-right">
            {translateWithParameters(
              'x_of_y_shown',
              organizationsShown.length,
              organizations.length
            )}
          </span>
          <Link className="small" onClick={onClick} to="/account/organizations">
            {translate('see_all')}
          </Link>
        </div>
      )}
    </div>
  );
}
