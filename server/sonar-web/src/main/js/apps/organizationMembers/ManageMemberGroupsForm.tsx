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
import { keyBy, pickBy, some } from 'lodash';
import OrganizationGroupCheckbox from '../organizations/components/OrganizationGroupCheckbox';
import SimpleModal from '../../components/controls/SimpleModal';
import { SubmitButton, ResetButtonLink } from '../../components/ui/buttons';
import { getUserGroups, UserGroup } from '../../api/users';
import { translate, translateWithParameters } from '../../helpers/l10n';
import DeferredSpinner from '../../components/common/DeferredSpinner';

interface Props {
  onClose: () => void;
  member: T.OrganizationMember;
  organization: T.Organization;
  organizationGroups: T.Group[];
  updateMemberGroups: (
    member: T.OrganizationMember,
    add: string[],
    remove: string[]
  ) => Promise<void>;
}

interface State {
  userGroups?: T.Dict<UserGroup & { status?: string }>;
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
      const { userGroups = {} } = prevState;
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

  handleSubmit = () => {
    return this.props
      .updateMemberGroups(
        this.props.member,
        Object.keys(pickBy(this.state.userGroups, group => group.status === 'add')),
        Object.keys(pickBy(this.state.userGroups, group => group.status === 'remove'))
      )
      .then(this.props.onClose);
  };

  render() {
    const { loading, userGroups = {} } = this.state;
    const header = translate('organization.members.manage_groups');
    const hasChanges = some(userGroups, group => group.status !== undefined);
    return (
      <SimpleModal header={header} onClose={this.props.onClose} onSubmit={this.handleSubmit}>
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form onSubmit={onFormSubmit}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>
            <div className="modal-body modal-container">
              <p>
                <strong>
                  {translateWithParameters(
                    'organization.members.members_groups',
                    this.props.member.name
                  )}
                </strong>
              </p>
              {loading ? (
                <DeferredSpinner className="spacer-top" />
              ) : (
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
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting || !hasChanges}>{translate('save')}</SubmitButton>
              <ResetButtonLink disabled={submitting} onClick={onCloseClick}>
                {translate('cancel')}
              </ResetButtonLink>
            </footer>
          </form>
        )}
      </SimpleModal>
    );
  }
}
