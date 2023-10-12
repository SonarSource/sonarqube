/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { isSameMinute } from 'date-fns';
import {
  CellComponent,
  ContentCell,
  Link,
  Note,
  Table,
  TableRow,
  TableRowInteractive,
} from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import { CleanCodeAttributePill } from '../../../components/shared/CleanCodeAttributePill';
import SoftwareImpactPill from '../../../components/shared/SoftwareImpactPill';
import { parseDate } from '../../../helpers/dates';
import { getRulesUrl } from '../../../helpers/urls';
import { ProfileChangelogEvent } from '../types';
import ChangesList from './ChangesList';

interface Props {
  events: ProfileChangelogEvent[];
}

export default function Changelog(props: Props) {
  const intl = useIntl();

  const sortedRows = sortBy(
    props.events,
    // sort events by date, rounded to a minute, recent events first
    (e) => -Number(parseDate(e.date)),
    (e) => e.action,
  );

  const rows = sortedRows.map((event, index) => {
    const prev = index > 0 ? sortedRows[index - 1] : null;
    const isSameDate = prev != null && isSameMinute(parseDate(prev.date), parseDate(event.date));
    const isBulkChange =
      prev != null &&
      isSameDate &&
      prev.authorName === event.authorName &&
      prev.action === event.action;

    return (
      <TableRowInteractive key={index}>
        <ContentCell className="sw-whitespace-nowrap sw-align-top">
          {!isBulkChange && <DateTimeFormatter date={event.date} />}
        </ContentCell>

        <ContentCell className="sw-whitespace-nowrap sw-align-top sw-max-w-[120px]">
          {!isBulkChange && (event.authorName ? event.authorName : <Note>System</Note>)}
        </ContentCell>

        <ContentCell className="sw-whitespace-nowrap sw-align-top">
          {!isBulkChange &&
            intl.formatMessage({ id: `quality_profiles.changelog.${event.action}` })}
        </ContentCell>

        <CellComponent className="sw-align-top">
          {event.ruleName && (
            <Link to={getRulesUrl({ rule_key: event.ruleKey })}>{event.ruleName}</Link>
          )}
          <div className="sw-mt-3 sw-flex sw-gap-2">
            {event.cleanCodeAttributeCategory && (
              <CleanCodeAttributePill
                cleanCodeAttributeCategory={event.cleanCodeAttributeCategory}
              />
            )}
            {event.impacts?.map((impact) => (
              <SoftwareImpactPill
                key={impact.softwareQuality}
                quality={impact.softwareQuality}
                severity={impact.severity}
              />
            ))}
          </div>
        </CellComponent>

        <ContentCell className="sw-align-top sw-max-w-[400px]">
          {event.params && <ChangesList changes={event.params} />}
        </ContentCell>
      </TableRowInteractive>
    );
  });

  return (
    <Table
      columnCount={5}
      columnWidths={['1%', '1%', '1%', 'auto', 'auto']}
      header={
        <TableRow>
          <ContentCell>{intl.formatMessage({ id: 'date' })}</ContentCell>
          <ContentCell>{intl.formatMessage({ id: 'user' })}</ContentCell>
          <ContentCell>{intl.formatMessage({ id: 'action' })}</ContentCell>
          <ContentCell>{intl.formatMessage({ id: 'rule' })}</ContentCell>
          <ContentCell>{intl.formatMessage({ id: 'updates' })}</ContentCell>
        </TableRow>
      }
    >
      {rows}
    </Table>
  );
}
