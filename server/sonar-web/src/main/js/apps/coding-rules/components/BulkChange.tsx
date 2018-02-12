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
import * as React from 'react';
import * as classNames from 'classnames';
import BulkChangeModal from './BulkChangeModal';
import { Query } from '../query';
import { Profile } from '../../../api/quality-profiles';
import Dropdown from '../../../components/controls/Dropdown';
import { translate } from '../../../helpers/l10n';

interface Props {
  organization: string | undefined;
  query: Query;
  referencedProfiles: { [profile: string]: Profile };
  total: number;
}

interface State {
  action?: string;
  modal: boolean;
  profile?: Profile;
}

export default class BulkChange extends React.PureComponent<Props, State> {
  closeDropdown?: () => void;
  state: State = { modal: false };

  getSelectedProfile = () => {
    const { profile } = this.props.query;
    return (profile && this.props.referencedProfiles[profile]) || undefined;
  };

  closeModal = () => this.setState({ action: undefined, modal: false, profile: undefined });

  handleActivateClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.closeDropdown) {
      this.closeDropdown();
    }
    this.setState({ action: 'activate', modal: true, profile: undefined });
  };

  handleActivateInProfileClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.closeDropdown) {
      this.closeDropdown();
    }
    this.setState({ action: 'activate', modal: true, profile: this.getSelectedProfile() });
  };

  handleDeactivateClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.closeDropdown) {
      this.closeDropdown();
    }
    this.setState({ action: 'deactivate', modal: true, profile: undefined });
  };

  handleDeactivateInProfileClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    if (this.closeDropdown) {
      this.closeDropdown();
    }
    this.setState({ action: 'deactivate', modal: true, profile: this.getSelectedProfile() });
  };

  render() {
    // show "Bulk Change" button only if user has at least one QP which he administrates
    const canBulkChange = Object.values(this.props.referencedProfiles).some(profile =>
      Boolean(profile.actions && profile.actions.edit)
    );
    if (!canBulkChange) {
      return null;
    }

    const { activation } = this.props.query;
    const profile = this.getSelectedProfile();
    const canChangeProfile = Boolean(
      profile && !profile.isBuiltIn && profile.actions && profile.actions.edit
    );
    const allowActivateOnProfile = canChangeProfile && activation === false;
    const allowDeactivateOnProfile = canChangeProfile && activation === true;

    return (
      <>
        <Dropdown>
          {({ closeDropdown, onToggleClick, open }) => {
            this.closeDropdown = closeDropdown;
            return (
              <div className={classNames('pull-left dropdown', { open })}>
                <button className="js-bulk-change" onClick={onToggleClick}>
                  {translate('bulk_change')}
                </button>
                <ul className="dropdown-menu">
                  <li>
                    <a href="#" onClick={this.handleActivateClick}>
                      {translate('coding_rules.activate_in')}…
                    </a>
                  </li>
                  {allowActivateOnProfile &&
                    profile && (
                      <li>
                        <a href="#" onClick={this.handleActivateInProfileClick}>
                          {translate('coding_rules.activate_in')} <strong>{profile.name}</strong>
                        </a>
                      </li>
                    )}
                  <li>
                    <a href="#" onClick={this.handleDeactivateClick}>
                      {translate('coding_rules.deactivate_in')}…
                    </a>
                  </li>
                  {allowDeactivateOnProfile &&
                    profile && (
                      <li>
                        <a href="#" onClick={this.handleDeactivateInProfileClick}>
                          {translate('coding_rules.deactivate_in')} <strong>{profile.name}</strong>
                        </a>
                      </li>
                    )}
                </ul>
              </div>
            );
          }}
        </Dropdown>
        {this.state.modal &&
          this.state.action && (
            <BulkChangeModal
              action={this.state.action}
              onClose={this.closeModal}
              organization={this.props.organization}
              profile={this.state.profile}
              query={this.props.query}
              referencedProfiles={this.props.referencedProfiles}
              total={this.props.total}
            />
          )}
      </>
    );
  }
}
