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
//@flow
/*::
export type Member = {
  login: string,
  name: string,
  avatar?: string,
  email?: string,
  groupCount?: number
};
*/

/*::
type MembersState = {
  paging?: number,
  total?: number,
  loading?: boolean,
  query?: string | null
};
*/

export const actions = {
  UPDATE_STATE: 'organizations/UPDATE_STATE',
  RECEIVE_MEMBERS: 'organizations/RECEIVE_MEMBERS',
  RECEIVE_MORE_MEMBERS: 'organizations/RECEIVE_MORE_MEMBERS',
  ADD_MEMBER: 'organizations/ADD_MEMBER',
  REMOVE_MEMBER: 'organizations/REMOVE_MEMBER'
};

export const receiveMembers = (
  organizationKey /*: string */,
  members /*: Array<Member> */,
  stateChanges /*: MembersState */
) => ({
  type: actions.RECEIVE_MEMBERS,
  organization: organizationKey,
  members,
  stateChanges
});

export const receiveMoreMembers = (
  organizationKey /*: string */,
  members /*: Array<Member> */,
  stateChanges /*: MembersState */
) => ({
  type: actions.RECEIVE_MORE_MEMBERS,
  organization: organizationKey,
  members,
  stateChanges
});

export const addMember = (organizationKey /*: string */, member /*: Member */) => ({
  type: actions.ADD_MEMBER,
  organization: organizationKey,
  member
});

export const removeMember = (organizationKey /*: string */, member /*: Member */) => ({
  type: actions.REMOVE_MEMBER,
  organization: organizationKey,
  member
});

export const updateState = (organizationKey /*: string */, stateChanges /*: MembersState */) => ({
  type: actions.UPDATE_STATE,
  organization: organizationKey,
  stateChanges
});
