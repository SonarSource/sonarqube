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
import { Button, Spinner } from '@sonarsource/echoes-react';
import * as React from 'react';
import { useLocation, useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { parseDate, toISO8601WithOffsetString } from '../../../helpers/dates';
import { translate } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { useGetQualityProfileChangelog } from '../../../queries/quality-profiles';
import { useStandardExperienceMode } from '../../../queries/settings';
import { QualityProfileChangelogFilterMode } from '../../../types/quality-profiles';
import { withQualityProfilesContext } from '../qualityProfilesContext';
import { Profile } from '../types';
import { getProfileChangelogPath } from '../utils';
import Changelog from './Changelog';
import ChangelogEmpty from './ChangelogEmpty';
import ChangelogSearch from './ChangelogSearch';

interface Props {
  profile: Profile;
}

function ChangelogContainer(props: Readonly<Props>) {
  const { profile } = props;
  const { data: isStandardMode } = useStandardExperienceMode();
  const router = useRouter();
  const {
    query: { since, to },
  } = useLocation();

  const filterMode = isStandardMode
    ? QualityProfileChangelogFilterMode.STANDARD
    : QualityProfileChangelogFilterMode.MQR;

  const {
    data: changeLogResponse,
    isLoading,
    fetchNextPage,
  } = useGetQualityProfileChangelog({
    since,
    to,
    profile,
    filterMode,
  });

  const events = changeLogResponse?.pages.flatMap((page) => page.events) ?? [];
  const total = changeLogResponse?.pages[0].paging.total;

  const handleDateRangeChange = ({ from, to }: { from?: Date; to?: Date }) => {
    const path = getProfileChangelogPath(profile.name, profile.language, {
      since: from && toISO8601WithOffsetString(from),
      to: to && toISO8601WithOffsetString(to),
    });
    router.push(path);
  };

  const handleReset = () => {
    const path = getProfileChangelogPath(profile.name, profile.language);
    router.replace(path);
  };

  const shouldDisplayFooter = isDefined(events) && isDefined(total) && events.length < total;

  return (
    <div className="sw-mt-4">
      <div className="sw-mb-2 sw-flex sw-gap-4 sw-items-center">
        <ChangelogSearch
          dateRange={{
            from: since ? parseDate(since) : undefined,
            to: to ? parseDate(to) : undefined,
          }}
          onDateRangeChange={handleDateRangeChange}
          onReset={handleReset}
        />
        <Spinner isLoading={isLoading} />
      </div>

      {isDefined(events) && events.length === 0 && <ChangelogEmpty />}

      {isDefined(events) && events.length > 0 && <Changelog events={events} />}

      {shouldDisplayFooter && (
        <footer className="sw-text-center sw-mt-2">
          <Button onClick={() => fetchNextPage()}>{translate('show_more')}</Button>
        </footer>
      )}
    </div>
  );
}

export default withQualityProfilesContext(ChangelogContainer);
