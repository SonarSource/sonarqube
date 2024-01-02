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
import { mockLanguage, mockRuleRepository } from '../../../../helpers/testMocks';
import { RepositoryFacet } from '../RepositoryFacet';

jest.mock('../../../../api/rules', () => ({
  getRuleRepositories: jest
    .fn()
    .mockResolvedValue([{ key: 'clirr', name: 'Clirr', language: 'java' }]),
}));

it('should handle search correctly', async () => {
  const wrapper = shallowRender();
  const result = await wrapper.instance().handleSearch('foo');

  expect(result).toStrictEqual({
    paging: {
      pageIndex: 1,
      pageSize: 1,
      total: 1,
    },
    results: ['clirr'],
  });
});

it('should return text repository correctly', () => {
  const wrapper = shallowRender();
  let text = wrapper.instance().renderTextName('l');
  expect(text).toBe('SonarQube');

  text = wrapper.instance().renderTextName('notFound');
  expect(text).toBe('notFound');

  text = wrapper.instance().renderTextName('noName');
  expect(text).toBe('noName');
});

it('should render repository correctly', () => {
  const wrapper = shallowRender();
  let text = wrapper.instance().renderName('l');
  expect(shallow(<div>{text}</div>)).toMatchSnapshot();

  text = wrapper.instance().renderName('notFound');
  expect(text).toBe('notFound');
});

it('should render search repository correctly', () => {
  const wrapper = shallowRender();
  let text = wrapper.instance().renderSearchTextName('l', 'Son');
  expect(shallow(<div>{text}</div>)).toMatchSnapshot();

  text = wrapper.instance().renderSearchTextName('notFound', '');
  expect(text).toBe('notFound');
});

function shallowRender(props: Partial<RepositoryFacet['props']> = {}) {
  return shallow<RepositoryFacet>(
    <RepositoryFacet
      languages={{ l: mockLanguage() }}
      referencedRepositories={{
        l: mockRuleRepository(),
        noName: mockRuleRepository({ name: undefined }),
      }}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      stats={{}}
      values={[]}
      {...props}
    />
  );
}
