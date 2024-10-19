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
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import MembersPageHeader from './MembersPageHeader';
import MembersListHeader from './MembersListHeader';
import MembersList from './MembersList';
import { getUsersGroups } from '../../api/user_groups';
import Suggestions from "../../components/embed-docs-modal/Suggestions";
import { translate } from "../../helpers/l10n";
import ListFooter from "../../components/controls/ListFooter";
import { addMember, removeMember, searchMembers } from "../../api/organizations";
import { Group, Organization, OrganizationMember, Paging } from "../../types/types";
import { LoggedInUser } from "../../types/users";
import { withOrganizationContext } from "../organizations/OrganizationContext";
import withCurrentUserContext from "../../app/components/current-user/withCurrentUserContext";
import {
  useAddGroupMembershipMutation,
  useGroupMembersQuery,
  useRemoveGroupMembershipMutation
} from "../../queries/group-memberships";

interface Props {
  currentUser: LoggedInUser;
  organization: Organization;
}

const PAGE_SIZE = 50;

function OrganizationMembers({ currentUser, organization }: Props) {

  const [members, setMembers] = React.useState<OrganizationMember[]>();
  const [groups, setGroups] = React.useState<Group[]>([]);
  const [loading, setLoading] = React.useState<boolean>(false);
  const [query, setQuery] = React.useState<string>();
  const [paging, setPaging] = React.useState<Paging>();

  const { mutateAsync: addUserToGroup } = useAddGroupMembershipMutation();
  const { mutateAsync: removeUserFromGroup } = useRemoveGroupMembershipMutation();
  const { data, isLoading, fetchNextPage } = useGroupMembersQuery({
    q: query,
    groupId: group.id,
    filter,
  });

  React.useEffect(() => {
    fetchMembers();

    if (this.props.organization.actions && this.props.organization.actions.admin) {
      fetchGroups();
    }
  }, []);

  const stopLoading = () => {
    setLoading(false);
  };

  const fetchMembers = (query?: string) => {
    setLoading(true);
    searchMembers({
      organization: this.props.organization.kee,
      ps: PAGE_SIZE,
      q: query
    }).then(({ paging, users }) => {
      setLoading(false);
      setMembers(members);
      setPaging(paging);
    }, stopLoading);
  };

  const fetchGroups = () => {
    getUsersGroups({ organization: this.props.organization.kee }).then(
      ({ groups }) => {
        setGroups(groups);
      },
      () => {
      }
    );
  };

  const handleSearchMembers = (query: string) => {
    setQuery(query);
    fetchMembers(query || undefined); // empty string -> undefined
  };

  const handleLoadMoreMembers = () => {
    if (!paging) {
      return;
    }

    setLoading(true);
    searchMembers({
      organization: this.props.organization.kee,
      p: paging.pageIndex + 1,
      ps: PAGE_SIZE,
      q: query || undefined // empty string -> undefined
    }).then(({ paging, users }) => {
      setLoading(false);
      setMembers([...members, ...users]);
      setPaging(paging);
    }, stopLoading);
  };

  const handleAddMember = ({ login }: OrganizationMember) => {
    // TODO optimistic update
    addMember({ login, organization: organization.kee }).then(
      member => {
        setMembers(members && [...members, member]);
        setPaging(paging && { ...paging, total: paging.total + 1 });
      },
      () => {
      }
    );
  };

  const handleRemoveMember = ({ login }: OrganizationMember) => {
    // TODO optimistic update
    removeMember({ login, organization: organization.kee }).then(
      () => {
        setMembers(members && members.filter(member => member.login !== login));
        setPaging(paging && { ...paging, total: paging.total - 1 });
      },
      () => {
      }
    );
  };

  const updateGroup = (
    login: string,
    updater: (member: OrganizationMember) => OrganizationMember
  ) => {
    setMembers(members && members.map(member => (member.login === login ? updater(member) : member)));
  };

  const updateMemberGroups = ({ login }: OrganizationMember, add: string[], remove: string[]) => {
    // TODO optimistic update
    const promises = [
      ...add.map(name =>
        addUserToGroup({ name, login, organization: organization.kee })
      ),
      ...remove.map(name =>
        removeUserFromGroup({
          organization: organization.kee,
          name,
          login,
        })
      )
    ]
    return Promise.all(promises).then(() => {
      updateGroup(login, member => ({
        ...member,
        groupCount: (member.groupCount || 0) + add.length - remove.length
      }));
    });
  };

  return (
    <div className="page page-limited">
      <Helmet title={translate('organization.members.page')}/>
      <Suggestions suggestions="organization_members"/>
      <MembersPageHeader
        handleAddMember={handleAddMember}
        loading={loading}
        members={members}
        organization={organization}
      />
      {members !== undefined &&
        paging !== undefined && (
          <>
            <MembersListHeader
              handleSearch={handleSearchMembers}
              total={paging.total}
            />
            <MembersList
              currentUser={currentUser}
              members={members}
              organization={organization}
              organizationGroups={groups}
              removeMember={handleRemoveMember}
              updateMemberGroups={updateMemberGroups}
            />
            {paging.total !== 0 && (
              <ListFooter
                count={members.length}
                loadMore={handleLoadMoreMembers}
                ready={!loading}
                total={paging.total}
              />
            )}
          </>
        )}
    </div>
  )
}

export default withCurrentUserContext(withOrganizationContext(OrganizationMembers));
