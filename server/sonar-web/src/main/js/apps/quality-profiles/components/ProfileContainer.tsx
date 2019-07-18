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
import Helmet from 'react-helmet';
import { WithRouterProps } from 'react-router';
import ProfileHeader from '../details/ProfileHeader';
import { Profile } from '../types';
import ProfileNotFound from './ProfileNotFound';

interface Props {
  children: React.ReactElement<any>;
  location: {
    pathname: string;
    query: { key?: string; language: string; name: string };
  };
  organization: string | null;
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

export default class ProfileContainer extends React.PureComponent<Props & WithRouterProps> {
  componentDidMount() {
    const { location, profiles, router } = this.props;
    if (location.query.key) {
      // try to find a quality profile with the given key
      // if managed to find one, redirect to a new version
      // otherwise do nothing, `render` will show not found page
      const profile = profiles.find(profile => profile.key === location.query.key);
      if (profile) {
        router.replace({
          pathname: location.pathname,
          query: { language: profile.language, name: profile.name }
        });
      }
    }
  }

  render() {
    const { organization, profiles, location, ...other } = this.props;
    const { key, language, name } = location.query;

    if (key) {
      // if there is a `key` parameter,
      // then if we managed to find a quality profile with this key
      // then we will be redirected in `componentDidMount`
      // otherwise show `ProfileNotFound`
      const profile = profiles.find(profile => profile.key === location.query.key);
      return profile ? null : <ProfileNotFound organization={organization} />;
    }

    const profile = profiles.find(
      profile => profile.language === language && profile.name === name
    );

    if (!profile) {
      return <ProfileNotFound organization={organization} />;
    }

    const child = React.cloneElement(this.props.children, {
      organization,
      profile,
      profiles,
      ...other
    });

    return (
      <div id="quality-profile">
        <Helmet title={profile.name} />
        <ProfileHeader
          organization={organization}
          profile={profile}
          updateProfiles={this.props.updateProfiles}
        />
        {child}
      </div>
    );
  }
}
