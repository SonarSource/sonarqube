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

import * as React from 'react';
import {
  ButtonPrimary,
  ButtonSecondary,
  ChevronDownIcon,
  Dropdown,
  ItemButton,
  PopupPlacement,
  PopupZLevel,
} from '~design-system';
import { Profile } from '../../../api/quality-profiles';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { Dict } from '../../../types/types';
import { Query } from '../query';
import BulkChangeModal from './BulkChangeModal';

interface Props {
  onSubmit?: () => void;
  query: Query;
  referencedProfiles: Dict<Profile>;
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

  handleActivateClick = () => {
    this.setState({ action: 'activate', modal: true, profile: undefined });
  };

  handleActivateInProfileClick = () => {
    this.setState({ action: 'activate', modal: true, profile: this.getSelectedProfile() });
  };

  handleDeactivateClick = () => {
    this.setState({ action: 'deactivate', modal: true, profile: undefined });
  };

  handleDeactivateInProfileClick = () => {
    this.setState({ action: 'deactivate', modal: true, profile: this.getSelectedProfile() });
  };

  render() {
    // show "Bulk Change" button only if user is admin of at least one QP
    const canBulkChange = Object.values(this.props.referencedProfiles).some((profile) =>
      Boolean(profile.actions?.edit),
    );
    if (!canBulkChange) {
      return (
        <Tooltip content={translate('coding_rules.can_not_bulk_change')}>
          <ButtonPrimary disabled>{translate('bulk_change')}</ButtonPrimary>
        </Tooltip>
      );
    }

    const { activation } = this.props.query;
    const profile = this.getSelectedProfile();
    const canChangeProfile = Boolean(
      profile && !profile.isBuiltIn && profile.actions && profile.actions.edit,
    );
    const allowActivateOnProfile = canChangeProfile && activation === false;
    const allowDeactivateOnProfile = canChangeProfile && activation === true;

    return (
      <>
        <Dropdown
          id="issue-bulkaction-menu"
          size="auto"
          placement={PopupPlacement.BottomRight}
          zLevel={PopupZLevel.Global}
          allowResizing
          overlay={
            <>
              <ItemButton onClick={this.handleActivateClick}>
                {translate('coding_rules.activate_in')}
              </ItemButton>

              {allowActivateOnProfile && profile && (
                <ItemButton onClick={this.handleActivateInProfileClick}>
                  {translate('coding_rules.activate_in')}{' '}
                  <strong className="sw-ml-1">{profile.name}</strong>
                </ItemButton>
              )}

              <ItemButton onClick={this.handleDeactivateClick}>
                {translate('coding_rules.deactivate_in')}
              </ItemButton>

              {allowDeactivateOnProfile && profile && (
                <ItemButton onClick={this.handleDeactivateInProfileClick}>
                  {translate('coding_rules.deactivate_in')}{' '}
                  <strong className="sw-ml-1">{profile.name}</strong>
                </ItemButton>
              )}
            </>
          }
        >
          <ButtonSecondary>
            {translate('bulk_change')}
            <ChevronDownIcon className="sw-ml-1" />
          </ButtonSecondary>
        </Dropdown>
        {this.state.modal && this.state.action && (
          <BulkChangeModal
            action={this.state.action}
            onClose={this.closeModal}
            onSubmit={this.props.onSubmit}
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
