/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import OrganizationCard from './OrganizationCard';
import { Organization } from '../../../app/types';
import { translate } from '../../../helpers/l10n';

interface Props {
  organizations: Organization[];
}

export default function OrganizationsList({ organizations }: Props) {
  if (organizations.length === 0) {
    return <div>{translate('my_account.organizations.no_results')}</div>;
  }

  return (
    <ul className="account-projects-list">
      {sortBy(organizations, organization => organization.name.toLocaleLowerCase()).map(
        organization => (
          <li key={organization.key}>
            <OrganizationCard organization={organization} />
          </li>
        )
      )}
    </ul>
  );
}
