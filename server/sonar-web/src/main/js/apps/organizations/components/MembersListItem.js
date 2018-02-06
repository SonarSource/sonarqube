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
import RemoveMemberForm from './forms/RemoveMemberForm';
import ManageMemberGroupsForm from './forms/ManageMemberGroupsForm';
import Avatar from '../../../components/ui/Avatar';
import { translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import ActionsDropdown, {
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
/*:: import type { Member } from '../../../store/organizationsMembers/actions'; */
/*:: import type { Organization, OrgGroup } from '../../../store/organizations/duck'; */

/*::
type Props = {
  member: Member,
  organization: Organization,
  organizationGroups: Array<OrgGroup>,
  removeMember: Member => void,
  updateMemberGroups: (member: Member, add: Array<string>, remove: Array<string>) => void
};
*/

const AVATAR_SIZE /*: number */ = 36;

export default class MembersListItem extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { member, organization } = this.props;
    return (
      <tr>
        <td className="thin nowrap">
          <Avatar hash={member.avatar} name={member.name} size={AVATAR_SIZE} />
        </td>
        <td className="nowrap text-middle">
          <strong>{member.name}</strong>
          <span className="note little-spacer-left">{member.login}</span>
        </td>
        {organization.canAdmin && (
          <td className="text-right text-middle">
            {translateWithParameters(
              'organization.members.x_groups',
              formatMeasure(member.groupCount || 0, 'INT')
            )}
          </td>
        )}
        {organization.canAdmin && (
          <td className="nowrap text-middle text-right">
            <ActionsDropdown>
              <ManageMemberGroupsForm
                organizationGroups={this.props.organizationGroups}
                organization={this.props.organization}
                updateMemberGroups={this.props.updateMemberGroups}
                member={this.props.member}
              />
              <ActionsDropdownDivider />
              <RemoveMemberForm
                organization={this.props.organization}
                removeMember={this.props.removeMember}
                member={this.props.member}
              />
            </ActionsDropdown>
          </td>
        )}
      </tr>
    );
  }
}
