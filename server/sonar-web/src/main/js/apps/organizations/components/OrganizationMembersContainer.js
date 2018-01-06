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

const mapStateToProps = (state, ownProps) => {
  const { organizationKey } = ownProps.params;
  const memberLogins = getOrganizationMembersLogins(state, organizationKey);
  return {
    memberLogins,
    members: getUsersByLogins(state, memberLogins),
    organization: getOrganizationByKey(state, organizationKey),
    organizationGroups: getOrganizationGroupsByKey(state, organizationKey),
    status: getOrganizationMembersState(state, organizationKey)
  };
};

export default connect(mapStateToProps, {
  fetchOrganizationMembers,
  fetchMoreOrganizationMembers,
  fetchOrganizationGroups,
  addOrganizationMember,
  removeOrganizationMember,
  updateOrganizationMemberGroups
})(OrganizationMembers);
