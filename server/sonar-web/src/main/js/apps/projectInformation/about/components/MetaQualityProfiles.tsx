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
import { Badge, Link, SubHeading } from 'design-system';
import React, { useContext, useEffect } from 'react';
import { ComponentQualityProfile } from '~sonar-aligned/types/component';
import { searchRules } from '../../../../api/rules';
import { LanguagesContext } from '../../../../app/components/languages/LanguagesContext';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../../helpers/urls';
import { Languages } from '../../../../types/languages';
import { Dict } from '../../../../types/types';

interface Props {
  profiles: ComponentQualityProfile[];
}

export function MetaQualityProfiles({ profiles }: Readonly<Props>) {
  const [deprecatedByKey, setDeprecatedByKey] = React.useState<Dict<number>>({});
  const languages = useContext(LanguagesContext);

  useEffect(() => {
    const existingProfiles = profiles.filter((p) => !p.deleted);
    const requests = existingProfiles.map((profile) => {
      const data = {
        activation: 'true',
        ps: 1,
        qprofile: profile.key,
        statuses: 'DEPRECATED',
      };
      return searchRules(data).then((r) => r.paging.total);
    });
    Promise.all(requests).then(
      (responses) => {
        const deprecatedByKey: Dict<number> = {};
        responses.forEach((count, i) => {
          const profileKey = existingProfiles[i].key;
          deprecatedByKey[profileKey] = count;
        });
        setDeprecatedByKey(deprecatedByKey);
      },
      () => {},
    );
  }, [profiles]);

  return (
    <div>
      <SubHeading id="quality-profiles-list">{translate('overview.quality_profiles')}</SubHeading>

      <ul className="sw-flex sw-flex-col sw-gap-2" aria-labelledby="quality-profiles-list">
        {profiles.map((profile) => (
          <ProfileItem
            key={profile.key}
            profile={profile}
            languages={languages}
            deprecatedByKey={deprecatedByKey}
          />
        ))}
      </ul>
    </div>
  );
}

function ProfileItem({
  profile,
  languages,
  deprecatedByKey,
}: {
  deprecatedByKey: Dict<number>;
  languages: Languages;
  profile: ComponentQualityProfile;
}) {
  const languageFromStore = languages[profile.language];
  const languageName = languageFromStore ? languageFromStore.name : profile.language;
  const count = deprecatedByKey[profile.key] || 0;

  return (
    <li>
      <div className="sw-grid sw-grid-cols-[1fr_3fr]">
        <span>{languageName}</span>
        <div>
          {profile.deleted ? (
            <Tooltip
              key={profile.key}
              content={translateWithParameters('overview.deleted_profile', profile.name)}
            >
              <div className="project-info-deleted-profile">{profile.name}</div>
            </Tooltip>
          ) : (
            <>
              <Link to={getQualityProfileUrl(profile.name, profile.language)}>
                <span
                  aria-label={translateWithParameters(
                    'overview.link_to_x_profile_y',
                    languageName,
                    profile.name,
                  )}
                >
                  {profile.name}
                </span>
              </Link>
              {count > 0 && (
                <Tooltip
                  key={profile.key}
                  content={translateWithParameters('overview.deprecated_profile', count)}
                >
                  <span className="sw-ml-6">
                    <Badge variant="deleted">{translate('deprecated')}</Badge>
                  </span>
                </Tooltip>
              )}
            </>
          )}
        </div>
      </div>
    </li>
  );
}

export default MetaQualityProfiles;
