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
import styled from '@emotion/styled';
import { FlagMessage, Modal } from 'design-system';
import { filter, slice, sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { ParsedAnalysis, Serie } from '../../types/project-activity';
import DateFormatter from '../intl/DateFormatter';
import TimeFormatter from '../intl/TimeFormatter';
import EventInner from './EventInner';
import { getAnalysisEventsForDate } from './utils';

export interface DataTableModalProps {
  analyses: ParsedAnalysis[];
  graphEndDate?: Date;
  graphStartDate?: Date;
  series: Serie[];
  onClose: () => void;
}

type DataTableEntry = { date: Date } & { [x: string]: string | undefined };

export const MAX_DATA_TABLE_ROWS = 100;

export default function DataTableModal(props: DataTableModalProps) {
  const { analyses, series, graphEndDate, graphStartDate } = props;

  if (series.length === 0) {
    return renderModal(
      props,
      <FlagMessage variant="warning">
        {translate('project_activity.graphs.data_table.no_data_warning')}
      </FlagMessage>,
    );
  }

  const tableData = series.reduce(
    (acc, serie) => {
      const data = filter(
        serie.data,
        // Make sure we respect the date filtering. On the graph, this is done by dynamically
        // "zooming" on the series. Here, we actually have to "cut off" part of the serie's
        // data points.
        ({ x }) => {
          if (graphEndDate && x > graphEndDate) {
            return false;
          }
          if (graphStartDate && x < graphStartDate) {
            return false;
          }
          return true;
        },
      );

      data.forEach(({ x, y }) => {
        const key = x.getTime();
        if (acc[key] === undefined) {
          acc[key] = { date: x } as DataTableEntry;
        }

        if (y !== undefined && !(typeof y === 'number' && isNaN(y))) {
          acc[key][serie.name] = formatMeasure(y, serie.type);
        }
      });

      return acc;
    },
    {} as { [x: number]: DataTableEntry },
  );

  const metrics = series.map(({ name }) => name);
  const rows = slice(
    sortBy(Object.values(tableData), ({ date }) => -date),
    0,
    MAX_DATA_TABLE_ROWS,
  ).map(({ date, ...values }) => (
    <tr key={date.getTime()}>
      <td className="sw-whitespace-nowrap">
        <DateFormatter long date={date} />
        <div className="sw-text-xs">
          <TimeFormatter date={date} />
        </div>
      </td>
      {metrics.map((metric) => (
        <td key={metric} className="sw-whitespace-nowrap sw-w-20">
          {values[metric] ?? '-'}
        </td>
      ))}
      <td>
        <ul>
          {getAnalysisEventsForDate(analyses, date).map((event) => (
            <li className="sw-mb-1" key={event.key}>
              <EventInner event={event} readonly />
            </li>
          ))}
        </ul>
      </td>
    </tr>
  ));

  const rowCount = rows.length;

  if (rowCount === 0) {
    const start = graphStartDate && <DateFormatter long date={graphStartDate} />;
    const end = graphEndDate && <DateFormatter long date={graphEndDate} />;
    let suffix = '';
    if (start && end) {
      suffix = '_x_y';
    } else if (start) {
      suffix = '_x';
    } else if (end) {
      suffix = '_y';
    }
    return renderModal(
      props,
      <FlagMessage variant="warning">
        <FormattedMessage
          defaultMessage={translate(
            `project_activity.graphs.data_table.no_data_warning_check_dates${suffix}`,
          )}
          id={`project_activity.graphs.data_table.no_data_warning_check_dates${suffix}`}
          values={{ start, end }}
        />
      </FlagMessage>,
    );
  }

  return renderModal(
    props,
    <>
      {rowCount === MAX_DATA_TABLE_ROWS && (
        <FlagMessage variant="warning">
          {translateWithParameters(
            'project_activity.graphs.data_table.max_lines_warning',
            MAX_DATA_TABLE_ROWS,
          )}
        </FlagMessage>
      )}
      <StyledTable className="sw-mt-2">
        <thead>
          <tr>
            <th>{translate('date')}</th>
            {series.map((serie) => (
              <th key={serie.name} className="sw-whitespace-nowrap sw-w-20">
                {serie.translatedName}
              </th>
            ))}
            <th>{translate('events')}</th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </StyledTable>
    </>,
  );
}

function renderModal(props: DataTableModalProps, children: React.ReactNode) {
  const heading = translate('project_activity.graphs.data_table.title');
  return (
    <Modal
      headerTitle={heading}
      isLarge
      onClose={props.onClose}
      body={children}
      primaryButton={null}
      secondaryButtonLabel={translate('close')}
    />
  );
}

const StyledTable = styled.table`
  width: 100%;
  & > thead > tr > th {
    position: relative;
    vertical-align: top;
    line-height: 18px;
    padding: 8px 10px;
    border-bottom: 1px solid var(--barBorderColor);
    font-weight: 600;
  }

  & > thead > tr > th > .small {
    display: block;
    line-height: 1.4;
    font-weight: 400;
  }

  & > tfoot > tr > td {
    font-size: 93%;
    color: var(--secondFontColor);
    padding: 5px;
  }

  & > tbody > tr > td {
    position: relative;
    padding: 8px 10px;
    line-height: 16px;
  }

  & > tbody > tr > td.text-middle {
    vertical-align: middle;
  }
`;
