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
import RenameProfileView from '../views/RenameProfileView';
import CopyProfileView from '../views/CopyProfileView';
import DeleteProfileView from '../views/DeleteProfileView';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { setDefaultProfile } from '../../../api/quality-profiles';
import { getProfilePath, getProfileComparePath, getProfilesPath } from '../utils';
import type { Profile } from '../propTypes';

type Props = {
  canAdmin: boolean,
  organization: ?string,
  profile: Profile,
  updateProfiles: () => Promise<*>
};

export default class ProfileActions extends React.PureComponent {
  props: Props;

  static contextTypes = {
    router: React.PropTypes.object
  };

  handleRenameClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    new RenameProfileView({ profile: this.props.profile })
      .on('done', () => this.props.updateProfiles())
      .render();
  };

  handleCopyClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    new CopyProfileView({ profile: this.props.profile })
      .on('done', profile => {
        this.props.updateProfiles().then(() => {
          this.context.router.push(getProfilePath(profile.key, this.props.organization));
        });
      })
      .render();
  };

  handleSetDefaultClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    setDefaultProfile(this.props.profile.key).then(this.props.updateProfiles);
  };

  handleDeleteClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    new DeleteProfileView({ profile: this.props.profile })
      .on('done', () => {
        this.context.router.replace(getProfilesPath(this.props.organization));
        this.props.updateProfiles();
      })
      .render();
  };

  render() {
    const { profile, canAdmin } = this.props;

    // FIXME use org, name and lang
    const backupUrl = window.baseUrl +
      '/api/qualityprofiles/backup?profileKey=' +
      encodeURIComponent(profile.key);

    // FIXME getRulesUrl
    const activateMoreUrl = getRulesUrl({
      qprofile: profile.key,
      activation: 'false'
    });

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
            to={getProfileComparePath(profile.key, this.props.organization)}
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
      </ul>
    );
  }
}
