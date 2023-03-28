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
import { Helmet } from 'react-helmet-async';
import MembersPageHeader from './MembersPageHeader';
import MembersListHeader from './MembersListHeader';
import MembersList from './MembersList';
import { searchUsersGroups, addUserToGroup, removeUserFromGroup } from '../../api/user_groups';
import Suggestions from "../../components/embed-docs-modal/Suggestions";
import {translate} from "../../helpers/l10n";
import ListFooter from "../../components/controls/ListFooter";
import {addMember, removeMember, searchMembers} from "../../api/organizations";
import {Group, Organization, OrganizationMember, Paging} from "../../types/types";
import {LoggedInUser} from "../../types/users";
import { withOrganizationContext } from "../organizations/OrganizationContext";
import withCurrentUserContext from "../../app/components/current-user/withCurrentUserContext";

interface Props {
  currentUser: LoggedInUser;
  organization: Organization;
}

interface State {
  groups: Group[];
  loading: boolean;
  members?: OrganizationMember[];
  paging?: Paging;
  query: string;
}

const PAGE_SIZE = 50;

class OrganizationMembers extends React.PureComponent<Props, State> {
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
      organization: this.props.organization.kee,
      ps: PAGE_SIZE,
      q: query
    }).then(({ paging, users }) => {
      if (this.mounted) {
        this.setState({ loading: false, members: users, paging });
      }
    }, this.stopLoading);
  };

  fetchGroups = () => {
    searchUsersGroups({ organization: this.props.organization.kee }).then(
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
      organization: this.props.organization.kee,
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

  handleAddMember = ({ login }: OrganizationMember) => {
    // TODO optimistic update
    addMember({ login, organization: this.props.organization.kee }).then(
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

  handleRemoveMember = ({ login }: OrganizationMember) => {
    // TODO optimistic update
    removeMember({ login, organization: this.props.organization.kee }).then(
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

  updateGroup = (
    login: string,
    updater: (member: OrganizationMember) => OrganizationMember
  ) => {
    this.setState(({ members }) => ({
      members: members && members.map(member => (member.login === login ? updater(member) : member))
    }));
  };

  updateMemberGroups = ({ login }: OrganizationMember, add: string[], remove: string[]) => {
    // TODO optimistic update
    const promises = [
      ...add.map(name =>
        addUserToGroup({ name, login, organization: this.props.organization.kee })
      ),
      ...remove.map(name =>
        removeUserFromGroup({ name, login, organization: this.props.organization.kee })
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
    return (
      <div className="page page-limited">
        <Helmet title={translate('organization.members.page')} />
        <Suggestions suggestions="organization_members" />
        <MembersPageHeader
          handleAddMember={this.handleAddMember}
          loading={loading}
          members={members}
          organization={organization}
        />
        {members !== undefined &&
          paging !== undefined && (
            <>
              <MembersListHeader
                handleSearch={this.handleSearchMembers}
                total={paging.total}
              />
              <MembersList
                currentUser={currentUser}
                members={members}
                organization={organization}
                organizationGroups={groups}
                removeMember={this.handleRemoveMember}
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

export default withCurrentUserContext(withOrganizationContext(OrganizationMembers));
