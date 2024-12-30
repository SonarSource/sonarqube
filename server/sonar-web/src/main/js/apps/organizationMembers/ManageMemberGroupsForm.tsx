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
import { Button, ButtonVariety, Spinner } from '@sonarsource/echoes-react';
import { Modal } from '~design-system';
import { keyBy, pickBy, some } from 'lodash';
import * as React from 'react';
import { getUserGroups, UserGroup } from '../../api/users';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Group, Organization, OrganizationMember } from '../../types/types';
import OrganizationGroupCheckbox from './OrganizationGroupCheckbox';

interface Props {
  onClose: () => void;
  member: OrganizationMember;
  organization: Organization;
  organizationGroups: Group[];
  updateMemberGroups: (
    member: OrganizationMember,
    add: UserGroup[],
    remove: UserGroup[],
  ) => Promise<void>;
}

interface State {
  userGroups?: { [k: string]: UserGroup & { status?: string } };
  loading?: boolean;
}

export default class ManageMemberGroupsForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {};

  componentDidMount() {
    this.mounted = true;
    this.loadUserGroups();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadUserGroups = () => {
    this.setState({ loading: true });
    getUserGroups({
      login: this.props.member.login,
      organization: this.props.organization.kee,
    }).then(
      (response) => {
        if (this.mounted) {
          this.setState({ loading: false, userGroups: keyBy(response.groups, 'name') });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  isGroupSelected = (groupName: string) => {
    if (this.state.userGroups) {
      const group = this.state.userGroups[groupName] || {};
      if (group.status) {
        return group.status === 'add';
      }
      return group.selected === true;
    }
    return false;
  };

  onCheck = (groupName: string, checked: boolean) => {
    this.setState((prevState: State) => {
      const { userGroups = {} } = prevState;
      const group = userGroups[groupName] || this.props.organizationGroups.find(g => g.name === groupName)
      let status = '';
      if (group.selected && !checked) {
        status = 'remove';
      } else if (!group.selected && checked) {
        status = 'add';
      }
      return { userGroups: { ...userGroups, [groupName]: { ...group, status } } };
    });
  };

  handleFormSubmit = (event) => {
    event.preventDefault();
    const addGroups = Object.values(pickBy(this.state.userGroups, (group) => group.status === 'add')).map(({ status, ...group }) => group);
    const removeGroups = Object.values(pickBy(this.state.userGroups, (group) => group.status === 'remove')).map(({ status, ...group }) => group);

    return this.props
      .updateMemberGroups(
        this.props.member,
        addGroups,
        removeGroups,
      )
      .then(this.props.onClose);
  };

  renderForm = () => {
    const { loading, userGroups = {} } = this.state;
    const hasChanges = some(userGroups, (group) => group.status !== undefined);
    return (
      <form onSubmit={this.handleFormSubmit}>
        <div className="modal-body modal-container">
          <p>
            <strong>
              {translateWithParameters(
                'organization.members.members_groups',
                this.props.member.name,
              )}
            </strong>
          </p>
          <Spinner isLoading={loading}>
            <ul className="list-spaced">
              {this.props.organizationGroups.map((group) => (
                <OrganizationGroupCheckbox
                  checked={this.isGroupSelected(group.name)}
                  group={group}
                  key={group.id}
                  onCheck={this.onCheck}
                />
              ))}
            </ul>
          </Spinner>
        </div>

        <footer className="modal-foot sw-mt-4 sw-flex sw-justify-end">
          {loading}
          <Spinner className="sw-ml-2" isLoading={loading} />
          <Button className="sw-mr-2" variety={ButtonVariety.Primary} type="submit">
            {translate('save')}
          </Button>
          <Button onClick={this.props.onClose}>{translate('cancel')}</Button>
        </footer>
      </form>
    );
  };

  render() {
    return (
      <Modal
        headerTitle={translate('organization.members.manage_groups')}
        onClose={this.props.onClose}
        body={this.renderForm()}
        loading={this.state.loading}
        showClosebtn={false}
      />
    );
  }
}
