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
import { Helmet } from 'react-helmet-async';
import { Outlet, useSearchParams } from 'react-router-dom';
import { useLocation } from '../../../components/hoc/withRouter';
import ProfileHeader from '../details/ProfileHeader';
import { QualityProfilesContextProps, withQualityProfilesContext } from '../qualityProfilesContext';
import ProfileNotFound from './ProfileNotFound';

export function ProfileContainer(props: QualityProfilesContextProps) {
  const [_, setSearchParams] = useSearchParams();
  const location = useLocation();

  const { key, language, name } = location.query;

  const { profiles } = props;

  // try to find a quality profile with the given key
  // if managed to find one, redirect to a new version
  // otherwise show not found page
  const profileForKey = key && profiles.find((p) => p.key === location.query.key);

  React.useEffect(() => {
    if (profileForKey) {
      setSearchParams({ language: profileForKey.language, name: profileForKey.name });
    }
  });

  if (key) {
    return profileForKey ? null : <ProfileNotFound />;
  }

  const filteredProfiles = profiles.filter((p) => p.language === language);
  const profile = filteredProfiles.find((p) => p.name === name);

  if (!profile) {
    return <ProfileNotFound />;
  }

  const context: QualityProfilesContextProps = {
    profile,
    ...props,
  };

  return (
    <div id="quality-profile">
      <Helmet defer={false} title={profile.name} />
      <ProfileHeader
        profile={profile}
        isComparable={filteredProfiles.length > 1}
        updateProfiles={props.updateProfiles}
      />
      <Outlet context={context} />
    </div>
  );
}

export default withQualityProfilesContext(ProfileContainer);
