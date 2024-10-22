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
import { AutoSizerProps } from 'react-virtualized';
import { renderWithRouter } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { BubbleChart } from '../BubbleChart';

jest.mock('react-virtualized/dist/commonjs/AutoSizer', () => ({
  AutoSizer: ({ children }: AutoSizerProps) => children({ width: 100, height: NaN }),
}));

jest.mock('d3-zoom', () => ({
  zoom: jest.fn().mockReturnValue({ scaleExtent: jest.fn().mockReturnValue({ on: jest.fn() }) }),
}));

jest.mock('d3-selection', () => ({
  select: jest.fn().mockReturnValue({ call: jest.fn() }),
}));

it('should display bubbles with correct chart structure', () => {
  renderBubbleChart();
  expect(screen.getAllByRole('link')).toHaveLength(2);
  expect(screen.getByText('5')).toBeInTheDocument();
});

it('should allow click on bubbles', async () => {
  const onBubbleClick = jest.fn();
  const { user } = renderBubbleChart({ onBubbleClick });
  await user.click(screen.getAllByRole('link')[0]);
  expect(onBubbleClick).toHaveBeenCalledWith('foo');
});

it('should navigate between bubbles by tab', async () => {
  const { user } = renderBubbleChart();
  await user.tab();
  expect(screen.getAllByRole('link')[0]).toHaveFocus();
  await user.tab();
  expect(screen.getAllByRole('link')[1]).toHaveFocus();
});

it('should not display ticks and grid', () => {
  renderBubbleChart({
    displayXGrid: false,
    displayYGrid: false,
    displayXTicks: false,
    displayYTicks: false,
  });

  expect(screen.queryByText('5')).not.toBeInTheDocument();
});

it('renders empty graph', () => {
  renderBubbleChart({
    items: [],
  });

  expect(screen.queryByRole('link')).not.toBeInTheDocument();
});

function renderBubbleChart(props: Partial<FCProps<typeof BubbleChart>> = {}) {
  return renderWithRouter(
    <BubbleChart
      height={100}
      items={[
        { x: 1, y: 10, size: 7, data: 'foo' },
        { x: 2, y: 30, size: 5, data: 'bar' },
      ]}
      padding={[0, 0, 0, 0]}
      {...props}
    />,
  );
}
