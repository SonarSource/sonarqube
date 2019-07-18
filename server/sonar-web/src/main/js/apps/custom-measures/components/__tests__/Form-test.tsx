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
import { change, click, submit, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import Form from '../Form';

jest.mock('../../../../api/metrics', () => ({
  getAllMetrics: () =>
    Promise.resolve([
      { id: '1', key: 'custom-metric', name: 'Custom Metric', type: 'STRING' },
      { id: '2', key: 'skipped-metric', name: 'Skipped Metric', type: 'FLOAT' }
    ])
}));

it('should render form', async () => {
  const onClose = jest.fn();
  const onSubmit = jest.fn(() => Promise.resolve());
  const wrapper = shallow(
    <Form
      confirmButtonText="confirmButtonText"
      header="header"
      onClose={onClose}
      onSubmit={onSubmit}
      skipMetrics={['skipped-metric']}
    />
  );
  expect(wrapper.dive()).toMatchSnapshot();

  await waitAndUpdate(wrapper);
  const form = wrapper.dive();
  expect(form).toMatchSnapshot();

  form.find('Select').prop<Function>('onChange')({ value: 'custom-metric' });
  change(form.find('[name="value"]'), 'Foo');
  change(form.find('[name="description"]'), 'bar');
  submit(form.find('form'));
  expect(onSubmit).toBeCalledWith({
    description: 'bar',
    metricKey: 'custom-metric',
    value: 'Foo'
  });

  await new Promise(setImmediate);
  expect(onClose).toBeCalled();

  onClose.mockClear();
  click(form.find('ResetButtonLink'));
  expect(onClose).toBeCalled();
});
