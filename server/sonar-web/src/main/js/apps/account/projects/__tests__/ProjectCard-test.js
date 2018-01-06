/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import { shallow } from 'enzyme';
import { Link } from 'react-router';
import ProjectCard from '../ProjectCard';
import Level from '../../../../components/ui/Level';

const BASE = { id: 'id', key: 'key', name: 'name', links: [] };

it('should render key and name', () => {
  const project = { ...BASE };
  const output = shallow(<ProjectCard project={project} />);
  expect(output.find('.account-project-key').text()).toBe('key');
  expect(
    output
      .find('.account-project-name')
      .find(Link)
      .prop('children')
  ).toBe('name');
});

it('should render description', () => {
  const project = { ...BASE, description: 'bla' };
  const output = shallow(<ProjectCard project={project} />);
  expect(output.find('.account-project-description').text()).toBe('bla');
});

it('should not render optional fields', () => {
  const project = { ...BASE };
  const output = shallow(<ProjectCard project={project} />);
  expect(output.find('.account-project-description').length).toBe(0);
  expect(output.find('.account-project-quality-gate').length).toBe(0);
  expect(output.find('.account-project-links').length).toBe(0);
});

it('should render analysis date', () => {
  const project = { ...BASE, lastAnalysisDate: '2016-05-17' };
  const output = shallow(<ProjectCard project={project} />);
  expect(output.find('.account-project-analysis DateFromNow')).toHaveLength(1);
});

it('should not render analysis date', () => {
  const project = { ...BASE };
  const output = shallow(<ProjectCard project={project} />);
  expect(output.find('.account-project-analysis').text()).toContain(
    'my_account.projects.never_analyzed'
  );
});

it('should render quality gate status', () => {
  const project = { ...BASE, qualityGate: 'ERROR' };
  const output = shallow(<ProjectCard project={project} />);
  expect(
    output
      .find('.account-project-quality-gate')
      .find(Level)
      .prop('level')
  ).toBe('ERROR');
});

it('should render links', () => {
  const project = {
    ...BASE,
    links: [{ name: 'n', type: 't', href: 'h' }]
  };
  const output = shallow(<ProjectCard project={project} />);
  expect(output.find('.account-project-links').find('li').length).toBe(1);
});
