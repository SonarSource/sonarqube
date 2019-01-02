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
import * as classNames from 'classnames';
import { formatMeasure } from '../../helpers/measures';

interface Props {
  className?: string;
  current?: number;
  label: string;
  total: number;
}

export default function PageCounter({ className, current, label, total }: Props) {
  return (
    <div className={classNames('display-inline-block', className)}>
      <strong className="little-spacer-right">
        {current !== undefined && formatMeasure(current + 1, 'INT') + ' / '}
        <span className="js-page-counter-total">{formatMeasure(total, 'INT')}</span>
      </strong>
      {label}
    </div>
  );
}
