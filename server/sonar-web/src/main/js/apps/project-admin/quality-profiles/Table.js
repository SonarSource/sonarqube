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
import React from 'react';
import PropTypes from 'prop-types';
import { groupBy, orderBy } from 'lodash';
import ProfileRow from './ProfileRow';
import { translate } from '../../../helpers/l10n';

export default class Table extends React.PureComponent {
  static propTypes = {
    allProfiles: PropTypes.array.isRequired,
    profiles: PropTypes.array.isRequired,
    onChangeProfile: PropTypes.func.isRequired
  };

  renderHeader() {
    // keep one empty cell for the spinner
    return (
      <thead>
        <tr>
          <th className="thin nowrap">
            {translate('language')}
          </th>
          <th className="thin nowrap">
            {translate('quality_profile')}
          </th>
          <th>&nbsp;</th>
        </tr>
      </thead>
    );
  }

  render() {
    const profilesByLanguage = groupBy(this.props.allProfiles, 'language');
    const orderedProfiles = orderBy(this.props.profiles, 'languageName');

    // set key to language to avoid destroying of component
    const profileRows = orderedProfiles.map(profile =>
      <ProfileRow
        key={profile.language}
        profile={profile}
        possibleProfiles={profilesByLanguage[profile.language]}
        onChangeProfile={this.props.onChangeProfile}
      />
    );

    return (
      <table className="data zebra">
        {this.renderHeader()}
        <tbody>
          {profileRows}
        </tbody>
      </table>
    );
  }
}
