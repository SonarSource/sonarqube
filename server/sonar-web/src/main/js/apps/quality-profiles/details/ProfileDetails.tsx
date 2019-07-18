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
import { Exporter, Profile } from '../types';
import ProfileExporters from './ProfileExporters';
import ProfileInheritance from './ProfileInheritance';
import ProfilePermissions from './ProfilePermissions';
import ProfileProjects from './ProfileProjects';
import ProfileRules from './ProfileRules';

interface Props {
  exporters: Exporter[];
  organization: string | null;
  profile: Profile;
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

export default function ProfileDetails(props: Props) {
  const { organization, profile } = props;
  return (
    <div>
      <div className="quality-profile-grid">
        <div className="quality-profile-grid-left">
          <ProfileRules organization={organization} profile={profile} />
          <ProfileExporters
            exporters={props.exporters}
            organization={organization}
            profile={profile}
          />
          {profile.actions && profile.actions.edit && !profile.isBuiltIn && (
            <ProfilePermissions organization={organization || undefined} profile={profile} />
          )}
        </div>
        <div className="quality-profile-grid-right">
          <ProfileInheritance
            organization={organization}
            profile={profile}
            profiles={props.profiles}
            updateProfiles={props.updateProfiles}
          />
          <ProfileProjects organization={organization} profile={profile} />
        </div>
      </div>
    </div>
  );
}
