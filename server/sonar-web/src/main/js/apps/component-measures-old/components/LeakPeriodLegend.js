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
import React from 'react';
import moment from 'moment';
import Tooltip from '../../../components/controls/Tooltip';
import { getPeriodLabel, getPeriodDate } from '../../../helpers/periods';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default function LeakPeriodLegend({ component, period }) {
  if (component.qualifier === 'APP') {
    return (
      <div className="measures-domains-leak-header">
        {translate('issues.leak_period')}
      </div>
    );
  }

  const label = (
    <div className="measures-domains-leak-header">
      {translateWithParameters('overview.leak_period_x', getPeriodLabel(period))}
    </div>
  );

  if (period.mode === 'days') {
    return label;
  }

  const date = getPeriodDate(period);
  const fromNow = moment(date).fromNow();
  const tooltip = fromNow + ', ' + moment(date).format('LL');
  return (
    <Tooltip placement="bottom" overlay={tooltip}>
      {label}
    </Tooltip>
  );
}
