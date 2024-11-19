/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { Link, Spinner } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../helpers/l10n';
import { Organization, OrganizationMember } from '../../types/types';
import AddMemberForm from './AddMemberForm';

export interface Props {
  handleAddMember: (member: OrganizationMember) => void;
  loading: boolean;
  members?: OrganizationMember[];
  organization: Organization;
}

export default function MembersPageHeader(props: Props) {
  const { members, organization } = props;
  const memberLogins = members ? members.map((member) => member.login) : [];
  const isAdmin = organization.actions && organization.actions.admin;

  return (
    <header className="page-header sw-mt-16 sw-ml-16 sw-mr-8">
      <div className="measure-one-line">
        <div className="sw-flex sw-items-center">
          <h2 className="page-title">{translate('organization.members.page')}</h2>
          <Spinner isLoading={props.loading} />
        </div>
        <div>
          {isAdmin && (
            <div className="page-actions text-right">
              <div className="display-inline-block spacer-left spacer-bottom">
                <AddMemberForm
                  addMember={props.handleAddMember}
                  memberLogins={memberLogins}
                  organization={organization}
                />
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="page-description">
        <FormattedMessage
          defaultMessage={translate('organization.members.page.description')}
          id="organization.members.page.description"
          values={{
            link: (
              <Link
                target="_blank"
                to="https://knowledgebase.autorabit.com/codescan/docs/add-users-to-a-codescan-cloud-organisation"
              >
                {translate('organization.members.manage_a_team')}
              </Link>
            ),
          }}
        />
      </div>
    </header>
  );
}
