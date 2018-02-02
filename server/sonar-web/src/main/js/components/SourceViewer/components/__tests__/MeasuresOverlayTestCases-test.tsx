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
import MeasuresOverlayTestCases from '../MeasuresOverlayTestCases';
import { waitAndUpdate, click } from '../../../../helpers/testUtils';
import { ShortLivingBranch, BranchType } from '../../../../app/types';

jest.mock('../../../../api/tests', () => ({
  getTests: () =>
    Promise.resolve({
      tests: [
        {
          id: 'AWGub2mGGZxsAttCZwQy',
          name: 'testAdd_WhichFails',
          fileKey: 'test:fake-project-for-tests:src/test/java/bar/SimplestTest.java',
          fileName: 'src/test/java/bar/SimplestTest.java',
          status: 'FAILURE',
          durationInMs: 6,
          coveredLines: 3,
          message: 'expected:<9> but was:<2>',
          stacktrace:
            'java.lang.AssertionError: expected:<9> but was:<2>\n\tat org.junit.Assert.fail(Assert.java:93)\n\tat org.junit.Assert.failNotEquals(Assert.java:647)'
        },
        {
          id: 'AWGub2mGGZxsAttCZwQz',
          name: 'testAdd_InError',
          fileKey: 'test:fake-project-for-tests:src/test/java/bar/SimplestTest.java',
          fileName: 'src/test/java/bar/SimplestTest.java',
          status: 'ERROR',
          durationInMs: 2,
          coveredLines: 3
        },
        {
          id: 'AWGub2mFGZxsAttCZwQx',
          name: 'testAdd',
          fileKey: 'test:fake-project-for-tests:src/test/java/bar/SimplestTest.java',
          fileName: 'src/test/java/bar/SimplestTest.java',
          status: 'OK',
          durationInMs: 8,
          coveredLines: 3
        }
      ]
    })
}));

const branchLike: ShortLivingBranch = {
  isMain: false,
  mergeBranch: 'master',
  name: 'feature',
  type: BranchType.SHORT
};

it('should render', async () => {
  const wrapper = shallow(
    <MeasuresOverlayTestCases branchLike={branchLike} componentKey="component-key" />
  );
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-sort-tests-by-duration'), {
    currentTarget: { blur() {}, dataset: { sort: 'duration' } }
  });
  expect(wrapper).toMatchSnapshot();
});
