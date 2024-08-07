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
import { Spinner } from 'design-system';
import * as React from 'react';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { useProfilesCompareQuery } from '../../../queries/quality-profiles';
import { useGetValueQuery } from '../../../queries/settings';
import { SettingsKey } from '../../../types/settings';
import { withQualityProfilesContext } from '../qualityProfilesContext';
import { Profile } from '../types';
import { getProfileComparePath } from '../utils';
import ComparisonForm from './ComparisonForm';
import ComparisonResults from './ComparisonResults';

interface Props {
  profile: Profile;
  profiles: Profile[];
}

export function ComparisonContainer(props: Readonly<Props>) {
  const { profile, profiles } = props;
  const location = useLocation();
  const router = useRouter();
  const { data: inheritRulesSetting } = useGetValueQuery({
    key: SettingsKey.QPAdminCanDisableInheritedRules,
  });
  const canDeactivateInheritedRules = inheritRulesSetting?.value === 'true';

  const { withKey } = location.query;
  const {
    data: compareResults,
    isLoading,
    refetch,
  } = useProfilesCompareQuery(profile.key, withKey);

  const handleCompare = (withKey: string) => {
    const path = getProfileComparePath(profile.name, profile.language, withKey);
    router.push(path);
  };

  const refresh = async () => {
    await refetch();
  };

  return (
    <div className="sw-body-sm">
      <div className="sw-flex sw-items-center">
        <ComparisonForm
          onCompare={handleCompare}
          profile={profile}
          profiles={profiles}
          withKey={withKey}
        />

        <Spinner className="sw-ml-2" loading={isLoading} />
      </div>

      {compareResults && (
        <ComparisonResults
          inLeft={compareResults.inLeft}
          inRight={compareResults.inRight}
          left={compareResults.left}
          leftProfile={profile}
          modified={compareResults.modified}
          refresh={refresh}
          right={compareResults.right}
          rightProfile={profiles.find((p) => p.key === withKey)}
          canDeactivateInheritedRules={canDeactivateInheritedRules}
        />
      )}
    </div>
  );
}

export default withQualityProfilesContext(ComparisonContainer);
