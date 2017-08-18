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
import * as React from 'react';
import TimeFormatter from './TimeFormatter';
import Tooltip from '../controls/Tooltip';

interface Props {
  className?: string;
  date: Date | string | number;
  placement?: string;
}

export default function TimeTooltipFormatter({ className, date, placement }: Props) {
  return (
    <TimeFormatter date={date} long={false}>
      {formattedTime =>
        <Tooltip
          overlay={<TimeFormatter date={date} long={true} />}
          placement={placement}
          mouseEnterDelay={0.5}>
          <time className={className} dateTime={new Date(date as Date).toISOString()}>
            {formattedTime}
          </time>
        </Tooltip>}
    </TimeFormatter>
  );
}
