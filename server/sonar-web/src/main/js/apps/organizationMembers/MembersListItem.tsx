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
import ActionsDropdown, {
  ActionsDropdownDivider,
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import Avatar from '../../components/ui/Avatar';
import ManageMemberGroupsForm from './ManageMemberGroupsForm';
import RemoveMemberForm from './RemoveMemberForm';

interface Props {
  member: T.OrganizationMember;
  organization: T.Organization;
  organizationGroups: T.Group[];
  removeMember?: (member: T.OrganizationMember) => void;
  updateMemberGroups: (
    member: T.OrganizationMember,
    add: string[],
    remove: string[]
  ) => Promise<void>;
}

interface State {
  removeMemberForm: boolean;
  manageGroupsForm: boolean;
}

const AVATAR_SIZE = 36;

export default class MembersListItem extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { removeMemberForm: false, manageGroupsForm: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleManageGroupsClick = () => {
    this.setState({ manageGroupsForm: true });
  };

  closeManageGroupsForm = () => {
    if (this.mounted) {
      this.setState({ manageGroupsForm: false });
    }
  };

  handleRemoveMemberClick = () => {
    this.setState({ removeMemberForm: true });
  };

  closeRemoveMemberForm = () => {
    if (this.mounted) {
      this.setState({ removeMemberForm: false });
    }
  };

  render() {
    const { member, organization, removeMember } = this.props;
    const { actions = {} } = organization;
    return (
      <tr>
        <td className="thin nowrap">
          <Avatar hash={member.avatar} name={member.name} size={AVATAR_SIZE} />
        </td>
        <td className="nowrap text-middle">
          <strong>{member.name}</strong>
          <span className="note little-spacer-left">{member.login}</span>
        </td>
        {actions.admin && (
          <td className="text-right text-middle">
            {translateWithParameters(
              'organization.members.x_groups',
              formatMeasure(member.groupCount || 0, 'INT')
            )}
          </td>
        )}
        {actions.admin && (
          <>
            <td className="nowrap text-middle text-right">
              <ActionsDropdown>
                <ActionsDropdownItem onClick={this.handleManageGroupsClick}>
                  {translate('organization.members.manage_groups')}
                </ActionsDropdownItem>
                {removeMember && (
                  <>
                    <ActionsDropdownDivider />
                    <ActionsDropdownItem destructive={true} onClick={this.handleRemoveMemberClick}>
                      {translate('organization.members.remove')}
                    </ActionsDropdownItem>
                  </>
                )}
              </ActionsDropdown>
            </td>

            {this.state.manageGroupsForm && (
              <ManageMemberGroupsForm
                member={this.props.member}
                onClose={this.closeManageGroupsForm}
                organization={this.props.organization}
                organizationGroups={this.props.organizationGroups}
                updateMemberGroups={this.props.updateMemberGroups}
              />
            )}

            {removeMember && this.state.removeMemberForm && (
              <RemoveMemberForm
                member={this.props.member}
                onClose={this.closeRemoveMemberForm}
                organization={this.props.organization}
                removeMember={removeMember}
              />
            )}
          </>
        )}
      </tr>
    );
  }
}
