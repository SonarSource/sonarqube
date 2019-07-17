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
import { IndexLink, Link } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import ProfileActions from '../components/ProfileActions';
import ProfileDate from '../components/ProfileDate';
import ProfileLink from '../components/ProfileLink';
import { Profile } from '../types';
import { getProfileChangelogPath, getProfilesForLanguagePath, getProfilesPath } from '../utils';

interface Props {
  profile: Profile;
  organization: string | null;
  updateProfiles: () => Promise<void>;
}

export default class ProfileHeader extends React.PureComponent<Props> {
  render() {
    const { organization, profile } = this.props;

    return (
      <header className="page-header quality-profile-header">
        <div className="note spacer-bottom">
          <IndexLink className="text-muted" to={getProfilesPath(organization)}>
            {translate('quality_profiles.page')}
          </IndexLink>
          {' / '}
          <Link
            className="text-muted"
            to={getProfilesForLanguagePath(profile.language, organization)}>
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
            <li className="small spacer-right">
              {translate('quality_profiles.updated_')} <ProfileDate date={profile.rulesUpdatedAt} />
            </li>
            <li className="small big-spacer-right">
              {translate('quality_profiles.used_')} <ProfileDate date={profile.lastUsed} />
            </li>
            <li>
              <Link
                className="button"
                to={getProfileChangelogPath(profile.name, profile.language, organization)}>
                {translate('changelog')}
              </Link>
            </li>
            <li>
              <ProfileActions
                className="pull-left"
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
