/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import { formatMeasure } from '../../helpers/measures';

/*:: type Props = {
  className?: string,
  colorScale: Object,
  colorNA?: string,
  metricType: string
}; */

export default function ColorBoxLegend(
  { className, colorScale, colorNA, metricType } /*: Props */
) {
  const colorDomain = colorScale.domain();
  const colorRange = colorScale.range();
  return (
    <div className={classNames('color-box-legend', className)}>
      {colorDomain.map((value, idx) => (
        <div key={value}>
          <span className="color-box-legend-rect" style={{ borderColor: colorRange[idx] }}>
            <span
              className="color-box-legend-rect-inner"
              style={{ backgroundColor: colorRange[idx] }}
            />
          </span>
          {formatMeasure(value, metricType)}
        </div>
      ))}
      {colorNA && (
        <div>
          <span className="color-box-legend-rect" style={{ borderColor: colorNA }}>
            <span className="color-box-legend-rect-inner" style={{ backgroundColor: colorNA }} />
          </span>
          N/A
        </div>
      )}
    </div>
  );
}
