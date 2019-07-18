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
import { mockIssue } from '../../../../helpers/testMocks';
import BulkChangeModal, { MAX_PAGE_SIZE } from '../BulkChangeModal';

jest.mock('../../../../api/issues', () => ({
  searchIssueTags: () => Promise.resolve([undefined, []])
}));

jest.mock('../BulkChangeModal', () => {
  const mock = require.requireActual('../BulkChangeModal');
  mock.MAX_PAGE_SIZE = 1;
  return mock;
});

it('should display error message when no issues available', async () => {
  const wrapper = getWrapper([]);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should display form when issues are present', async () => {
  const wrapper = getWrapper([mockIssue()]);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should display warning when too many issues are passed', async () => {
  const issues: T.Issue[] = [];
  for (let i = MAX_PAGE_SIZE + 1; i > 0; i--) {
    issues.push(mockIssue());
  }

  const wrapper = getWrapper(issues);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h2')).toMatchSnapshot();
  expect(wrapper.find('Alert')).toMatchSnapshot();
});

const getWrapper = (issues: T.Issue[]) => {
  return shallow(
    <BulkChangeModal
      component={undefined}
      currentUser={{ isLoggedIn: true }}
      fetchIssues={() =>
        Promise.resolve({
          issues,
          paging: {
            pageIndex: issues.length,
            pageSize: issues.length,
            total: issues.length
          }
        })
      }
      onClose={() => {}}
      onDone={() => {}}
      organization={undefined}
    />
  );
};
