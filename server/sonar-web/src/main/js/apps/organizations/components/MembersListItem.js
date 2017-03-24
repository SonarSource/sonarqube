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
//@flow
import React from 'react';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import type { Member } from '../../../store/organizationsMembers/actions';
import type { Organization } from '../../../store/organizations/duck';

type Props = {
  member: Member,
  organization: Organization
};

const AVATAR_SIZE: number = 36;

export default class MembersListItem extends React.PureComponent {
  props: Props;

  render() {
    const { member, organization } = this.props;
    return (
      <tr>
        <td className="thin nowrap">
          <Avatar hash={member.avatar} size={AVATAR_SIZE} />
        </td>
        <td className="nowrap text-middle"><strong>{member.name}</strong></td>
        {organization.canAdmin &&
          <td className="text-right text-middle">
            {translate('organization.members.x_group(s)', formatMeasure(member.groupCount, 'INT'))}
          </td>
        }
      </tr>
    );
  }
}
