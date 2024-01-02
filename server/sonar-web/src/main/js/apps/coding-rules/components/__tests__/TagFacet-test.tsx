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
import { getRuleTags } from '../../../../api/rules';
import TagFacet from '../TagFacet';

jest.mock('../../../../api/rules', () => ({
  getRuleTags: jest.fn().mockResolvedValue([]),
}));

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should handle search', async () => {
  const wrapper = shallowRender();

  const query = 'query';

  await wrapper.instance().handleSearch(query);

  expect(getRuleTags).toHaveBeenCalledWith({ ps: 50, q: query });
});

describe('ListStyleFacet Renderers', () => {
  const instance = shallowRender().instance();

  it('should include renderFacetItem', () => {
    expect(instance.renderTag('tag')).toMatchSnapshot();
  });

  it('should include renderSearchResult', () => {
    expect(instance.renderSearchResult('my-tag', 'tag')).toMatchSnapshot();
  });
});

function shallowRender(props: Partial<TagFacet['props']> = {}) {
  return shallow<TagFacet>(
    <TagFacet
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      stats={{}}
      values={[]}
      {...props}
    />
  );
}
