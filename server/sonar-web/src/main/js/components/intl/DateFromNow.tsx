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
import { differenceInHours } from 'date-fns';
import * as React from 'react';
import { FormattedRelativeTime } from 'react-intl';
import { parseDate } from '../../helpers/dates';
import { translate } from '../../helpers/l10n';
import { ParsableDate } from '../../types/dates';
import DateTimeFormatter from './DateTimeFormatter';
import { getRelativeTimeProps } from './dateUtils';

export interface DateFromNowProps {
  children?: (formattedDate: string) => React.ReactNode;
  className?: string;
  date?: ParsableDate;
  hourPrecision?: boolean;
}

export default function DateFromNow(props: DateFromNowProps) {
  const { children: originalChildren = (f: string) => f, date, hourPrecision, className } = props;
  let children = originalChildren;

  if (!date) {
    /*
     * We return a JSX.Element to bypass typescript issue with functional components return type
     * (https://github.com/DefinitelyTyped/DefinitelyTyped/issues/20544)
     */
    // eslint-disable-next-line react/jsx-no-useless-fragment
    return <>{originalChildren(translate('never'))}</>;
  }

  if (hourPrecision && differenceInHours(Date.now(), parseDate(date)) < 1) {
    children = () => originalChildren(translate('less_than_1_hour_ago'));
  }

  const parsedDate = parseDate(date);

  const relativeTimeProps = getRelativeTimeProps(date);

  return (
    <DateTimeFormatter date={parsedDate}>
      {(formattedDate) => (
        <span className={className} title={formattedDate}>
          <FormattedRelativeTime {...relativeTimeProps}>
            {(d) => <>{children(d)}</>}
          </FormattedRelativeTime>
        </span>
      )}
    </DateTimeFormatter>
  );
}
