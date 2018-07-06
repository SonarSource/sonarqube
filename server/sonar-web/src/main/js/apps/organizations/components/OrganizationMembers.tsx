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
import Helmet from 'react-helmet';
import MembersPageHeader from './MembersPageHeader';
import MembersListHeader from './MembersListHeader';
import MembersList from './MembersList';
import AddMemberForm from './forms/AddMemberForm';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import ListFooter from '../../../components/controls/ListFooter';
import DocTooltip from '../../../components/docs/DocTooltip';
import { translate } from '../../../helpers/l10n';
import { Group, Organization, OrganizationMember } from '../../../app/types';

interface Props {
  addOrganizationMember: (organizationKey: string, member: OrganizationMember) => void;
  fetchMoreOrganizationMembers: (organizationKey: string, query?: string) => void;
  fetchOrganizationGroups: (organizationKey: string) => void;
  fetchOrganizationMembers: (organizationKey: string, query?: string) => void;
  members: OrganizationMember[];
  memberLogins: string[];
  organization: Organization;
  organizationGroups: Group[];
  removeOrganizationMember: (organizationKey: string, member: OrganizationMember) => void;
  status: { loading?: boolean; total?: number; pageIndex?: number; query?: string };
  updateOrganizationMemberGroups: (
    organization: Organization,
    member: OrganizationMember,
    add: string[],
    remove: string[]
  ) => void;
}

export default class OrganizationMembers extends React.PureComponent<Props> {
  componentDidMount() {
    this.handleSearchMembers();
    if (this.props.organization.canAdmin) {
      this.props.fetchOrganizationGroups(this.props.organization.key);
    }
  }

  handleSearchMembers = (query?: string) => {
    this.props.fetchOrganizationMembers(this.props.organization.key, query);
  };

  handleLoadMoreMembers = () => {
    this.props.fetchMoreOrganizationMembers(this.props.organization.key, this.props.status.query);
  };

  addMember = (member: OrganizationMember) => {
    this.props.addOrganizationMember(this.props.organization.key, member);
  };

  removeMember = (member: OrganizationMember) => {
    this.props.removeOrganizationMember(this.props.organization.key, member);
  };

  updateMemberGroups = (member: OrganizationMember, add: string[], remove: string[]) => {
    this.props.updateOrganizationMemberGroups(this.props.organization, member, add, remove);
  };

  render() {
    const { organization, status, members } = this.props;
    return (
      <div className="page page-limited">
        <Helmet title={translate('organization.members.page')} />
        <Suggestions suggestions="organization_members" />
        <MembersPageHeader loading={Boolean(status.loading)}>
          {organization.canAdmin && (
            <div className="page-actions">
              <AddMemberForm
                addMember={this.addMember}
                memberLogins={this.props.memberLogins}
                organization={organization}
              />
              <DocTooltip className="spacer-left" doc="organizations/add-organization-member" />
            </div>
          )}
        </MembersPageHeader>
        <MembersListHeader handleSearch={this.handleSearchMembers} total={status.total} />
        <MembersList
          members={members}
          organization={organization}
          organizationGroups={this.props.organizationGroups}
          removeMember={this.removeMember}
          updateMemberGroups={this.updateMemberGroups}
        />
        {status.total != null && (
          <ListFooter
            count={members.length}
            loadMore={this.handleLoadMoreMembers}
            ready={!status.loading}
            total={status.total}
          />
        )}
      </div>
    );
  }
}
