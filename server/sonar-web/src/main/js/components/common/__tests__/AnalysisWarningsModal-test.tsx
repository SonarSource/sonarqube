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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { getTask } from '../../../api/ce';
import AnalysisWarningsModal from '../AnalysisWarningsModal';

jest.mock('../../../api/ce', () => ({
  getTask: jest.fn().mockResolvedValue({
    warnings: ['message foo', 'message-bar', 'multiline message\nsecondline\n  third line']
  })
}));

beforeEach(() => {
  (getTask as jest.Mock<any>).mockClear();
});

it('should fetch warnings and render', async () => {
  const wrapper = shallow(<AnalysisWarningsModal onClose={jest.fn()} taskId="abcd1234" />);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getTask).toBeCalledWith('abcd1234', ['warnings']);
});

it('should render warnings without fetch', () => {
  const wrapper = shallow(
    <AnalysisWarningsModal onClose={jest.fn()} warnings={['warning 1', 'warning 2']} />
  );
  expect(wrapper).toMatchSnapshot();
  expect(getTask).not.toBeCalled();
});
