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
import { filter, slice, sortBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { ParsedAnalysis, Serie } from '../../types/project-activity';
import { Button } from '../controls/buttons';
import Modal from '../controls/Modal';
import DateFormatter from '../intl/DateFormatter';
import TimeFormatter from '../intl/TimeFormatter';
import { Alert } from '../ui/Alert';
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
      <Alert variant="info">
        {translate('project_activity.graphs.data_table.no_data_warning')}
      </Alert>
    );
  }

  const tableData = series.reduce((acc, serie) => {
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
      }
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
  }, {} as { [x: number]: DataTableEntry });

  const metrics = series.map(({ name }) => name);
  const rows = slice(
    sortBy(Object.values(tableData), ({ date }) => -date),
    0,
    MAX_DATA_TABLE_ROWS
  ).map(({ date, ...values }) => (
    <tr key={date.getTime()}>
      <td className="nowrap">
        <DateFormatter long={true} date={date} />
        <div className="small note">
          <TimeFormatter date={date} />
        </div>
      </td>
      {metrics.map((metric) => (
        <td key={metric} className="thin nowrap">
          {values[metric] || '-'}
        </td>
      ))}
      <td>
        <ul>
          {getAnalysisEventsForDate(analyses, date).map((event) => (
            <li className="little-spacer-bottom" key={event.key}>
              <EventInner event={event} readonly={true} />
            </li>
          ))}
        </ul>
      </td>
    </tr>
  ));

  const rowCount = rows.length;

  if (rowCount === 0) {
    const start = graphStartDate && <DateFormatter long={true} date={graphStartDate} />;
    const end = graphEndDate && <DateFormatter long={true} date={graphEndDate} />;
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
      <Alert variant="info">
        <FormattedMessage
          defaultMessage={translate(
            `project_activity.graphs.data_table.no_data_warning_check_dates${suffix}`
          )}
          id={`project_activity.graphs.data_table.no_data_warning_check_dates${suffix}`}
          values={{ start, end }}
        />
      </Alert>
    );
  }

  return renderModal(
    props,
    <>
      {rowCount === MAX_DATA_TABLE_ROWS && (
        <Alert variant="info">
          {translateWithParameters(
            'project_activity.graphs.data_table.max_lines_warning',
            MAX_DATA_TABLE_ROWS
          )}
        </Alert>
      )}
      <table className="spacer-top data zebra">
        <thead>
          <tr>
            <th>{translate('date')}</th>
            {series.map((serie) => (
              <th key={serie.name} className="thin nowrap">
                {serie.translatedName}
              </th>
            ))}
            <th>{translate('events')}</th>
          </tr>
        </thead>
        <tbody>{rows}</tbody>
      </table>
    </>
  );
}

function renderModal(props: DataTableModalProps, children: React.ReactNode) {
  const heading = translate('project_activity.graphs.data_table.title');
  return (
    <Modal onRequestClose={props.onClose} contentLabel={heading} size="medium">
      <div className="modal-head">
        <h2>{heading}</h2>
      </div>
      <div className="modal-body modal-container">{children}</div>
      <div className="modal-foot">
        <Button onClick={props.onClose}>{translate('close')}</Button>
      </div>
    </Modal>
  );
}
