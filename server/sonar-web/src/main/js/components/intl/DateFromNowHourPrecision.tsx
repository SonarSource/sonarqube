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
import { DateSource } from 'react-intl';
import DateFromNow from './DateFromNow';
import DateTimeFormatter from './DateTimeFormatter';
import Tooltip from '../controls/Tooltip';
import { differenceInHours } from '../../helpers/dates';
import { translate } from '../../helpers/l10n';

interface Props {
  children?: (formattedDate: string) => React.ReactNode;
  date?: DateSource;
}

export default class DateFromNowHourPrecision extends React.PureComponent<Props> {
  render() {
    const { children, date } = this.props;

    let overrideDate: string | undefined;
    if (!date) {
      overrideDate = translate('never');
    } else if (differenceInHours(Date.now(), date) < 1) {
      overrideDate = translate('less_than_1_hour_ago');
    }

    if (overrideDate) {
      return children ? children(overrideDate) : overrideDate;
    }

    return (
      <Tooltip overlay={<DateTimeFormatter date={date!} />}>
        <span>
          <DateFromNow date={date!}>{children}</DateFromNow>
        </span>
      </Tooltip>
    );
  }
}
