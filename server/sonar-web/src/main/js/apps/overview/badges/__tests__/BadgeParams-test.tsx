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
import { shallow } from 'enzyme';
import * as React from 'react';
import BadgeParams from '../BadgeParams';
import { BadgeType } from '../utils';

jest.mock('../../../../api/web-api', () => ({
  fetchWebApi: () =>
    Promise.resolve([
      {
        path: 'api/project_badges',
        actions: [
          {
            key: 'measure',
            params: [{ key: 'metric', possibleValues: ['alert_status', 'coverage'] }]
          }
        ]
      }
    ])
}));

const METRICS = {
  alert_status: { key: 'alert_status', name: 'Quality Gate' } as T.Metric,
  coverage: { key: 'coverage', name: 'Coverage' } as T.Metric
};

it('should display marketing badge params', () => {
  const updateOptions = jest.fn();
  const wrapper = getWrapper({ updateOptions });
  expect(wrapper).toMatchSnapshot();
  (wrapper.instance() as BadgeParams).handleColorChange({ value: 'black' });
  expect(updateOptions).toHaveBeenCalledWith({ color: 'black' });
});

it('should display measure badge params', () => {
  const updateOptions = jest.fn();
  const wrapper = getWrapper({ updateOptions, type: BadgeType.measure });
  expect(wrapper).toMatchSnapshot();
  (wrapper.instance() as BadgeParams).handleMetricChange({ value: 'code_smell' });
  expect(updateOptions).toHaveBeenCalledWith({ metric: 'code_smell' });
});

it('should display quality gate badge params', () => {
  const updateOptions = jest.fn();
  const wrapper = getWrapper({ updateOptions, type: BadgeType.qualityGate });
  expect(wrapper).toMatchSnapshot();
  (wrapper.instance() as BadgeParams).handleFormatChange({ value: 'md' });
  expect(updateOptions).toHaveBeenCalledWith({ format: 'md' });
});

function getWrapper(props = {}) {
  return shallow(
    <BadgeParams
      metrics={METRICS}
      options={{ color: 'white', metric: 'alert_status' }}
      type={BadgeType.marketing}
      updateOptions={jest.fn()}
      {...props}
    />
  );
}
