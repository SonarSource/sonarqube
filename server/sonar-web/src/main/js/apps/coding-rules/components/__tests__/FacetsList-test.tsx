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
import { Query } from '../../query';
import FacetsList from '../FacetsList';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should correctly hide profile facets', () => {
  const wrapper = shallowRender({ hideProfileFacet: true });
  expect(wrapper.find('ProfileFacet').length).toEqual(0);
  expect(wrapper.find('InheritanceFacet').length).toEqual(0);
  expect(wrapper.find('ActivationSeverityFacet').length).toEqual(0);
});

it('should correctly hide the template facet', () => {
  const wrapper = shallowRender({ organizationsEnabled: true });
  expect(wrapper.find('TemplateFacet').length).toEqual(0);
});

it('should correctly enable/disable the language facet', () => {
  const wrapper = shallowRender({ query: { profile: 'foo' } });
  expect(wrapper.find('Connect(LanguageFacet)').prop('disabled')).toBe(true);

  wrapper.setProps({ query: {} }).update();
  expect(wrapper.find('Connect(LanguageFacet)').prop('disabled')).toBe(false);
});

it('should correctly enable/disable the activation severity facet', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('ActivationSeverityFacet').prop('disabled')).toBe(true);

  wrapper.setProps({ query: { activation: 'foo' }, selectedProfile: { key: 'foo' } }).update();
  expect(wrapper.find('ActivationSeverityFacet').prop('disabled')).toBe(false);
});

it('should correctly enable/disable the inheritcance facet', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('InheritanceFacet').prop('disabled')).toBe(true);

  wrapper.setProps({ selectedProfile: { isInherited: true } }).update();
  expect(wrapper.find('InheritanceFacet').prop('disabled')).toBe(false);
});

function shallowRender(props = {}) {
  return shallow(
    <FacetsList
      onFacetToggle={jest.fn()}
      onFilterChange={jest.fn()}
      openFacets={{}}
      organization="foo"
      query={{} as Query}
      referencedProfiles={{}}
      referencedRepositories={{}}
      {...props}
    />
  );
}
