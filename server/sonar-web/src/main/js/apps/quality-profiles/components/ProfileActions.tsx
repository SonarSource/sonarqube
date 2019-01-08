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
import RenameProfileForm from './RenameProfileForm';
import CopyProfileForm from './CopyProfileForm';
import DeleteProfileForm from './DeleteProfileForm';
import ExtendProfileForm from './ExtendProfileForm';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { setDefaultProfile } from '../../../api/quality-profiles';
import { getProfilePath, getProfileComparePath, getProfilesPath } from '../utils';
import { Profile } from '../types';
import ActionsDropdown, {
  ActionsDropdownItem,
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';
import { withRouter, Router } from '../../../components/hoc/withRouter';

interface Props {
  className?: string;
  fromList?: boolean;
  organization: string | null;
  profile: Profile;
  router: Pick<Router, 'push' | 'replace'>;
  updateProfiles: () => Promise<void>;
}

interface State {
  copyFormOpen: boolean;
  extendFormOpen: boolean;
  deleteFormOpen: boolean;
  renameFormOpen: boolean;
}

export class ProfileActions extends React.PureComponent<Props, State> {
  state: State = {
    copyFormOpen: false,
    extendFormOpen: false,
    deleteFormOpen: false,
    renameFormOpen: false
  };

  closeCopyForm = () => {
    this.setState({ copyFormOpen: false });
  };

  closeDeleteForm = () => {
    this.setState({ deleteFormOpen: false });
  };

  closeExtendForm = () => {
    this.setState({ extendFormOpen: false });
  };

  closeRenameForm = () => {
    this.setState({ renameFormOpen: false });
  };

  handleCopyClick = () => {
    this.setState({ copyFormOpen: true });
  };

  handleDeleteClick = () => {
    this.setState({ deleteFormOpen: true });
  };

  handleExtendClick = () => {
    this.setState({ extendFormOpen: true });
  };

  handleRenameClick = () => {
    this.setState({ renameFormOpen: true });
  };

  handleProfileCopy = (name: string) => {
    this.closeCopyForm();
    this.navigateToNewProfile(name);
  };

  handleProfileDelete = () => {
    this.props.router.replace(getProfilesPath(this.props.organization));
    this.props.updateProfiles();
  };

  handleProfileExtend = (name: string) => {
    this.closeExtendForm();
    this.navigateToNewProfile(name);
  };

  handleProfileRename = (name: string) => {
    this.closeRenameForm();
    this.props.updateProfiles().then(
      () => {
        if (!this.props.fromList) {
          this.props.router.replace(
            getProfilePath(name, this.props.profile.language, this.props.organization)
          );
        }
      },
      () => {}
    );
  };

  handleSetDefaultClick = () => {
    setDefaultProfile(this.props.profile.key).then(this.props.updateProfiles, () => {});
  };

  navigateToNewProfile = (name: string) => {
    this.props.updateProfiles().then(
      () => {
        this.props.router.push(
          getProfilePath(name, this.props.profile.language, this.props.organization)
        );
      },
      () => {}
    );
  };

  render() {
    const { profile } = this.props;
    const { actions = {} } = profile;

    // FIXME use org, name and lang
    const backupUrl =
      (window as any).baseUrl +
      '/api/qualityprofiles/backup?profileKey=' +
      encodeURIComponent(profile.key);

    const activateMoreUrl = getRulesUrl(
      {
        qprofile: profile.key,
        activation: 'false'
      },
      this.props.organization
    );

    return (
      <>
        <ActionsDropdown className={this.props.className}>
          {actions.edit && (
            <ActionsDropdownItem id="quality-profile-activate-more-rules" to={activateMoreUrl}>
              {translate('quality_profiles.activate_more_rules')}
            </ActionsDropdownItem>
          )}

          {!profile.isBuiltIn && (
            <ActionsDropdownItem
              download={`${profile.key}.xml`}
              id="quality-profile-backup"
              to={backupUrl}>
              {translate('backup_verb')}
            </ActionsDropdownItem>
          )}

          <ActionsDropdownItem
            id="quality-profile-compare"
            to={getProfileComparePath(profile.name, profile.language, this.props.organization)}>
            {translate('compare')}
          </ActionsDropdownItem>

          {actions.copy && (
            <>
              <ActionsDropdownItem id="quality-profile-copy" onClick={this.handleCopyClick}>
                {translate('copy')}
              </ActionsDropdownItem>

              <ActionsDropdownItem id="quality-profile-extend" onClick={this.handleExtendClick}>
                {translate('extend')}
              </ActionsDropdownItem>
            </>
          )}

          {actions.edit && (
            <ActionsDropdownItem id="quality-profile-rename" onClick={this.handleRenameClick}>
              {translate('rename')}
            </ActionsDropdownItem>
          )}

          {actions.setAsDefault && (
            <ActionsDropdownItem
              id="quality-profile-set-as-default"
              onClick={this.handleSetDefaultClick}>
              {translate('set_as_default')}
            </ActionsDropdownItem>
          )}

          {actions.delete && <ActionsDropdownDivider />}

          {actions.delete && (
            <ActionsDropdownItem
              destructive={true}
              id="quality-profile-delete"
              onClick={this.handleDeleteClick}>
              {translate('delete')}
            </ActionsDropdownItem>
          )}
        </ActionsDropdown>

        {this.state.copyFormOpen && (
          <CopyProfileForm
            onClose={this.closeCopyForm}
            onCopy={this.handleProfileCopy}
            profile={profile}
          />
        )}

        {this.state.extendFormOpen && (
          <ExtendProfileForm
            onClose={this.closeExtendForm}
            onExtend={this.handleProfileExtend}
            organization={this.props.organization}
            profile={profile}
          />
        )}

        {this.state.deleteFormOpen && (
          <DeleteProfileForm
            onClose={this.closeDeleteForm}
            onDelete={this.handleProfileDelete}
            profile={profile}
          />
        )}

        {this.state.renameFormOpen && (
          <RenameProfileForm
            onClose={this.closeRenameForm}
            onRename={this.handleProfileRename}
            profile={profile}
          />
        )}
      </>
    );
  }
}

export default withRouter(ProfileActions);
