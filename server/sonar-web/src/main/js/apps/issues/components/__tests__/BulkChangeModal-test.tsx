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
import * as React from 'react';
import { shallow } from 'enzyme';
import BulkChangeModal from '../BulkChangeModal';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/issues', () => ({
  searchIssueTags: () => Promise.resolve([undefined, []])
}));

it('should display error message when no issues available', async () => {
  const wrapper = getWrapper([]);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should display form when issues are present', async () => {
  const wrapper = getWrapper([
    {
      actions: [],
      component: 'foo',
      componentLongName: 'foo',
      componentQualifier: 'foo',
      componentUuid: 'foo',
      creationDate: 'foo',
      key: 'foo',
      flows: [],
      fromHotspot: false,
      message: 'foo',
      organization: 'foo',
      project: 'foo',
      projectName: 'foo',
      projectOrganization: 'foo',
      projectKey: 'foo',
      rule: 'foo',
      ruleName: 'foo',
      secondaryLocations: [],
      severity: 'foo',
      status: 'foo',
      transitions: [],
      type: 'BUG'
    }
  ]);
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
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
            pageIndex: 0,
            pageSize: 0,
            total: 0
          }
        })
      }
      onClose={() => {}}
      onDone={() => {}}
      organization={undefined}
    />
  );
};
