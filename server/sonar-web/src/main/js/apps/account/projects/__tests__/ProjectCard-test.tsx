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
import ProjectCard from '../ProjectCard';

it('should render key and name', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('.account-project-key').text()).toBe('key');
  expect(
    wrapper
      .find('.account-project-name')
      .find('Link')
      .prop('children')
  ).toBe('name');
});

it('should render description', () => {
  const wrapper = shallowRender({ description: 'bla' });
  expect(wrapper.find('.account-project-description').text()).toBe('bla');
});

it('should not render optional fields', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('.account-project-description').length).toBe(0);
  expect(wrapper.find('.account-project-quality-gate').length).toBe(0);
  expect(wrapper.find('.account-project-links').length).toBe(0);
});

it('should render analysis date', () => {
  const wrapper = shallowRender({ lastAnalysisDate: '2016-05-17' });
  expect(wrapper.find('.account-project-analysis DateFromNow')).toHaveLength(1);
});

it('should not render analysis date', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('.account-project-analysis').text()).toContain(
    'my_account.projects.never_analyzed'
  );
});

it('should render quality gate status', () => {
  const wrapper = shallowRender({ qualityGate: 'ERROR' });
  expect(
    wrapper
      .find('.account-project-quality-gate')
      .find('Level')
      .prop('level')
  ).toBe('ERROR');
});

it('should render links', () => {
  const wrapper = shallowRender({
    links: [{ name: 'name', type: 'type', href: 'href' }]
  });
  expect(wrapper.find('MetaLink').length).toBe(1);
});

function shallowRender(project: Partial<T.MyProject> = {}) {
  return shallow(<ProjectCard project={{ key: 'key', links: [], name: 'name', ...project }} />);
}
