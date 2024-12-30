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

import { TooltipProvider } from '@sonarsource/echoes-react';
import { render } from '@testing-library/react';
import { MetricType } from '~sonar-aligned/types/metrics';
import { AdvancedTimeline, PropsWithoutTheme } from '../AdvancedTimeline';

// Replace scaleTime with scaleUtc to avoid timezone-dependent snapshots
jest.mock('d3-scale', () => {
  const d3scale = jest.requireActual('d3-scale');
  return {
    ...d3scale,
    scaleTime: d3scale.scaleUtc,
  };
});

jest.mock('lodash', () => {
  const lodash = jest.requireActual('lodash');

  return { ...lodash, throttle: (f: unknown) => f };
});

it('should render correctly', () => {
  const checkSnapShot = (props: Partial<PropsWithoutTheme> = {}, snapshotName = 'default') => {
    const renderedComponent = renderComponent(props);

    // eslint-disable-next-line testing-library/no-container, testing-library/no-node-access
    const svg = renderedComponent.container.querySelector("[class='line-chart']");

    expect(svg).toMatchSnapshot(snapshotName);
  };

  checkSnapShot();
  checkSnapShot({ disableZoom: false, updateZoom: () => {} }, 'Zoom enabled');
  checkSnapShot({ formatYTick: (t) => `Nicer tick ${t}` }, 'format y tick');
  checkSnapShot({ width: undefined }, 'no width');
  checkSnapShot({ height: undefined }, 'no height');
  checkSnapShot({ showAreas: undefined }, 'no areas');
  checkSnapShot({ selectedDate: new Date('2019-10-01') }, 'selected date');
  checkSnapShot({ metricType: MetricType.Rating }, 'rating metric');
  checkSnapShot({ metricType: MetricType.Level }, 'level metric');
  checkSnapShot({ zoomSpeed: 2 }, 'zoomSpeed');
  checkSnapShot({ leakPeriodDate: new Date('2019-10-02T00:00:00.000Z') }, 'leakPeriodDate');
  checkSnapShot({ basisCurve: true }, 'basisCurve');
  checkSnapShot(
    { splitPointDate: new Date('2019-10-02T00:00:00.000Z') },
    'split point, but not Rating',
  );
});

function renderComponent(props?: Partial<PropsWithoutTheme>) {
  return render(
    <TooltipProvider>
      <AdvancedTimeline
        height={100}
        maxYTicksCount={10}
        metricType="TEST_METRIC"
        series={[
          {
            name: 'test-1',
            type: 'test-type-1',
            translatedName: '',
            data: [
              {
                x: new Date('2019-10-01T00:00:00.000Z'),
                y: 1,
              },
              {
                x: new Date('2019-10-02T00:00:00.000Z'),
                y: 2,
              },
            ],
          },
          {
            name: 'test-2',
            type: 'test-type-2',
            translatedName: '',
            data: [
              {
                x: new Date('2019-10-03T00:00:00.000Z'),
                y: 3,
              },
            ],
          },
        ]}
        width={100}
        zoomSpeed={1}
        {...props}
      />
    </TooltipProvider>,
  );
}
