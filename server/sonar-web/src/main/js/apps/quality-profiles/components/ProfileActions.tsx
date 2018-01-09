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
import * as PropTypes from 'prop-types';
import RenameProfileForm from './RenameProfileForm';
import CopyProfileForm from './CopyProfileForm';
import DeleteProfileForm from './DeleteProfileForm';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { setDefaultProfile } from '../../../api/quality-profiles';
import { getProfilePath, getProfileComparePath, getProfilesPath } from '../utils';
import { Profile } from '../types';
import ActionsDropdown, {
  ActionsDropdownItem,
  ActionsDropdownDivider
} from '../../../components/controls/ActionsDropdown';

interface Props {
  className?: string;
  fromList?: boolean;
  onRequestFail: (reasong: any) => void;
  organization: string | null;
  profile: Profile;
  updateProfiles: () => Promise<void>;
}

interface State {
  copyFormOpen: boolean;
  deleteFormOpen: boolean;
  renameFormOpen: boolean;
}

export default class ProfileActions extends React.PureComponent<Props, State> {
  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      copyFormOpen: false,
      deleteFormOpen: false,
      renameFormOpen: false
    };
  }

  handleRenameClick = () => {
    this.setState({ renameFormOpen: true });
  };

  handleProfileRename = (name: string) => {
    this.closeRenameForm();
    this.props.updateProfiles().then(
      () => {
        if (!this.props.fromList) {
          this.context.router.replace(
            getProfilePath(name, this.props.profile.language, this.props.organization)
          );
        }
      },
      () => {}
    );
  };

  closeRenameForm = () => {
    this.setState({ renameFormOpen: false });
  };

  handleCopyClick = () => {
    this.setState({ copyFormOpen: true });
  };

  handleProfileCopy = (name: string) => {
    this.props.updateProfiles().then(
      () => {
        this.context.router.push(
          getProfilePath(name, this.props.profile.language, this.props.organization)
        );
      },
      () => {}
    );
  };

  closeCopyForm = () => {
    this.setState({ copyFormOpen: false });
  };

  handleSetDefaultClick = () => {
    setDefaultProfile(this.props.profile.key).then(this.props.updateProfiles, () => {});
  };

  handleDeleteClick = () => {
    this.setState({ deleteFormOpen: true });
  };

  handleProfileDelete = () => {
    this.context.router.replace(getProfilesPath(this.props.organization));
    this.props.updateProfiles();
  };

  closeDeleteForm = () => {
    this.setState({ deleteFormOpen: false });
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
      <ActionsDropdown className={this.props.className}>
        {actions.edit && (
          <ActionsDropdownItem to={activateMoreUrl} id="quality-profile-activate-more-rules">
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
          <ActionsDropdownItem id="quality-profile-copy" onClick={this.handleCopyClick}>
            {translate('copy')}
          </ActionsDropdownItem>
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

        {this.state.copyFormOpen && (
          <CopyProfileForm
            onClose={this.closeCopyForm}
            onCopy={this.handleProfileCopy}
            onRequestFail={this.props.onRequestFail}
            profile={profile}
          />
        )}

        {this.state.deleteFormOpen && (
          <DeleteProfileForm
            onClose={this.closeDeleteForm}
            onDelete={this.handleProfileDelete}
            onRequestFail={this.props.onRequestFail}
            profile={profile}
          />
        )}

        {this.state.renameFormOpen && (
          <RenameProfileForm
            onClose={this.closeRenameForm}
            onRename={this.handleProfileRename}
            onRequestFail={this.props.onRequestFail}
            profile={profile}
          />
        )}
      </ActionsDropdown>
    );
  }
}
