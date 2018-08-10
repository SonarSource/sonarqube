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
import { connect } from 'react-redux';
import OrganizationMembers from './OrganizationMembers';
import {
  getOrganizationByKey,
  getOrganizationGroupsByKey,
  getOrganizationMembersLogins,
  getUsersByLogins,
  getOrganizationMembersState
} from '../../../store/rootReducer';
import {
  fetchOrganizationMembers,
  fetchMoreOrganizationMembers,
  fetchOrganizationGroups,
  addOrganizationMember,
  removeOrganizationMember,
  updateOrganizationMemberGroups
} from '../actions';
import { Organization, OrganizationMember, Group } from '../../../app/types';

interface OwnProps {
  params: { organizationKey: string };
}

interface StateProps {
  memberLogins: string[];
  members: OrganizationMember[];
  organization: Organization;
  organizationGroups: Group[];
  status: { loading?: boolean; total?: number; pageIndex?: number; query?: string };
}

interface DispatchProps {
  addOrganizationMember: (organizationKey: string, member: OrganizationMember) => void;
  fetchMoreOrganizationMembers: (organizationKey: string, query?: string) => void;
  fetchOrganizationGroups: (organizationKey: string) => void;
  fetchOrganizationMembers: (organizationKey: string, query?: string) => void;
  removeOrganizationMember: (organizationKey: string, member: OrganizationMember) => void;
  updateOrganizationMemberGroups: (
    organization: Organization,
    member: OrganizationMember,
    add: string[],
    remove: string[]
  ) => void;
}

const mapStateToProps = (state: any, ownProps: OwnProps): StateProps => {
  const { organizationKey } = ownProps.params;
  const memberLogins = getOrganizationMembersLogins(state, organizationKey);
  return {
    memberLogins,
    members: getUsersByLogins(state, memberLogins),
    organization: getOrganizationByKey(state, organizationKey)!,
    organizationGroups: getOrganizationGroupsByKey(state, organizationKey),
    status: getOrganizationMembersState(state, organizationKey)
  };
};

const mapDispatchToProps = {
  addOrganizationMember,
  fetchMoreOrganizationMembers,
  fetchOrganizationGroups,
  fetchOrganizationMembers,
  removeOrganizationMember,
  updateOrganizationMemberGroups
};

export default connect<StateProps, DispatchProps, OwnProps>(
  mapStateToProps,
  mapDispatchToProps
)(OrganizationMembers);
