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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { BarChart } from '../BarChart';

it('renders chart correctly', async () => {
  const user = userEvent.setup();
  const onBarClick = jest.fn();
  renderChart({ onBarClick });

  const p1 = screen.getByLabelText('point 1');
  expect(p1).toBeInTheDocument();
  await user.click(p1);

  expect(onBarClick).toHaveBeenCalledWith({ description: 'point 1', x: 1, y: 20 });
});

it('displays values', () => {
  const xValues = ['hi', '43', 'testing'];
  renderChart({ xValues });

  expect(screen.getByText(xValues[0])).toBeInTheDocument();
  expect(screen.getByText(xValues[1])).toBeInTheDocument();
  expect(screen.getByText(xValues[2])).toBeInTheDocument();
});

function renderChart(overrides: Partial<FCProps<typeof BarChart>> = {}) {
  return render(
    <BarChart
      barsWidth={20}
      data={[
        { x: 1, y: 20, description: 'point 1' },
        { x: 2, y: 40, description: 'apex' },
        { x: 3, y: 31, description: 'point 3' },
      ]}
      height={75}
      onBarClick={jest.fn()}
      width={200}
      {...overrides}
    />,
  );
}
