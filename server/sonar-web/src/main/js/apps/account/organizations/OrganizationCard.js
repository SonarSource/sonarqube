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
import OrganizationLink from '../../../components/ui/OrganizationLink';
/*:: import type { Organization } from '../../../store/organizations/duck'; */

/*::
type Props = {
  organization: Organization
};
*/

export default function OrganizationCard(props /*: Props */) {
  const { organization } = props;

  return (
    <div className="account-project-card clearfix">
      <aside className="account-project-side">
        {!!organization.avatar &&
          <div className="spacer-bottom">
            <img src={organization.avatar} height={30} alt={organization.name} />
          </div>}
        {!!organization.url &&
          <div className="text-limited text-top spacer-bottom">
            <a className="small" href={organization.url} title={organization.url} rel="nofollow">
              {organization.url}
            </a>
          </div>}
      </aside>

      <h3 className="account-project-name">
        <OrganizationLink organization={organization}>
          {organization.name}
        </OrganizationLink>
      </h3>

      <div className="account-project-key">
        {organization.key}
      </div>

      {!!organization.description &&
        <div className="account-project-description">
          {organization.description}
        </div>}
    </div>
  );
}
