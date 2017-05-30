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
import PageHeader from './PageHeader';
import Evolution from './Evolution';
import ProfilesList from './ProfilesList';
import type { Profile } from '../propTypes';

type Props = {
  canAdmin: boolean,
  languages: Array<{ key: string, name: string }>,
  location: { query: { [string]: string } },
  onRequestFail: Object => void,
  organization?: string,
  profiles: Array<Profile>,
  updateProfiles: () => Promise<*>
};

export default class HomeContainer extends React.PureComponent {
  props: Props;

  render() {
    return (
      <div>
        <PageHeader {...this.props} />

        <div className="page-with-sidebar">
          <div className="page-main">
            <ProfilesList {...this.props} />
          </div>
          <div className="page-sidebar">
            <Evolution {...this.props} />
          </div>
        </div>
      </div>
    );
  }
}
