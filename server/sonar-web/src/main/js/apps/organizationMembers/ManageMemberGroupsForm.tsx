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
import { keyBy, pickBy } from 'lodash';
import { getUserGroups, UserGroup } from '../../api/users';
import Modal from '../../components/controls/Modal';
import { translate, translateWithParameters } from '../../helpers/l10n';
import OrganizationGroupCheckbox from '../organizations/components/OrganizationGroupCheckbox';
import { SubmitButton, ResetButtonLink } from '../../components/ui/buttons';

interface Props {
  onClose: () => void;
  member: T.OrganizationMember;
  organization: T.Organization;
  organizationGroups: T.Group[];
  updateMemberGroups: (member: T.OrganizationMember, add: string[], remove: string[]) => void;
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
    getUserGroups(this.props.member.login, this.props.organization.key).then(
      response => {
        if (this.mounted) {
          this.setState({ loading: false, userGroups: keyBy(response.groups, 'name') });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  isGroupSelected = (groupName: string) => {
    if (this.state.userGroups) {
      const group = this.state.userGroups[groupName] || {};
      if (group.status) {
        return group.status === 'add';
      } else {
        return group.selected === true;
      }
    }
    return false;
  };

  onCheck = (groupName: string, checked: boolean) => {
    this.setState((prevState: State) => {
      const userGroups = prevState.userGroups || {};
      const group = userGroups[groupName] || {};
      let status = '';
      if (group.selected && !checked) {
        status = 'remove';
      } else if (!group.selected && checked) {
        status = 'add';
      }
      return { userGroups: { ...userGroups, [groupName]: { ...group, status } } };
    });
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.props.updateMemberGroups(
      this.props.member,
      Object.keys(pickBy(this.state.userGroups, group => group.status === 'add')),
      Object.keys(pickBy(this.state.userGroups, group => group.status === 'remove'))
    );
    this.props.onClose();
  };

  render() {
    const header = translate('organization.members.manage_groups');
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body modal-container">
            <strong>
              {translateWithParameters(
                'organization.members.members_groups',
                this.props.member.name
              )}
            </strong>{' '}
            {this.state.loading && <i className="spinner" />}
            {!this.state.loading && (
              <ul className="list-spaced">
                {this.props.organizationGroups.map(group => (
                  <OrganizationGroupCheckbox
                    checked={this.isGroupSelected(group.name)}
                    group={group}
                    key={group.id}
                    onCheck={this.onCheck}
                  />
                ))}
              </ul>
            )}
          </div>
          <footer className="modal-foot">
            <div>
              <SubmitButton>{translate('save')}</SubmitButton>
              <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }
}
