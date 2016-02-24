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
import moment from 'moment';
import React from 'react';

export default function TaskDate ({ date, baseDate, format }) {
  const m = moment(date);
  const baseM = moment(baseDate);
  const diff = (date && baseDate) ? m.diff(baseM, 'minutes') : 0;

  return (
      <td className="thin nowrap text-right">
        {diff > 0 && (
            <span className="text-warning little-spacer-right">{`(+${diff})`}</span>
        )}

        {date ? moment(date).format(format) : ''}
      </td>
  );
}
