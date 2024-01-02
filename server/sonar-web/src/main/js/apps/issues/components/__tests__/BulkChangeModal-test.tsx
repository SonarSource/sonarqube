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
import { shallow } from 'enzyme';
import * as React from 'react';
import { Props as ReactSelectProps } from 'react-select';
import { SelectComponentsProps } from 'react-select/src/Select';
import { searchIssueTags } from '../../../../api/issues';
import { SubmitButton } from '../../../../components/controls/buttons';
import Select, { CreatableSelect, SearchSelect } from '../../../../components/controls/Select';
import { mockIssue } from '../../../../helpers/testMocks';
import { change, waitAndUpdate } from '../../../../helpers/testUtils';
import { Issue } from '../../../../types/types';
import BulkChangeModal, { MAX_PAGE_SIZE } from '../BulkChangeModal';

jest.mock('../../../../api/issues', () => ({
  searchIssueTags: jest.fn().mockResolvedValue([undefined, []]),
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

it('should properly handle the search for tags', async () => {
  const wrapper = getWrapper([]);
  await new Promise((resolve) => {
    wrapper.instance().handleTagsSearch('query', resolve);
  });
  expect(searchIssueTags).toHaveBeenCalled();
});

it.each([
  ['type', 'set_type'],
  ['severity', 'set_severity'],
])('should render select for %s', async (_field, action) => {
  const wrapper = getWrapper([mockIssue(false, { actions: [action] })]);
  await waitAndUpdate(wrapper);

  const { Option, SingleValue } = wrapper.find<SelectComponentsProps>(Select).props().components;

  expect(Option({ data: { label: 'label', value: 'value' } })).toMatchSnapshot('Option');
  expect(SingleValue({ data: { label: 'label', value: 'value' } })).toMatchSnapshot('SingleValue');
});

it('should render tags correctly', async () => {
  const wrapper = getWrapper([mockIssue(false, { actions: ['set_tags'] })]);
  await waitAndUpdate(wrapper);

  expect(wrapper.find(CreatableSelect).exists()).toBe(true);
  expect(wrapper.find(SearchSelect).exists()).toBe(true);
});

it('should disable the submit button unless some change is configured', async () => {
  const wrapper = getWrapper([mockIssue(false, { actions: ['set_severity', 'comment'] })]);
  await waitAndUpdate(wrapper);

  return new Promise<void>((resolve) => {
    expect(wrapper.find(SubmitButton).props().disabled).toBe(true);

    // Setting a comment is not sufficient; some other change must occur.
    change(wrapper.find('#comment'), 'Some comment');
    expect(wrapper.find(SubmitButton).props().disabled).toBe(true);

    wrapper.find<ReactSelectProps>(Select).at(0).simulate('change', { value: 'foo' });

    expect(wrapper.find(SubmitButton).props().disabled).toBe(false);
    resolve();
  });
});

const getWrapper = (issues: Issue[]) => {
  return shallow<BulkChangeModal>(
    <BulkChangeModal
      component={undefined}
      currentUser={{ isLoggedIn: true, dismissedNotices: {} }}
      fetchIssues={() =>
        Promise.resolve({
          issues,
          paging: {
            pageIndex: issues.length,
            pageSize: issues.length,
            total: issues.length,
          },
        })
      }
      onClose={() => {}}
      onDone={() => {}}
    />
  );
};
