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
import { sortBy } from 'lodash';
import OrganizationAvatar from '../../../components/common/OrganizationAvatar';
import Dropdown from '../../../components/controls/Dropdown';
import DropdownIcon from '../../../components/icons-components/DropdownIcon';
import OrganizationListItem from '../../../components/ui/OrganizationListItem';
import { sanitizeAlmId } from '../../../helpers/almIntegrations';
import { getBaseUrl } from '../../../helpers/urls';

interface Props {
  organization: T.Organization;
  organizations: T.Organization[];
}

export default function OrganizationNavigationHeader({ organization, organizations }: Props) {
  const other = organizations.filter(o => o.key !== organization.key);

  return (
    <header className="navbar-context-header">
      <OrganizationAvatar organization={organization} />
      {other.length ? (
        <Dropdown
          className="display-inline-block"
          overlay={
            <ul className="menu">
              {sortBy(other, org => org.name.toLowerCase()).map(organization => (
                <OrganizationListItem key={organization.key} organization={organization} />
              ))}
            </ul>
          }>
          <a
            className="display-inline-flex-center spacer-left link-base-color link-no-underline"
            href="#">
            {organization.name}
            <DropdownIcon className="little-spacer-left" />
          </a>
        </Dropdown>
      ) : (
        <span className="spacer-left">{organization.name}</span>
      )}
      {organization.alm && (
        <a
          className="link-no-underline"
          href={organization.alm.url}
          rel="noopener noreferrer"
          target="_blank">
          <img
            alt={sanitizeAlmId(organization.alm.key)}
            className="text-text-top spacer-left"
            height={16}
            src={`${getBaseUrl()}/images/sonarcloud/${sanitizeAlmId(organization.alm.key)}.svg`}
            width={16}
          />
        </a>
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
