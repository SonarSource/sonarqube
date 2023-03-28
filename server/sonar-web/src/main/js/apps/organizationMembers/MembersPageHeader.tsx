/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import AddMemberForm from './AddMemberForm';
import {Organization, OrganizationMember} from "../../types/types";
import {translate} from "../../helpers/l10n";
import Link from "../../components/common/Link";
import DeferredSpinner from "../../components/ui/DeferredSpinner";

export interface Props {
  handleAddMember: (member: OrganizationMember) => void;
  loading: boolean;
  members?: OrganizationMember[];
  organization: Organization;
}

export default function MembersPageHeader(props: Props) {
  const { members, organization } = props;
  const memberLogins = members ? members.map(member => member.login) : [];
  const isAdmin = organization.actions && organization.actions.admin;

  return (
    <header className="page-header">
      <h1 className="page-title">{translate('organization.members.page')}</h1>
      <DeferredSpinner loading={props.loading} />
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
      <div className="page-description">
        <FormattedMessage
          defaultMessage={translate('organization.members.page.description')}
          id="organization.members.page.description"
          values={{
            link: (
              <Link target="_blank" to="https://knowledgebase.autorabit.com/codescan/docs/add-users-to-a-codescan-cloud-organisation">
                {translate('organization.members.manage_a_team')}
              </Link>
            )
          }}
        />
      </div>
    </header>
  );
}
