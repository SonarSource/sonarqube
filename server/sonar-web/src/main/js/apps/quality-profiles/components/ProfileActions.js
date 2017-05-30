/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { Link } from 'react-router';
import RenameProfileForm from './RenameProfileForm';
import CopyProfileForm from './CopyProfileForm';
import DeleteProfileForm from './DeleteProfileForm';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { setDefaultProfile } from '../../../api/quality-profiles';
import { getProfilePath, getProfileComparePath, getProfilesPath } from '../utils';
import type { Profile } from '../propTypes';

type Props = {
  canAdmin: boolean,
  fromList: boolean,
  onRequestFail: Object => void,
  organization: ?string,
  profile: Profile,
  updateProfiles: () => Promise<*>
};

type State = {
  copyFormOpen: boolean,
  deleteFormOpen: boolean,
  renameFormOpen: boolean
};

export default class ProfileActions extends React.PureComponent {
  props: Props;
  state: State;

  static defaultProps = {
    fromList: false
  };

  static contextTypes = {
    router: React.PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      copyFormOpen: false,
      deleteFormOpen: false,
      renameFormOpen: false
    };
  }

  handleRenameClick = (event: Event) => {
    event.preventDefault();
    this.setState({ renameFormOpen: true });
  };

  handleProfileRename = (name: string) => {
    this.closeRenameForm();
    this.props.updateProfiles().then(() => {
      if (!this.props.fromList) {
        this.context.router.replace(
          getProfilePath(name, this.props.profile.language, this.props.organization)
        );
      }
    });
  };

  closeRenameForm = () => {
    this.setState({ renameFormOpen: false });
  };

  handleCopyClick = (event: Event) => {
    event.preventDefault();
    this.setState({ copyFormOpen: true });
  };

  handleProfileCopy = (name: string) => {
    this.props.updateProfiles().then(() => {
      this.context.router.push(
        getProfilePath(name, this.props.profile.language, this.props.organization)
      );
    });
  };

  closeCopyForm = () => {
    this.setState({ copyFormOpen: false });
  };

  handleSetDefaultClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    setDefaultProfile(this.props.profile.key).then(this.props.updateProfiles);
  };

  handleDeleteClick = (event: Event) => {
    event.preventDefault();
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
    const { profile, canAdmin } = this.props;

    // FIXME use org, name and lang
    const backupUrl =
      window.baseUrl + '/api/qualityprofiles/backup?profileKey=' + encodeURIComponent(profile.key);

    const activateMoreUrl = getRulesUrl(
      {
        qprofile: profile.key,
        activation: 'false'
      },
      this.props.organization
    );

    return (
      <ul className="dropdown-menu dropdown-menu-right">
        {canAdmin &&
          <li>
            <Link to={activateMoreUrl}>
              {translate('quality_profiles.activate_more_rules')}
            </Link>
          </li>}
        <li>
          <a id="quality-profile-backup" href={backupUrl}>
            {translate('backup_verb')}
          </a>
        </li>
        <li>
          <Link
            to={getProfileComparePath(profile.name, profile.language, this.props.organization)}
            id="quality-profile-compare">
            {translate('compare')}
          </Link>
        </li>
        {canAdmin &&
          <li>
            <a id="quality-profile-copy" href="#" onClick={this.handleCopyClick}>
              {translate('copy')}
            </a>
          </li>}
        {canAdmin &&
          <li>
            <a id="quality-profile-rename" href="#" onClick={this.handleRenameClick}>
              {translate('rename')}
            </a>
          </li>}
        {canAdmin &&
          !profile.isDefault &&
          <li>
            <a id="quality-profile-set-as-default" href="#" onClick={this.handleSetDefaultClick}>
              {translate('set_as_default')}
            </a>
          </li>}
        {canAdmin &&
          !profile.isDefault &&
          <li>
            <a id="quality-profile-delete" href="#" onClick={this.handleDeleteClick}>
              {translate('delete')}
            </a>
          </li>}

        {this.state.copyFormOpen &&
          <CopyProfileForm
            onClose={this.closeCopyForm}
            onCopy={this.handleProfileCopy}
            onRequestFail={this.props.onRequestFail}
            profile={profile}
          />}

        {this.state.deleteFormOpen &&
          <DeleteProfileForm
            onClose={this.closeDeleteForm}
            onDelete={this.handleProfileDelete}
            onRequestFail={this.props.onRequestFail}
            profile={profile}
          />}

        {this.state.renameFormOpen &&
          <RenameProfileForm
            onClose={this.closeRenameForm}
            onRename={this.handleProfileRename}
            onRequestFail={this.props.onRequestFail}
            profile={profile}
          />}
      </ul>
    );
  }
}
