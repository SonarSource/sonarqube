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

import { render } from '../../helpers/testUtils';
import { Histogram } from '../Histogram';

it('renders correctly', () => {
  const { container } = renderHistogram();
  expect(container).toMatchSnapshot();
});

it('renders correctly with yValues', () => {
  const { container } = renderHistogram({ yValues: ['100.0', '75.0', '150.0'] });
  expect(container).toMatchSnapshot();
});

it('renders correctly with yValues and yTicks', () => {
  const { container } = renderHistogram({
    yValues: ['100.0', '75.0', '150.0'],
    yTicks: ['a', 'b', 'c'],
  });
  expect(container).toMatchSnapshot();
});

it('renders correctly with yValues, yTicks, and yTooltips', () => {
  const { container } = renderHistogram({
    yValues: ['100.0', '75.0', '150.0'],
    yTicks: ['a', 'b', 'c'],
    yTooltips: ['a - 100', 'b - 75', 'c - 150'],
  });
  expect(container).toMatchSnapshot();
});

function renderHistogram(props: Partial<Histogram['props']> = {}) {
  return render(<Histogram bars={[100, 75, 150]} height={75} width={100} {...props} />);
}
