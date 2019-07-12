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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Profile } from '../../../api/quality-profiles';
import { Query } from '../query';
import BulkChangeModal from './BulkChangeModal';

interface Props {
  languages: T.Languages;
  organization: string | undefined;
  query: Query;
  referencedProfiles: T.Dict<Profile>;
  total: number;
}

interface State {
  action?: string;
  modal: boolean;
  profile?: Profile;
}

export default class BulkChange extends React.PureComponent<Props, State> {
  state: State = { modal: false };

  getSelectedProfile = () => {
    const { profile } = this.props.query;
    return (profile && this.props.referencedProfiles[profile]) || undefined;
  };

  closeModal = () => this.setState({ action: undefined, modal: false, profile: undefined });

  handleActivateClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ action: 'activate', modal: true, profile: undefined });
  };

  handleActivateInProfileClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ action: 'activate', modal: true, profile: this.getSelectedProfile() });
  };

  handleDeactivateClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ action: 'deactivate', modal: true, profile: undefined });
  };

  handleDeactivateInProfileClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ action: 'deactivate', modal: true, profile: this.getSelectedProfile() });
  };

  render() {
    // show "Bulk Change" button only if user has at least one QP which he administrates
    const canBulkChange = Object.values(this.props.referencedProfiles).some(profile =>
      Boolean(profile.actions && profile.actions.edit)
    );
    if (!canBulkChange) {
      return (
        <Tooltip overlay={translate('coding_rules.can_not_bulk_change')}>
          <Button className="js-bulk-change" disabled={true}>
            {translate('bulk_change')}
          </Button>
        </Tooltip>
      );
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
        <Dropdown
          className="pull-left"
          overlay={
            <ul className="menu">
              <li>
                <a href="#" onClick={this.handleActivateClick}>
                  {translate('coding_rules.activate_in')}…
                </a>
              </li>
              {allowActivateOnProfile && profile && (
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
              {allowDeactivateOnProfile && profile && (
                <li>
                  <a href="#" onClick={this.handleDeactivateInProfileClick}>
                    {translate('coding_rules.deactivate_in')} <strong>{profile.name}</strong>
                  </a>
                </li>
              )}
            </ul>
          }>
          <Button className="js-bulk-change">{translate('bulk_change')}</Button>
        </Dropdown>
        {this.state.modal && this.state.action && (
          <BulkChangeModal
            action={this.state.action}
            languages={this.props.languages}
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
