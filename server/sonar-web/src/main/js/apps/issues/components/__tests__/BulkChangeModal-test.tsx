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
import { searchIssueTags } from '../../../../api/issues';
import { SubmitButton } from '../../../../components/controls/buttons';
import SelectLegacy from '../../../../components/controls/SelectLegacy';
import { mockIssue } from '../../../../helpers/testMocks';
import { change, waitAndUpdate } from '../../../../helpers/testUtils';
import { Issue } from '../../../../types/types';
import BulkChangeModal, { MAX_PAGE_SIZE } from '../BulkChangeModal';

jest.mock('../../../../api/issues', () => ({
  searchIssueTags: jest.fn().mockResolvedValue([undefined, []])
}));

jest.mock('../BulkChangeModal', () => {
  const mock = jest.requireActual('../BulkChangeModal');
  mock.MAX_PAGE_SIZE = 1;
  return mock;
});

jest.mock('../../utils', () => ({
  searchAssignees: jest.fn().mockResolvedValue({
    results: [
      {
        active: true,
        avatar: '##toto',
        login: 'toto@toto',
        name: 'toto'
      },
      {
        active: false,
        avatar: '##toto',
        login: 'login@login',
        name: 'toto'
      },
      {
        active: true,
        avatar: '##toto',
        login: 'login@login'
      }
    ]
  })
}));

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
  const issues: Issue[] = [];
  for (let i = MAX_PAGE_SIZE + 1; i > 0; i--) {
    issues.push(mockIssue());
  }

  const wrapper = getWrapper(issues);
  await waitAndUpdate(wrapper);
  expect(wrapper.find('h2')).toMatchSnapshot();
  expect(wrapper.find('Alert')).toMatchSnapshot();
});

it('should properly handle the search for assignee', async () => {
  const issues: Issue[] = [];
  for (let i = MAX_PAGE_SIZE + 1; i > 0; i--) {
    issues.push(mockIssue());
  }

  const wrapper = getWrapper(issues);
  const result = await wrapper.instance().handleAssigneeSearch('toto');
  expect(result).toMatchSnapshot();
});

it('should properly handle the search for tags', async () => {
  const wrapper = getWrapper([]);
  await wrapper.instance().handleTagsSearch('query');
  expect(searchIssueTags).toBeCalled();
});

it('should disable the submit button unless some change is configured', async () => {
  const wrapper = getWrapper([mockIssue(false, { actions: ['set_severity', 'comment'] })]);
  await waitAndUpdate(wrapper);

  return new Promise<void>((resolve, reject) => {
    expect(wrapper.find(SubmitButton).props().disabled).toBe(true);

    // Setting a comment is not sufficient; some other change must occur.
    change(wrapper.find('#comment'), 'Some comment');
    expect(wrapper.find(SubmitButton).props().disabled).toBe(true);

    const { onChange } = wrapper
      .find(SelectLegacy)
      .at(0)
      .props();
    if (!onChange) {
      reject();
      return;
    }

    onChange({ value: 'foo' });
    expect(wrapper.find(SubmitButton).props().disabled).toBe(false);
    resolve();
  });
});

const getWrapper = (issues: Issue[]) => {
  return shallow<BulkChangeModal>(
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
    />
  );
};
