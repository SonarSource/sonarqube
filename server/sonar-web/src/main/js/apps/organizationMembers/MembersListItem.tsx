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
import {
  ButtonIcon,
  ButtonVariety,
  DropdownMenu,
  IconMoreVertical,
} from '@sonarsource/echoes-react';
import * as React from 'react';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import Avatar from '../../components/ui/Avatar';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Group, Organization, OrganizationActions, OrganizationMember } from '../../types/types';
import ManageMemberGroupsForm from './ManageMemberGroupsForm';
import RemoveMemberForm from './RemoveMemberForm';
import {UserGroup} from "../../api/users";
import { setMemberType } from '../../api/organizations';
import { addGlobalErrorMessage } from '~design-system';

const USER_TYPES = [
  { value: "STANDARD", label: "Standard User" },
  { value: "PLATFORM", label: "Platform Integration User" }
];

interface Props {
  member: OrganizationMember;
  organization: Organization;
  organizationGroups: Group[];
  removeMember?: (member: OrganizationMember) => void;
  updateMemberGroups: (
    member: OrganizationMember,
    add: UserGroup[],
    remove: UserGroup[],
  ) => Promise<void>;
}

interface State {
  removeMemberForm: boolean;
  manageGroupsForm: boolean;
  type: string;
}

const AVATAR_SIZE = 36;

export default class MembersListItem extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { removeMemberForm: false, manageGroupsForm: false, type: this.props.member.type };

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

  handleRadioChange = async (type: string, member: OrganizationMember) => {
    await setMemberType(this.props.organization.kee, member.login, type); // API Call
    this.setState({ type });
  };

  isPermissibleToChange = (actions: OrganizationActions, member: OrganizationMember, value: string) => {
    if (!actions.admin) {
      return false;
    }
    if (member.isAdmin && value === 'PLATFORM') {
      addGlobalErrorMessage('You are a System Admin. You are required to have a Standard User License.');
      return false;
    }
    return true;
  }


  render() {
    const { member, organization, removeMember } = this.props;
    const { actions = {} } = organization;
    const { type } = this.state;
    return (
      <tr>
        <td className="thin nowrap">
          <Avatar className="sw-p-2" hash={member.avatar} name={member.name} size={AVATAR_SIZE} />
        </td>
        <td className="nowrap text-middle">
          <strong>{member.name}</strong>
          <span className="note sw-ml-2">{member.login}</span>
        </td>
        {USER_TYPES.map(({ value, label }) => (
          <td key={value} className="nowrap text-middle">
            <input
              type="radio"
              name={member.login}
              value={value}
              className={`member-type-${member.type}`}
              checked={type === value}
              onChange={() => {
                if (this.isPermissibleToChange(actions, member, value)) {
                  this.handleRadioChange(value, member);
                }
              }}            />
            <span className='note sw-ml-2'>{label}</span>
          </td>
        ))}
        {actions.admin && (
          <td className="text-right text-middle">
            {translateWithParameters(
              'organization.members.x_groups',
              formatMeasure(member.groupCount || 0, 'INT'),
            )}
          </td>
        )}
        {actions.admin && (
          <>
            <td className="nowrap text-middle text-right">
              <DropdownMenu.Root
                items={
                  <>
                    <DropdownMenu.ItemButton onClick={this.handleManageGroupsClick}>
                      {translate('organization.members.manage_groups')}
                    </DropdownMenu.ItemButton>
                    {removeMember && (
                      <DropdownMenu.ItemButton onClick={this.handleRemoveMemberClick}>
                        {translate('organization.members.remove')}
                      </DropdownMenu.ItemButton>
                    )}
                  </>
                }
              >
                <ButtonIcon
                  className="it__user-actions-toggle"
                  Icon={IconMoreVertical}
                  variety={ButtonVariety.DefaultGhost}
                />
              </DropdownMenu.Root>
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
