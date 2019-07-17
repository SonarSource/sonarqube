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
import { translate } from 'sonar-ui-common/helpers/l10n';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import DocTooltip from '../../../components/docs/DocTooltip';
import { hasPrivateAccess, isPaidOrganization } from '../../../helpers/organizations';
import { isSonarCloud } from '../../../helpers/system';

interface Props {
  currentUser: T.CurrentUser;
  organization: T.Organization;
  userOrganizations: T.Organization[];
}

export default function OrganizationNavigationMeta({
  currentUser,
  organization,
  userOrganizations
}: Props) {
  const onSonarCloud = isSonarCloud();
  return (
    <div className="navbar-context-meta">
      {organization.url != null && (
        <a
          className="spacer-right text-limited"
          href={organization.url}
          rel="nofollow"
          title={organization.url}>
          {organization.url}
        </a>
      )}
      {onSonarCloud &&
        isPaidOrganization(organization) &&
        hasPrivateAccess(currentUser, organization, userOrganizations) && (
          <DocTooltip
            className="spacer-right"
            doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/organizations/subscription-paid-plan.md')}>
            <div className="badge">{translate('organization.paid_plan.badge')}</div>
          </DocTooltip>
        )}
      <div className="text-muted">
        <strong>{translate('organization.key')}:</strong> {organization.key}
      </div>
      {onSonarCloud && (
        <div className="navbar-context-meta-secondary">
          <HomePageSelect currentPage={{ type: 'ORGANIZATION', organization: organization.key }} />
        </div>
      )}
    </div>
  );
}
