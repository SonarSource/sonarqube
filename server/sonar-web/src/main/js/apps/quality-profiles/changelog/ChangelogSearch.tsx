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
import { ButtonSecondary, DateRangePicker } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';

interface ChangelogSearchProps {
  dateRange: { from?: Date; to?: Date } | undefined;
  onDateRangeChange: (range: { from?: Date; to?: Date }) => void;
  onReset: () => void;
}

export default function ChangelogSearch(props: ChangelogSearchProps) {
  const { dateRange } = props;

  const intl = useIntl();

  return (
    <div className="sw-flex sw-gap-2">
      <DateRangePicker
        clearButtonLabel={intl.formatMessage({ id: 'clear' })}
        fromLabel={intl.formatMessage({ id: 'start_date' })}
        inputSize="small"
        separatorText={intl.formatMessage({ id: 'to_' })}
        toLabel={intl.formatMessage({ id: 'end_date' })}
        onChange={props.onDateRangeChange}
        value={dateRange}
      />
      <ButtonSecondary className="sw-ml-2 sw-align-top" onClick={props.onReset}>
        {intl.formatMessage({ id: 'reset_verb' })}
      </ButtonSecondary>
    </div>
  );
}
