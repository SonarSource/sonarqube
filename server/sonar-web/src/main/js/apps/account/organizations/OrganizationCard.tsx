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
import OrganizationAvatar from '../../../components/common/OrganizationAvatar';
import OrganizationLink from '../../../components/ui/OrganizationLink';
import { translate } from '../../../helpers/l10n';
import { Organization } from '../../../app/types';

interface Props {
  organization: Organization;
}

export default function OrganizationCard({ organization }: Props) {
  return (
    <div className="account-project-card clearfix">
      <aside className="account-project-side note">
        <strong>{translate('organization.key')}:</strong> {organization.key}
      </aside>

      <h3 className="account-project-name">
        <OrganizationAvatar organization={organization} />
        <OrganizationLink className="spacer-left text-middle" organization={organization}>
          {organization.name}
        </OrganizationLink>
        {organization.isAdmin && (
          <span className="outline-badge spacer-left">{translate('admin')}</span>
        )}
      </h3>

      {!!organization.description && (
        <div className="markdown spacer-top">{organization.description}</div>
      )}

      {!!organization.url && (
        <div className="markdown spacer-top">
          <a href={organization.url} title={organization.url} rel="nofollow">
            {organization.url}
          </a>
        </div>
      )}
    </div>
  );
}
