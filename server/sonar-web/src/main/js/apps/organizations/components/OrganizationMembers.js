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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import MembersPageHeader from './MembersPageHeader';
import MembersListHeader from './MembersListHeader';
import MembersList from './MembersList';
import AddMemberForm from './forms/AddMemberForm';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';
/*:: import type { Organization, OrgGroup } from '../../../store/organizations/duck'; */
/*:: import type { Member } from '../../../store/organizationsMembers/actions'; */

/*::
type Props = {
  members: Array<Member>,
  memberLogins: Array<string>,
  organizationGroups: Array<OrgGroup>,
  status: { loading?: boolean, total?: number, pageIndex?: number, query?: string },
  organization: Organization,
  fetchOrganizationMembers: (organizationKey: string, query?: string) => void,
  fetchMoreOrganizationMembers: (organizationKey: string, query?: string) => void,
  fetchOrganizationGroups: (organizationKey: string) => void,
  addOrganizationMember: (organizationKey: string, member: Member) => void,
  removeOrganizationMember: (organizationKey: string, member: Member) => void,
  updateOrganizationMemberGroups: (
    organization: Organization,
    member: Member,
    add: Array<string>,
    remove: Array<string>
  ) => void
};
*/

export default class OrganizationMembers extends React.PureComponent {
  /*:: props: Props; */

  componentDidMount() {
    this.handleSearchMembers();
    if (this.props.organization.canAdmin) {
      this.props.fetchOrganizationGroups(this.props.organization.key);
    }
  }

  handleSearchMembers = (query /*: string | void */) => {
    this.props.fetchOrganizationMembers(this.props.organization.key, query);
  };

  handleLoadMoreMembers = () => {
    this.props.fetchMoreOrganizationMembers(this.props.organization.key, this.props.status.query);
  };

  addMember = (member /*: Member */) => {
    this.props.addOrganizationMember(this.props.organization.key, member);
  };

  removeMember = (member /*: Member */) => {
    this.props.removeOrganizationMember(this.props.organization.key, member);
  };

  updateMemberGroups = (
    member /*: Member */,
    add /*: Array<string> */,
    remove /*: Array<string> */
  ) => {
    this.props.updateOrganizationMemberGroups(this.props.organization, member, add, remove);
  };

  render() {
    const { organization, status, members } = this.props;
    return (
      <div className="page page-limited">
        <Helmet title={translate('organization.members.page')} />
        <MembersPageHeader loading={status.loading} total={status.total}>
          {organization.canAdmin && (
            <div className="page-actions">
              <AddMemberForm
                addMember={this.addMember}
                organization={organization}
                memberLogins={this.props.memberLogins}
              />
            </div>
          )}
        </MembersPageHeader>
        <MembersListHeader total={status.total} handleSearch={this.handleSearchMembers} />
        <MembersList
          members={members}
          organizationGroups={this.props.organizationGroups}
          organization={organization}
          removeMember={this.removeMember}
          updateMemberGroups={this.updateMemberGroups}
        />
        {status.total != null && (
          <ListFooter
            count={members.length}
            total={status.total}
            ready={!status.loading}
            loadMore={this.handleLoadMoreMembers}
          />
        )}
      </div>
    );
  }
}
