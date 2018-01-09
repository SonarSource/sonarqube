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
import { Link } from 'react-router';
import { sortBy } from 'lodash';
import ProfileLink from '../components/ProfileLink';
import { getDeprecatedActiveRulesUrl } from '../../../helpers/urls';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { Profile } from '../types';

interface Props {
  organization: string | null;
  profiles: Profile[];
}

export default function EvolutionDeprecated(props: Props) {
  const profilesWithDeprecations = props.profiles.filter(
    profile => profile.activeDeprecatedRuleCount > 0
  );

  if (profilesWithDeprecations.length === 0) {
    return null;
  }

  const sortedProfiles = sortBy(profilesWithDeprecations, p => -p.activeDeprecatedRuleCount);

  return (
    <div className="boxed-group boxed-group-inner quality-profiles-evolution-deprecated">
      <div className="spacer-bottom">
        <strong>{translate('quality_profiles.deprecated_rules')}</strong>
      </div>
      <div className="spacer-bottom">
        {translateWithParameters(
          'quality_profiles.deprecated_rules_are_still_activated',
          profilesWithDeprecations.length
        )}
      </div>
      <ul>
        {sortedProfiles.map(profile => (
          <li key={profile.key} className="spacer-top">
            <div className="text-ellipsis">
              <ProfileLink
                className="link-no-underline"
                language={profile.language}
                name={profile.name}
                organization={props.organization}>
                {profile.name}
              </ProfileLink>
            </div>
            <div className="note">
              {profile.languageName}
              {', '}
              <Link
                to={getDeprecatedActiveRulesUrl({ qprofile: profile.key }, props.organization)}
                className="text-muted">
                {translateWithParameters(
                  'quality_profile.x_rules',
                  profile.activeDeprecatedRuleCount
                )}
              </Link>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
