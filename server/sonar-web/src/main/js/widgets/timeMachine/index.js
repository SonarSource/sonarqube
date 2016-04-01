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
import { render } from 'react-dom';
import d3 from 'd3';

import { LineChart } from '../../components/charts/line-chart';

const DEFAULTS = {
  width: 80,
  height: 15,
  dateFormat: 'YYYYMMDDHHmmss'
};

export default function (selector) {
  // use [].slice instead of Array.from, because this code might be executed with polyfill
  const elements = [].slice.call(document.querySelectorAll(selector));

  elements.forEach(element => {
    const { dataset } = element;
    const width = Number(dataset.width || DEFAULTS.width);
    const height = Number(dataset.height || DEFAULTS.height);

    const { x, y } = dataset;
    const xData = x.split(',').map(v => moment(v, DEFAULTS.dateFormat).toDate());
    const yData = y.split(',').map(v => Number(v));

    const data = xData.map((x, index) => {
      const y = yData[index];
      return { x, y };
    });

    const domain = d3.extent(yData);

    render((
        <LineChart
            data={data}
            domain={domain}
            width={width}
            height={height}
            padding={[1, 1, 1, 1]}
            interpolate="linear"
            displayBackdrop={false}
            displayPoints={false}
            displayVerticalGrid={false}/>
    ), element);
  });
}
