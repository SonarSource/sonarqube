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
import { Link } from 'react-router';
import { sortBy } from 'lodash';
import * as isSameMinute from 'date-fns/is_same_minute';
import ChangesList from './ChangesList';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { parseDate } from '../../../helpers/dates';
import { ProfileChangelogEvent } from '../types';

interface Props {
  events: ProfileChangelogEvent[];
  organization: string | null;
}

export default function Changelog(props: Props) {
  let isEvenRow = false;
  const sortedRows = sortBy(
    props.events,
    // sort events by date, rounded to a minute, recent events first
    e => -Number(parseDate(e.date)),
    e => e.action
  );

  const rows = sortedRows.map((event, index) => {
    const prev = index > 0 ? sortedRows[index - 1] : null;
    const isSameDate = prev != null && isSameMinute(parseDate(prev.date), parseDate(event.date));
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
      <tr className={className} key={index}>
        <td className="thin nowrap">{!isBulkChange && <DateTimeFormatter date={event.date} />}</td>

        <td className="thin nowrap">
          {!isBulkChange &&
            (event.authorName ? (
              <span>{event.authorName}</span>
            ) : (
              <span className="note">System</span>
            ))}
        </td>

        <td className="thin nowrap">
          {!isBulkChange && translate('quality_profiles.changelog', event.action)}
        </td>

        <td className="quality-profile-changelog-rule-cell">
          <Link to={getRulesUrl({ rule_key: event.ruleKey }, props.organization)}>
            {event.ruleName}
          </Link>
        </td>

        <td>{event.params && <ChangesList changes={event.params} />}</td>
      </tr>
    );
  });

  return (
    <table className="data zebra-hover">
      <thead>
        <tr>
          <th className="thin nowrap">{translate('date')}</th>
          <th className="thin nowrap">{translate('user')}</th>
          <th className="thin nowrap">{translate('action')}</th>
          <th>{translate('rule')}</th>
          <th>{translate('parameters')}</th>
        </tr>
      </thead>
      <tbody>{rows}</tbody>
    </table>
  );
}
