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
import { keyBy } from 'lodash';
import * as React from 'react';
import { getTree, searchProjects } from '../../../../api/components';
import { mockComponent } from '../../../../helpers/mocks/component';
import { ComponentQualifier } from '../../../../types/component';
import { ReferencedComponent } from '../../../../types/issues';
import { Query } from '../../utils';
import ProjectFacet from '../ProjectFacet';

jest.mock('../../../../api/components', () => ({
  getTree: jest.fn().mockResolvedValue({ baseComponent: {}, components: [], paging: {} }),
  searchProjects: jest.fn().mockResolvedValue({
    components: [],
    facets: [],
    paging: {},
  }),
}));

beforeEach(() => jest.clearAllMocks());

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should callback to load search results', () => {
  const loadSearchResultCount = jest.fn();
  const wrapper = shallowRender({ loadSearchResultCount });
  wrapper.instance().loadSearchResultCount([
    { key: '1', name: 'first' },
    { key: '2', name: 'seecond' },
  ]);

  expect(loadSearchResultCount).toHaveBeenCalledWith('projects', { projects: ['1', '2'] });
});

it('should handle search for projects globally', async () => {
  const wrapper = shallowRender();
  const query = 'my project';

  await wrapper.instance().handleSearch(query);

  expect(searchProjects).toHaveBeenCalled();
  expect(getTree).not.toHaveBeenCalled();
});

it('should handle search for projects in portfolio', async () => {
  const wrapper = shallowRender({
    component: mockComponent({ qualifier: ComponentQualifier.Portfolio }),
  });
  const query = 'my project';

  await wrapper.instance().handleSearch(query);

  expect(searchProjects).not.toHaveBeenCalled();
  expect(getTree).toHaveBeenCalled();
});

describe("ListStyleFacet's renderers", () => {
  const components: ReferencedComponent[] = [
    { key: 'projectKey', name: 'First Project Name', uuid: '141324' },
    { key: 'projectKey2', name: 'Second Project Name', uuid: '643878' },
  ];
  const referencedComponents = keyBy(components, (c) => c.key);
  const wrapper = shallowRender({ referencedComponents });
  const instance = wrapper.instance();

  it('should include getProjectName', () => {
    expect(instance.getProjectName(components[0].key)).toBe(components[0].name);
    expect(instance.getProjectName('nonexistent')).toBe('nonexistent');
  });

  it('should include renderFacetItem', () => {
    expect(instance.renderFacetItem(components[0].key)).toMatchSnapshot();
  });

  it('should include renderSearchResult', () => {
    expect(instance.renderSearchResult(components[0], 'First')).toMatchSnapshot();
  });
});

function shallowRender(props: Partial<ProjectFacet['props']> = {}) {
  return shallow<ProjectFacet>(
    <ProjectFacet
      component={undefined}
      fetching={false}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      projects={[]}
      query={{} as Query}
      referencedComponents={{}}
      stats={undefined}
      {...props}
    />
  );
}
