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
import { searchRules } from '../../../../api/rules';
import { mockReferencedRule } from '../../../../helpers/mocks/issues';
import { mockRule } from '../../../../helpers/testMocks';
import { Query } from '../../utils';
import RuleFacet from '../RuleFacet';

jest.mock('../../../../api/rules', () => ({
  searchRules: jest.fn().mockResolvedValue({}),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle search', async () => {
  const wrapper = shallowRender();

  const query = 'query';

  await wrapper.instance().handleSearch(query);

  expect(searchRules).toHaveBeenCalledWith(
    expect.objectContaining({ languages: 'js,java', q: query })
  );
});

describe('ListStyleFacet Renderers', () => {
  const referencedRules = { r1: mockReferencedRule() };
  const instance = shallowRender({ referencedRules }).instance();

  it('should include renderFacetItem', () => {
    const rule = referencedRules.r1;
    expect(instance.getRuleName('r1')).toBe(`(${rule.langName}) ${rule.name}`);
    expect(instance.getRuleName('nonexistent')).toBe('nonexistent');
  });

  it('should include renderSearchResult', () => {
    const rule = mockRule();
    expect(instance.renderSearchResult(rule)).toBe(`(${rule.langName}) ${rule.name}`);
    expect(instance.renderSearchResult(mockRule({ langName: '' }))).toBe(rule.name);
  });
});

function shallowRender(props: Partial<RuleFacet['props']> = {}) {
  return shallow<RuleFacet>(
    <RuleFacet
      fetching={true}
      languages={['js', 'java']}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      query={{} as Query}
      referencedRules={{}}
      rules={['r1']}
      stats={{}}
      {...props}
    />
  );
}
