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
import { connect } from 'react-redux';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import AddMemberForm from './AddMemberForm';
import SyncMemberForm from './SyncMemberForm';
import DeferredSpinner from '../../components/common/DeferredSpinner';
import DocTooltip from '../../components/docs/DocTooltip';
import NewInfoBox from '../../components/ui/NewInfoBox';
import { sanitizeAlmId } from '../../helpers/almIntegrations';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getCurrentUserSetting, Store } from '../../store/rootReducer';
import { setCurrentUserSetting } from '../../store/users';

interface Props {
  dismissSyncNotifOrg: string[];
  handleAddMember: (member: T.OrganizationMember) => void;
  loading: boolean;
  members?: T.OrganizationMember[];
  organization: T.Organization;
  setCurrentUserSetting: (setting: T.CurrentUserSetting) => void;
}

export class MembersPageHeader extends React.PureComponent<Props> {
  handleDismissSyncNotif = () => {
    const { dismissSyncNotifOrg, organization } = this.props;
    this.props.setCurrentUserSetting({
      key: 'organizations.members.dismissSyncNotif',
      value: [...dismissSyncNotifOrg, organization.key].join(',')
    });
  };

  render() {
    const { dismissSyncNotifOrg, members, organization } = this.props;
    const memberLogins = members ? members.map(member => member.login) : [];
    const isAdmin = organization.actions && organization.actions.admin;
    const almKey = organization.alm && sanitizeAlmId(organization.alm.key);
    const hasMemberSync = organization.alm && organization.alm.membersSync;
    const showSyncNotif =
      isAdmin &&
      organization.alm &&
      !hasMemberSync &&
      !dismissSyncNotifOrg.some(orgKey => orgKey === organization.key);

    return (
      <header className="page-header">
        <h1 className="page-title">{translate('organization.members.page')}</h1>
        <DeferredSpinner loading={this.props.loading} />
        {isAdmin && (
          <div className="page-actions text-right">
            {almKey && !showSyncNotif && <SyncMemberForm organization={organization} />}
            {!hasMemberSync && (
              <div className="display-inline-block spacer-left spacer-bottom">
                <AddMemberForm
                  addMember={this.props.handleAddMember}
                  memberLogins={memberLogins}
                  organization={organization}
                />
                <DocTooltip
                  className="spacer-left"
                  doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/organizations/add-organization-member.md')}
                />
              </div>
            )}
            {almKey &&
              showSyncNotif && (
                <NewInfoBox
                  description={translateWithParameters(
                    'organization.members.auto_sync_members_from_org_x',
                    translate(almKey)
                  )}
                  onClose={this.handleDismissSyncNotif}
                  title={translateWithParameters(
                    'organization.members.auto_sync_with_x',
                    translate(almKey)
                  )}>
                  <SyncMemberForm organization={organization} />
                </NewInfoBox>
              )}
          </div>
        )}
        <div className="page-description">
          <FormattedMessage
            defaultMessage={translate('organization.members.page.description')}
            id="organization.members.page.description"
            values={{
              link: (
                <Link to="/documentation/organizations/manage-team/">
                  {translate('organization.members.manage_a_team')}
                </Link>
              )
            }}
          />
        </div>
      </header>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  dismissSyncNotifOrg: (
    getCurrentUserSetting(state, 'organizations.members.dismissSyncNotif') || ''
  ).split(',')
});

const mapDispatchToProps = { setCurrentUserSetting };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(MembersPageHeader);
