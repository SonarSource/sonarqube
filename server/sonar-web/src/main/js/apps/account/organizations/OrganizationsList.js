/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { sortBy } from 'lodash';
import OrganizationCard from './OrganizationCard';
import type { Organization } from '../../../store/organizations/duck';

type Props = {
  organizations: Array<Organization>
};

export default function OrganizationsList(props: Props) {
  return (
    <ul className="account-projects-list">
      {sortBy(props.organizations, organization =>
        organization.name.toLocaleLowerCase()
      ).map(organization => (
        <li key={organization.key}>
          <OrganizationCard organization={organization} />
        </li>
      ))}
    </ul>
  );
}
