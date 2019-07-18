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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../components/docs/DocTooltip';
import { isGithub, sanitizeAlmId } from '../../helpers/almIntegrations';
import AddMemberForm from './AddMemberForm';
import SyncMemberForm from './SyncMemberForm';

export interface Props {
  handleAddMember: (member: T.OrganizationMember) => void;
  loading: boolean;
  members?: T.OrganizationMember[];
  organization: T.Organization;
  refreshMembers: () => Promise<void>;
}

export default function MembersPageHeader(props: Props) {
  const { members, organization, refreshMembers } = props;
  const memberLogins = members ? members.map(member => member.login) : [];
  const isAdmin = organization.actions && organization.actions.admin;
  const almKey = organization.alm && sanitizeAlmId(organization.alm.key);
  const hasMemberSync = organization.alm && organization.alm.membersSync;
  const showSyncNotif = isAdmin && organization.alm && !hasMemberSync;
  const isSyncEligible =
    almKey && isGithub(almKey) && organization.alm && !organization.alm.personal;

  return (
    <header className="page-header">
      <h1 className="page-title">
        {translate('organization.members.page')}
        <DeferredSpinner className="little-spacer-left" loading={props.loading} />
      </h1>

      {isAdmin && (
        <div className="page-actions text-right">
          {isSyncEligible && !showSyncNotif && (
            <SyncMemberForm
              buttonText={translate('organization.members.config_synchro')}
              hasOtherMembers={members && members.length > 1}
              organization={organization}
              refreshMembers={refreshMembers}
            />
          )}
          {!hasMemberSync && (
            <div className="display-inline-block spacer-left spacer-bottom">
              <AddMemberForm
                addMember={props.handleAddMember}
                memberLogins={memberLogins}
                organization={organization}
              />
              <DocTooltip
                className="spacer-left"
                doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/organizations/add-organization-member.md')}
              />
            </div>
          )}
        </div>
      )}
      <div className="page-description">
        <FormattedMessage
          defaultMessage={translate('organization.members.page.description')}
          id="organization.members.page.description"
          values={{
            link: (
              <Link target="_blank" to="/documentation/organizations/manage-team/">
                {translate('organization.members.manage_a_team')}
              </Link>
            )
          }}
        />
        {almKey && isSyncEligible && showSyncNotif && (
          <Alert className="spacer-top" display="inline" variant="info">
            {translateWithParameters(
              'organization.members.auto_sync_members_from_org_x',
              translate('organization', almKey)
            )}
            <span className="spacer-left">
              <SyncMemberForm
                buttonText={translate('configure')}
                hasOtherMembers={members && members.length > 1}
                organization={organization}
                refreshMembers={refreshMembers}
              />
            </span>
          </Alert>
        )}
      </div>
    </header>
  );
}
