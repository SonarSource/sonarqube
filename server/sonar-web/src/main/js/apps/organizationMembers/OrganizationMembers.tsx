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
import Helmet from 'react-helmet';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { addMember, removeMember, searchMembers } from '../../api/organizations';
import { addUserToGroup, removeUserFromGroup, searchUsersGroups } from '../../api/user_groups';
import A11ySkipTarget from '../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../app/components/embed-docs-modal/Suggestions';
import MembersList from './MembersList';
import MembersListHeader from './MembersListHeader';
import MembersPageHeader from './MembersPageHeader';

interface Props {
  currentUser: T.LoggedInUser;
  organization: T.Organization;
}

interface State {
  groups: T.Group[];
  loading: boolean;
  members?: T.OrganizationMember[];
  paging?: T.Paging;
  query: string;
}

const PAGE_SIZE = 50;

export default class OrganizationMembers extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    groups: [],
    loading: true,
    query: ''
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchMembers();
    if (this.props.organization.actions && this.props.organization.actions.admin) {
      this.fetchGroups();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  fetchMembers = (query?: string) => {
    this.setState({ loading: true });
    searchMembers({
      organization: this.props.organization.key,
      ps: PAGE_SIZE,
      q: query
    }).then(({ paging, users }) => {
      if (this.mounted) {
        this.setState({ loading: false, members: users, paging });
      }
    }, this.stopLoading);
  };

  fetchGroups = () => {
    searchUsersGroups({ organization: this.props.organization.key }).then(
      ({ groups }) => {
        if (this.mounted) {
          this.setState({ groups });
        }
      },
      () => {}
    );
  };

  handleSearchMembers = (query: string) => {
    this.setState({ query });
    this.fetchMembers(query || undefined); // empty string -> undefined
  };

  handleLoadMoreMembers = () => {
    const { paging, query } = this.state;
    if (!paging) {
      return;
    }

    this.setState({ loading: true });
    searchMembers({
      organization: this.props.organization.key,
      p: paging.pageIndex + 1,
      ps: PAGE_SIZE,
      q: query || undefined // empty string -> undefined
    }).then(({ paging, users }) => {
      if (this.mounted) {
        this.setState(({ members = [] }) => ({
          loading: false,
          members: [...members, ...users],
          paging
        }));
      }
    }, this.stopLoading);
  };

  handleAddMember = ({ login }: T.OrganizationMember) => {
    // TODO optimistic update
    addMember({ login, organization: this.props.organization.key }).then(
      member => {
        if (this.mounted) {
          this.setState(({ members, paging }) => ({
            members: members && [...members, member],
            paging: paging && { ...paging, total: paging.total + 1 }
          }));
        }
      },
      () => {}
    );
  };

  handleRemoveMember = ({ login }: T.OrganizationMember) => {
    // TODO optimistic update
    removeMember({ login, organization: this.props.organization.key }).then(
      () => {
        if (this.mounted) {
          this.setState(({ members, paging }) => ({
            members: members && members.filter(member => member.login !== login),
            paging: paging && { ...paging, total: paging.total - 1 }
          }));
        }
      },
      () => {}
    );
  };

  refreshMembers = () => {
    return searchMembers({
      organization: this.props.organization.key,
      ps: PAGE_SIZE,
      q: this.state.query || undefined
    }).then(({ paging, users }) => {
      if (this.mounted) {
        this.setState({ members: users, paging });
      }
    });
  };

  updateGroup = (
    login: string,
    updater: (member: T.OrganizationMember) => T.OrganizationMember
  ) => {
    this.setState(({ members }) => ({
      members: members && members.map(member => (member.login === login ? updater(member) : member))
    }));
  };

  updateMemberGroups = ({ login }: T.OrganizationMember, add: string[], remove: string[]) => {
    // TODO optimistic update
    const promises = [
      ...add.map(name =>
        addUserToGroup({ name, login, organization: this.props.organization.key })
      ),
      ...remove.map(name =>
        removeUserFromGroup({ name, login, organization: this.props.organization.key })
      )
    ];
    return Promise.all(promises).then(() => {
      if (this.mounted) {
        this.updateGroup(login, member => ({
          ...member,
          groupCount: (member.groupCount || 0) + add.length - remove.length
        }));
      }
    });
  };

  render() {
    const { currentUser, organization } = this.props;
    const { groups, loading, members, paging } = this.state;
    const hasMemberSync = organization.alm && organization.alm.membersSync;
    return (
      <div className="page page-limited">
        <Helmet title={translate('organization.members.page')} />
        <Suggestions suggestions="organization_members" />
        <A11ySkipTarget anchor="members_main" />
        <MembersPageHeader
          handleAddMember={this.handleAddMember}
          loading={loading}
          members={members}
          organization={organization}
          refreshMembers={this.refreshMembers}
        />
        {members !== undefined && paging !== undefined && (
          <>
            <MembersListHeader
              handleSearch={this.handleSearchMembers}
              organization={organization}
              total={paging.total}
            />
            <MembersList
              currentUser={currentUser}
              members={members}
              organization={organization}
              organizationGroups={groups}
              removeMember={hasMemberSync ? undefined : this.handleRemoveMember}
              updateMemberGroups={this.updateMemberGroups}
            />
            {paging.total !== 0 && (
              <ListFooter
                count={members.length}
                loadMore={this.handleLoadMoreMembers}
                ready={!loading}
                total={paging.total}
              />
            )}
          </>
        )}
      </div>
    );
  }
}
