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
import * as React from 'react';
import { Link } from 'react-router';
import ChangesList from './ChangesList';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { differenceInSeconds } from '../../../helpers/dates';
import { ProfileChangelogEvent } from '../types';

interface Props {
  events: ProfileChangelogEvent[];
  organization: string | null;
}

export default function Changelog(props: Props) {
  let isEvenRow = false;

  const rows = props.events.map((event, index) => {
    const prev = index > 0 ? props.events[index - 1] : null;
    const isSameDate =
      prev != null && differenceInSeconds(new Date(prev.date), new Date(event.date)) < 10;
    const isBulkChange =
      prev != null &&
      isSameDate &&
      prev.authorName === event.authorName &&
      prev.action === event.action;

    if (!isBulkChange) {
      isEvenRow = !isEvenRow;
    }

    const className = 'js-profile-changelog-event ' + (isEvenRow ? 'even' : 'odd');

    return (
      <tr key={index} className={className}>
        <td className="thin nowrap">
          {!isBulkChange && <DateTimeFormatter date={event.date} />}
        </td>

        <td className="thin nowrap">
          {!isBulkChange &&
            (event.authorName
              ? <span>
                  {event.authorName}
                </span>
              : <span className="note">System</span>)}
        </td>

        <td className="thin nowrap">
          {!isBulkChange && translate('quality_profiles.changelog', event.action)}
        </td>

        <td style={{ lineHeight: '1.5' }}>
          <Link to={getRulesUrl({ rule_key: event.ruleKey }, props.organization)}>
            {event.ruleName}
          </Link>
        </td>

        <td className="thin nowrap">
          {event.params && <ChangesList changes={event.params} />}
        </td>
      </tr>
    );
  });

  return (
    <table className="data zebra-hover">
      <thead>
        <tr>
          <th className="thin nowrap">
            {translate('date')} <i className="icon-sort-desc" />
          </th>
          <th className="thin nowrap">
            {translate('user')}
          </th>
          <th className="thin nowrap">
            {translate('action')}
          </th>
          <th>
            {translate('rule')}
          </th>
          <th className="thin nowrap">
            {translate('parameters')}
          </th>
        </tr>
      </thead>
      <tbody>
        {rows}
      </tbody>
    </table>
  );
}
