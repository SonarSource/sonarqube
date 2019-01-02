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
import { groupBy, orderBy } from 'lodash';
import ProfileRow from './ProfileRow';
import { Profile } from '../../api/quality-profiles';
import { translate } from '../../helpers/l10n';

interface Props {
  allProfiles: Profile[];
  profiles: Profile[];
  onChangeProfile: (oldProfile: string, newProfile: string) => Promise<void>;
}

export default function Table(props: Props) {
  const profilesByLanguage = groupBy(props.allProfiles, 'language');
  const orderedProfiles = orderBy(props.profiles, 'languageName');

  // set key to language to avoid destroying of component
  const profileRows = orderedProfiles.map(profile => (
    <ProfileRow
      key={profile.language}
      onChangeProfile={props.onChangeProfile}
      possibleProfiles={profilesByLanguage[profile.language]}
      profile={profile}
    />
  ));

  return (
    <div className="boxed-group boxed-group-inner">
      <table className="data zebra">
        <thead>
          <tr>
            <th className="thin nowrap">{translate('language')}</th>
            <th className="thin nowrap">{translate('quality_profile')}</th>
            {/* keep one empty cell for the spinner */}
            <th>&nbsp;</th>
          </tr>
        </thead>
        <tbody>{profileRows}</tbody>
      </table>
    </div>
  );
}
