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
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import DateFormatter from '../../../components/intl/DateFormatter';
import ProfileLink from '../components/ProfileLink';
import { Profile } from '../types';
import { isStagnant } from '../utils';

interface Props {
  organization: string | null;
  profiles: Profile[];
}

export default function EvolutionStagnant(props: Props) {
  const outdated = props.profiles.filter(profile => !profile.isBuiltIn && isStagnant(profile));

  if (outdated.length === 0) {
    return null;
  }

  return (
    <div className="boxed-group boxed-group-inner quality-profiles-evolution-stagnant">
      <div className="spacer-bottom">
        <strong>{translate('quality_profiles.stagnant_profiles')}</strong>
      </div>
      <div className="spacer-bottom">
        {translate('quality_profiles.not_updated_more_than_year')}
      </div>
      <ul>
        {outdated.map(profile => (
          <li className="spacer-top" key={profile.key}>
            <div className="text-ellipsis">
              <ProfileLink
                className="link-no-underline"
                language={profile.language}
                name={profile.name}
                organization={props.organization}>
                {profile.name}
              </ProfileLink>
            </div>
            {profile.rulesUpdatedAt && (
              <DateFormatter date={profile.rulesUpdatedAt} long={true}>
                {formattedDate => (
                  <div className="note">
                    {translateWithParameters(
                      'quality_profiles.x_updated_on_y',
                      profile.languageName,
                      formattedDate
                    )}
                  </div>
                )}
              </DateFormatter>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
