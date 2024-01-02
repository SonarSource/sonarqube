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
import { FormattedMessage } from 'react-intl';
import { NavLink } from 'react-router-dom';
import Link from '../../../components/common/Link';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Tooltip from '../../../components/controls/Tooltip';
import { useLocation } from '../../../components/hoc/withRouter';
import DateFromNow from '../../../components/intl/DateFromNow';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import ProfileActions from '../components/ProfileActions';
import ProfileLink from '../components/ProfileLink';
import { PROFILE_PATH } from '../constants';
import { Profile } from '../types';
import {
  getProfileChangelogPath,
  getProfilesForLanguagePath,
  isProfileComparePath,
} from '../utils';

interface Props {
  profile: Profile;
  isComparable: boolean;
  updateProfiles: () => Promise<void>;
}

export default function ProfileHeader(props: Props) {
  const { profile, isComparable, updateProfiles } = props;
  const location = useLocation();

  return (
    <div className="page-header quality-profile-header">
      <div className="note spacer-bottom">
        <NavLink end={true} to={PROFILE_PATH}>
          {translate('quality_profiles.page')}
        </NavLink>
        {' / '}
        <Link to={getProfilesForLanguagePath(profile.language)}>{profile.languageName}</Link>
      </div>

      <h1 className="page-title">
        <ProfileLink language={profile.language} name={profile.name}>
          <span>{profile.name}</span>
        </ProfileLink>
        {profile.isDefault && (
          <Tooltip overlay={translate('quality_profiles.list.default.help')}>
            <span className=" spacer-left badge">{translate('default')}</span>
          </Tooltip>
        )}
        {profile.isBuiltIn && (
          <BuiltInQualityProfileBadge className="spacer-left" tooltip={false} />
        )}
      </h1>
      {!isProfileComparePath(location.pathname) && (
        <div className="pull-right">
          <ul className="list-inline" style={{ lineHeight: '24px' }}>
            <li className="small spacer-right">
              {translate('quality_profiles.updated_')} <DateFromNow date={profile.rulesUpdatedAt} />
            </li>
            <li className="small big-spacer-right">
              {translate('quality_profiles.used_')} <DateFromNow date={profile.lastUsed} />
            </li>
            <li>
              <Link className="button" to={getProfileChangelogPath(profile.name, profile.language)}>
                {translate('changelog')}
              </Link>
            </li>

            <li>
              <ProfileActions
                className="pull-left"
                profile={profile}
                isComparable={isComparable}
                updateProfiles={updateProfiles}
              />
            </li>
          </ul>
        </div>
      )}

      {profile.isBuiltIn && (
        <div className="page-description">{translate('quality_profiles.built_in.description')}</div>
      )}

      {profile.parentKey && profile.parentName && (
        <div className="page-description">
          <FormattedMessage
            defaultMessage={translate('quality_profiles.extend_description')}
            id="quality_profiles.extend_description"
            values={{
              link: (
                <>
                  <Link to={getQualityProfileUrl(profile.parentName, profile.language)}>
                    {profile.parentName}
                  </Link>
                  <HelpTooltip
                    className="little-spacer-left"
                    overlay={translateWithParameters(
                      'quality_profiles.extend_description_help',
                      profile.parentName
                    )}
                  />
                </>
              ),
            }}
          />
        </div>
      )}
    </div>
  );
}
