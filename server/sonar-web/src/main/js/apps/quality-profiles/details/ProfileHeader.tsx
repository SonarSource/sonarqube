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

import { Helmet } from 'react-helmet-async';
import { Badge, Breadcrumbs, HoverLink, Link, PageContentFontWrapper } from '~design-system';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import DateFromNow from '../../../components/intl/DateFromNow';
import { AdminPageHeader } from '../../../components/ui/AdminPageHeader';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import ProfileActions from '../components/ProfileActions';
import { PROFILE_PATH } from '../constants';
import { QualityProfilePath } from '../routes';
import { Profile } from '../types';
import {
  getProfileChangelogPath,
  getProfilesForLanguagePath,
  isProfileComparePath,
} from '../utils';

interface Props {
  organization: string;
  isComparable: boolean;
  profile: Profile;
  updateProfiles: () => Promise<void>;
}

export default function ProfileHeader(props: Props) {
  const { organization, profile, isComparable, updateProfiles } = props;
  const location = useLocation();
  const isComparePage = location.pathname.endsWith(`/${QualityProfilePath.COMPARE}`);
  const isChangeLogPage = location.pathname.endsWith(`/${QualityProfilePath.CHANGELOG}`);

  return (
    <div className="it__quality-profiles__header">
      {(isComparePage || isChangeLogPage) && (
        <Helmet
          defer={false}
          title={translateWithParameters(
            isChangeLogPage
              ? 'quality_profiles.page_title_changelog_x'
              : 'quality_profiles.page_title_compare_x',
            profile.name,
          )}
        />
      )}

      <Breadcrumbs className="sw-mb-6">
        <HoverLink to={`/organizations/${organization}/${PROFILE_PATH}`}>{translate('quality_profiles.page')}</HoverLink>
        <HoverLink to={getProfilesForLanguagePath(profile.language, organization)}>
          {profile.languageName}
        </HoverLink>
      </Breadcrumbs>

      <AdminPageHeader
        description={profile.isBuiltIn && translate('quality_profiles.built_in.description')}
        title={
          <span className="sw-inline-flex sw-items-center sw-gap-2">
            {profile.name}
            {profile.isBuiltIn && <BuiltInQualityProfileBadge tooltip={false} />}
            {profile.isDefault && <Badge>{translate('default')}</Badge>}
          </span>
        }
      >
        <div className="sw-flex sw-items-center sw-gap-3 sw-self-start">
          {!isProfileComparePath(location.pathname) && (
            <PageContentFontWrapper className="sw-typo-default sw-flex sw-gap-3">
              <div>
                <strong className="sw-typo-semibold">
                  {translate('quality_profiles.updated_')}
                </strong>{' '}
                <DateFromNow date={profile.rulesUpdatedAt} />
              </div>
              <div>
                <strong className="sw-typo-semibold">{translate('quality_profiles.used_')}</strong>{' '}
                <DateFromNow date={profile.lastUsed} />
              </div>

              {!isChangeLogPage && (
                <div>
                  <Link
                    className="it__quality-profiles__changelog"
                    to={getProfileChangelogPath(profile.name, profile.language, organization)}
                  >
                    {translate('see_changelog')}
                  </Link>
                </div>
              )}
            </PageContentFontWrapper>
          )}

          <ProfileActions
            organization={organization}
            profile={profile}
            isComparable={isComparable}
            updateProfiles={updateProfiles}
          />
        </div>
      </AdminPageHeader>
    </div>
  );
}
