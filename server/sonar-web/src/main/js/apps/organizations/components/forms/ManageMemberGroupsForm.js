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
// @flow
import React from 'react';
import { keyBy, pickBy } from 'lodash';
import { getUserGroups } from '../../../../api/users';
import Modal from '../../../../components/controls/Modal';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import OrganizationGroupCheckbox from '../OrganizationGroupCheckbox';
import { ActionsDropdownItem } from '../../../../components/controls/ActionsDropdown';
/*:: import type { Member } from '../../../../store/organizationsMembers/actions'; */
/*:: import type { Organization, OrgGroup } from '../../../../store/organizations/duck'; */

/*::
type Props = {
  member: Member,
  organization: Organization,
  organizationGroups: Array<OrgGroup>,
  updateMemberGroups: (member: Member, add: Array<string>, remove: Array<string>) => void
};
*/

/*::
type State = {
  open: boolean,
  userGroups?: {},
  loading?: boolean
};
*/

export default class ManageMemberGroupsForm extends React.PureComponent {
  /*:: mounted: boolean */
  /*:: props: Props; */

  state /*: State */ = {
    open: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  openForm = () => {
    this.loadUserGroups();
    this.setState({ open: true });
  };

  closeForm = () => {
    this.setState({ open: false });
  };

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

  isGroupSelected = (groupName /*: string */) => {
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

  onCheck = (groupName /*: string */, checked /*: boolean */) => {
    this.setState((prevState /*: State */) => {
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

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    this.props.updateMemberGroups(
      this.props.member,
      Object.keys(pickBy(this.state.userGroups, group => group.status === 'add')),
      Object.keys(pickBy(this.state.userGroups, group => group.status === 'remove'))
    );
    this.closeForm();
  };

  renderModal() {
    const header = translate('organization.members.manage_groups');
    return (
      <Modal key="manage-member-modal" contentLabel={header} onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
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
                    key={group.id}
                    group={group}
                    checked={this.isGroupSelected(group.name)}
                    onCheck={this.onCheck}
                  />
                ))}
              </ul>
            )}
          </div>
          <footer className="modal-foot">
            <div>
              <button type="submit">{translate('save')}</button>
              <button type="reset" className="button-link" onClick={this.closeForm}>
                {translate('cancel')}
              </button>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    const buttonComponent = (
      <ActionsDropdownItem onClick={this.openForm}>
        {translate('organization.members.manage_groups')}
      </ActionsDropdownItem>
    );
    if (this.state.open) {
      return [buttonComponent, this.renderModal()];
    }
    return buttonComponent;
  }
}
