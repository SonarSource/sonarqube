/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import moment from 'moment';
import { translate } from '../../../helpers/l10n';

export default class ProfileEvolution extends React.Component {
  render () {
    const { profile } = this.props;

    return (
        <div className="quality-profile-evolution">
          <div>
            <h6 className="little-spacer-bottom">
              {translate('quality_profiles.list.updated')}
            </h6>
            {profile.userUpdatedAt ? (
                <div>
                  {moment(profile.userUpdatedAt).format('LL')}
                </div>
            ) : (
                <div className="note">
                  {translate('never')}
                </div>
            )}
          </div>
          <div>
            <h6 className="little-spacer-bottom">
              {translate('quality_profiles.list.used')}
            </h6>
            {profile.lastUsed ? (
                <div>
                  {moment(profile.lastUsed).format('LL')}
                </div>
            ) : (
                <div className="note">
                  {translate('never')}
                </div>
            )}
          </div>
        </div>
    );
  }
}
