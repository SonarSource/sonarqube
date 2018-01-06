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
import React from 'react';
import MembersListItem from './MembersListItem';
/*:: import type { Member } from '../../../store/organizationsMembers/actions'; */
/*:: import type { Organization, OrgGroup } from '../../../store/organizations/duck'; */

/*::
type Props = {
  members: Array<Member>,
  organizationGroups: Array<OrgGroup>,
  organization: Organization,
  removeMember: Member => void,
  updateMemberGroups: (member: Member, add: Array<string>, remove: Array<string>) => void
};
*/

export default class MembersList extends React.PureComponent {
  /*:: props: Props; */

  render() {
    return (
      <div className="boxed-group boxed-group-inner">
        <table className="data zebra">
          <tbody>
            {this.props.members.map(member => (
              <MembersListItem
                key={member.login}
                member={member}
                organizationGroups={this.props.organizationGroups}
                organization={this.props.organization}
                removeMember={this.props.removeMember}
                updateMemberGroups={this.props.updateMemberGroups}
              />
            ))}
          </tbody>
        </table>
      </div>
    );
  }
}
