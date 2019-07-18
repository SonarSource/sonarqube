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
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import OrganizationAvatar from '../../../components/common/OrganizationAvatar';
import OrganizationListItem from '../../../components/ui/OrganizationListItem';
import {
  getUserAlmKey,
  hasAdvancedALMIntegration,
  sanitizeAlmId
} from '../../../helpers/almIntegrations';

export interface Props {
  currentUser: T.CurrentUser;
  organization: T.Organization;
  organizations: T.Organization[];
}

export default function OrganizationNavigationHeader({
  currentUser,
  organization,
  organizations
}: Props) {
  const other = organizations.filter(o => o.key !== organization.key);
  const isAdmin = organization.actions && organization.actions.admin;

  let almKey;
  let tooltipContent;
  let tooltipIconSrc;
  if (organization.alm) {
    almKey = sanitizeAlmId(organization.alm.key);
    tooltipContent = (
      <>
        <p>{translateWithParameters('organization.bound_to_x', translate(almKey))}</p>
        <hr className="spacer-top spacer-bottom" />
        <a href={organization.alm.url} rel="noopener noreferrer" target="_blank">
          {translateWithParameters('organization.see_on_x', translate(almKey))}
        </a>
      </>
    );
    tooltipIconSrc = `${getBaseUrl()}/images/sonarcloud/${almKey}.svg`;
  } else if (hasAdvancedALMIntegration(currentUser)) {
    almKey = getUserAlmKey(currentUser) || '';
    tooltipContent = (
      <>
        <p>{translateWithParameters('organization.not_bound_to_x', translate(almKey))}</p>
        {isAdmin && (
          <>
            <hr className="spacer-top spacer-bottom" />
            <Link to={`/organizations/${organization.key}/edit`}>
              {translate('organization.go_to_settings_to_bind')}
            </Link>
          </>
        )}
      </>
    );
    tooltipIconSrc = `${getBaseUrl()}/images/sonarcloud/${almKey}-unbound.svg`;
  }

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
      {almKey && (
        <Tooltip mouseLeaveDelay={0.25} overlay={tooltipContent}>
          <img
            alt={translate(almKey)}
            className="text-middle spacer-left"
            height={16}
            src={tooltipIconSrc}
            width={16}
          />
        </Tooltip>
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
