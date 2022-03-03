/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import { changeEvent, createEvent } from '../../../../api/projectActivity';
import { mockComponent } from '../../../../helpers/mocks/component';
import {
  mockAnalysisEvent,
  mockLocation,
  mockMetric,
  mockRouter
} from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import ProjectActivityAppContainer from '../ProjectActivityAppContainer';

jest.mock('../../../../helpers/dates', () => ({
  parseDate: jest.fn(date => `PARSED:${date}`)
}));

jest.mock('../../../../api/time-machine', () => {
  const { mockPaging } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getAllTimeMachineData: jest.fn().mockResolvedValue({
      measures: [
        {
          metric: 'bugs',
          history: [{ date: '2022-01-01', value: '10' }]
        }
      ],
      paging: mockPaging({ total: 1 })
    })
  };
});

jest.mock('../../../../api/metrics', () => {
  const { mockMetric } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getAllMetrics: jest.fn().mockResolvedValue([mockMetric()])
  };
});

jest.mock('../../../../api/projectActivity', () => {
  const { mockAnalysis, mockPaging } = jest.requireActual('../../../../helpers/testMocks');
  return {
    ...jest.requireActual('../../../../api/projectActivity'),
    createEvent: jest.fn(),
    changeEvent: jest.fn(),
    getProjectActivity: jest.fn().mockResolvedValue({
      analyses: [mockAnalysis({ key: 'foo' })],
      paging: mockPaging({ total: 1 })
    })
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should filter metric correctly', () => {
  const wrapper = shallowRender();
  let metrics = wrapper
    .instance()
    .filterMetrics(mockComponent({ qualifier: ComponentQualifier.Project }), [
      mockMetric({ key: MetricKey.bugs }),
      mockMetric({ key: MetricKey.security_review_rating })
    ]);
  expect(metrics).toHaveLength(1);
  metrics = wrapper
    .instance()
    .filterMetrics(mockComponent({ qualifier: ComponentQualifier.Portfolio }), [
      mockMetric({ key: MetricKey.bugs }),
      mockMetric({ key: MetricKey.security_hotspots_reviewed })
    ]);
  expect(metrics).toHaveLength(1);
});

it('should correctly create and update custom events', async () => {
  const analysisKey = 'foo';
  const name = 'bar';
  const newName = 'baz';
  const event = mockAnalysisEvent({ name });
  (createEvent as jest.Mock).mockResolvedValueOnce({ analysis: analysisKey, ...event });
  (changeEvent as jest.Mock).mockResolvedValueOnce({
    analysis: analysisKey,
    ...event,
    name: newName
  });

  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  const instance = wrapper.instance();

  instance.addCustomEvent(analysisKey, name);
  expect(createEvent).toHaveBeenCalledWith(analysisKey, name, undefined);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().analyses[0].events[0]).toEqual(event);

  instance.changeEvent(event.key, newName);
  expect(changeEvent).toHaveBeenCalledWith(event.key, newName);
  await waitAndUpdate(wrapper);
  expect(wrapper.state().analyses[0].events[0]).toEqual({ ...event, name: newName });
});

function shallowRender(props: Partial<ProjectActivityAppContainer['props']> = {}) {
  return shallow<ProjectActivityAppContainer>(
    <ProjectActivityAppContainer
      component={mockComponent({ breadcrumbs: [mockComponent()] })}
      location={mockLocation()}
      router={mockRouter()}
      {...props}
    />
  );
}
