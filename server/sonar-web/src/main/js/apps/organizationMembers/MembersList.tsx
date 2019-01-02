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
import { sortBy } from 'lodash';
import MembersListItem from './MembersListItem';
import { translate } from '../../helpers/l10n';

interface Props {
  members: T.OrganizationMember[];
  organizationGroups: T.Group[];
  organization: T.Organization;
  removeMember: (member: T.OrganizationMember) => void;
  updateMemberGroups: (
    member: T.OrganizationMember,
    add: Array<string>,
    remove: Array<string>
  ) => void;
}

export default class MembersList extends React.PureComponent<Props> {
  render() {
    const { members } = this.props;

    if (!members.length) {
      return <div className="note">{translate('no_results')}</div>;
    }

    const sortedMembers = sortBy(members, member => member.name, member => member.login);
    return (
      <div className="boxed-group boxed-group-inner">
        <table className="data zebra">
          <tbody>
            {sortedMembers.map(member => (
              <MembersListItem
                key={member.login}
                member={member}
                organization={this.props.organization}
                organizationGroups={this.props.organizationGroups}
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
