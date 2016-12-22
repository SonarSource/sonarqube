/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { getPeriodDate, getPeriodLabel } from '../../../helpers/periods';
import { translateWithParameters } from '../../../helpers/l10n';

const LeakPeriodLegend = ({ period }) => {
  const leakPeriodLabel = getPeriodLabel(period);
  const leakPeriodDate = getPeriodDate(period);

  const momentDate = moment(leakPeriodDate);
  const fromNow = momentDate.fromNow();
  const tooltip = translateWithParameters(
      'overview.started_on_x',
      momentDate.format('LL'));

  return (
      <div className="overview-legend" title={tooltip} data-toggle="tooltip">
        {translateWithParameters('overview.leak_period_x', leakPeriodLabel)}
        <br/>
        <span className="note">
          {translateWithParameters('overview.started_x', fromNow)}
        </span>
      </div>
  );
};

export default LeakPeriodLegend;
