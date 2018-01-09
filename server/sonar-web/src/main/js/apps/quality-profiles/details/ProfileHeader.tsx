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
import { Link, IndexLink } from 'react-router';
import ProfileLink from '../components/ProfileLink';
import ProfileActions from '../components/ProfileActions';
import ProfileDate from '../components/ProfileDate';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import { translate } from '../../../helpers/l10n';
import {
  isStagnant,
  getProfilesPath,
  getProfilesForLanguagePath,
  getProfileChangelogPath
} from '../utils';
import { Profile } from '../types';

interface Props {
  onRequestFail: (reasong: any) => void;
  profile: Profile;
  organization: string | null;
  updateProfiles: () => Promise<void>;
}

export default class ProfileHeader extends React.PureComponent<Props> {
  renderUpdateDate() {
    const { profile } = this.props;
    let inner = (
      <span>
        {translate('quality_profiles.updated_')} <ProfileDate date={profile.userUpdatedAt} />
      </span>
    );
    if (isStagnant(profile)) {
      inner = <span className="badge badge-normal-size badge-focus">{inner}</span>;
    }
    return <li className="small spacer-right">{inner}</li>;
  }

  renderUsageDate() {
    const { profile } = this.props;
    let inner = (
      <span>
        {translate('quality_profiles.used_')} <ProfileDate date={profile.lastUsed} />
      </span>
    );
    if (!profile.lastUsed) {
      inner = <span className="badge badge-normal-size badge-focus">{inner}</span>;
    }

    return <li className="small big-spacer-right">{inner}</li>;
  }

  render() {
    const { organization, profile } = this.props;

    return (
      <header className="page-header quality-profile-header">
        <div className="note spacer-bottom">
          <IndexLink to={getProfilesPath(organization)} className="text-muted">
            {translate('quality_profiles.page')}
          </IndexLink>
          {' / '}
          <Link
            to={getProfilesForLanguagePath(profile.language, organization)}
            className="text-muted">
            {profile.languageName}
          </Link>
        </div>

        <h1 className="page-title">
          <ProfileLink
            className="link-base-color"
            language={profile.language}
            name={profile.name}
            organization={organization}>
            <span>{profile.name}</span>
          </ProfileLink>
          {profile.isBuiltIn && (
            <BuiltInQualityProfileBadge className="spacer-left" tooltip={false} />
          )}
        </h1>

        <div className="pull-right">
          <ul className="list-inline" style={{ lineHeight: '24px' }}>
            {this.renderUpdateDate()}
            {this.renderUsageDate()}
            <li>
              <Link
                to={getProfileChangelogPath(profile.name, profile.language, organization)}
                className="button">
                {translate('changelog')}
              </Link>
            </li>
            <li>
              <ProfileActions
                className="pull-left"
                onRequestFail={this.props.onRequestFail}
                organization={organization}
                profile={profile}
                updateProfiles={this.props.updateProfiles}
              />
            </li>
          </ul>
        </div>

        {profile.isBuiltIn && (
          <div className="page-description">
            {translate('quality_profiles.built_in.description.1')}
            <br />
            {translate('quality_profiles.built_in.description.2')}
          </div>
        )}
      </header>
    );
  }
}
