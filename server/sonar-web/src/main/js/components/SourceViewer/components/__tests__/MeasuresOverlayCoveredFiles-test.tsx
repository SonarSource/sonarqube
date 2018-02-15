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
import * as React from 'react';
import { shallow } from 'enzyme';
import MeasuresOverlayCoveredFiles from '../MeasuresOverlayCoveredFiles';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/tests', () => ({
  getCoveredFiles: () =>
    Promise.resolve([{ key: 'project:src/file.js', longName: 'src/file.js', coveredLines: 3 }])
}));

const testCase = {
  coveredLines: 3,
  durationInMs: 1,
  fileId: 'abcd',
  fileKey: 'project:test.js',
  fileName: 'test.js',
  id: 'test-abcd',
  name: 'should work',
  status: 'OK'
};

it('should render OK test', async () => {
  const wrapper = shallow(<MeasuresOverlayCoveredFiles testCase={testCase} />);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render ERROR test', async () => {
  const wrapper = shallow(
    <MeasuresOverlayCoveredFiles
      testCase={{ ...testCase, status: 'ERROR', message: 'Something failed' }}
    />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});
