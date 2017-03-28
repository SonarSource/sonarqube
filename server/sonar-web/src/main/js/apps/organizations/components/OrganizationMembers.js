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
import PageHeader from './PageHeader';
import MembersList from './MembersList';
import AddMemberForm from './forms/AddMemberForm';
import UsersSearch from '../../users/components/UsersSearch';
import ListFooter from '../../../components/controls/ListFooter';
import type { Organization } from '../../../store/organizations/duck';
import type { Member } from '../../../store/organizationsMembers/actions';

type Props = {
  members: Array<Member>,
  memberLogins: Array<string>,
  status: { loading?: boolean, total?: number, pageIndex?: number, query?: string },
  organization: Organization,
  fetchOrganizationMembers: (organizationKey: string, query?: string) => void,
  fetchMoreOrganizationMembers: (organizationKey: string, query?: string) => void,
  addOrganizationMember: (organizationKey: string, login: Member) => void,
  removeOrganizationMember: (organizationKey: string, login: Member) => void
};

export default class OrganizationMembers extends React.PureComponent {
  props: Props;

  componentDidMount() {
    const notLoadedYet = this.props.members.length < 1 || this.props.status.query != null;
    if (!this.props.loading && notLoadedYet) {
      this.handleSearchMembers();
    }
  }

  handleSearchMembers = (query?: string) => {
    this.props.fetchOrganizationMembers(this.props.organization.key, query);
  };

  handleLoadMoreMembers = () => {
    this.props.fetchMoreOrganizationMembers(this.props.organization.key, this.props.status.query);
  };

  addMember = (member: Member) => {
    this.props.addOrganizationMember(this.props.organization.key, member);
  };

  removeMember = (member: Member) => {
    this.props.removeOrganizationMember(this.props.organization.key, member);
  };

  render() {
    const { organization, status, members } = this.props;
    return (
      <div className="page page-limited">
        <PageHeader loading={status.loading} total={status.total}>
          {organization.canAdmin &&
            <div className="page-actions">
              <div className="button-group">
                <AddMemberForm memberLogins={this.props.memberLogins} addMember={this.addMember} />
              </div>
            </div>}
        </PageHeader>
        <UsersSearch onSearch={this.handleSearchMembers} />
        <MembersList
          members={members}
          organization={organization}
          removeMember={this.removeMember}
        />
        {status.total != null &&
          <ListFooter
            count={members.length}
            total={status.total}
            ready={!status.loading}
            loadMore={this.handleLoadMoreMembers}
          />}
      </div>
    );
  }
}
