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
import { mockLanguage } from '../../../../helpers/testMocks';
import { LanguageFacet } from '../LanguageFacet';

it('should handle search correctly', async () => {
  const wrapper = shallowRender({ stats: { java: 12 } });
  const result = await wrapper.instance().handleSearch('ja');

  expect(result).toStrictEqual({
    paging: {
      pageIndex: 1,
      pageSize: 2,
      total: 2,
    },
    results: [
      { key: 'js', name: 'javascript' },
      { key: 'java', name: 'java' },
    ],
  });
});

it('should render name correctly', () => {
  const wrapper = shallowRender();

  expect(wrapper.instance().getLanguageName('js')).toBe('javascript');
  expect(wrapper.instance().getLanguageName('unknownKey')).toBe('unknownKey');
});

it('should render search results correctly', () => {
  const wrapper = shallowRender();

  expect(wrapper.instance().renderSearchResult({ key: 'hello', name: 'Hello' }, 'llo'))
    .toMatchInlineSnapshot(`
    <React.Fragment>
      He
      <mark>
        llo
      </mark>
    </React.Fragment>
  `);
});

function shallowRender(props: Partial<LanguageFacet['props']> = {}) {
  return shallow<LanguageFacet>(
    <LanguageFacet
      languages={{
        js: mockLanguage({ key: 'js', name: 'javascript' }),
        c: mockLanguage({ key: 'c', name: 'c' }),
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
