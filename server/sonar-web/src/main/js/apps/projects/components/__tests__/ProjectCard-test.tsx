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
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { Project } from '../../types';
import ProjectCard from '../ProjectCard';

const ORGANIZATION = { key: 'org', name: 'org' };

const MEASURES = {
  alert_status: 'OK',
  reliability_rating: '1.0',
  sqale_rating: '1.0',
  new_bugs: '12'
};

const PROJECT: Project = {
  analysisDate: '2017-01-01',
  leakPeriodDate: '2016-12-01',
  key: 'foo',
  measures: MEASURES,
  name: 'Foo',
  organization: { key: 'org', name: 'org' },
  tags: [],
  visibility: 'public'
};

it('should show <ProjectCardOverall/> by default', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('ProjectCardOverall')).toBeTruthy();
  expect(wrapper.find('ProjectCardLeak')).toBeTruthy();
});

it('should show <ProjectCardLeak/> when asked', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('ProjectCardLeak')).toBeTruthy();
  expect(wrapper.find('ProjectCardOverall')).toBeTruthy();
});

function shallowRender(type?: string) {
  return shallow(
    <ProjectCard
      currentUser={mockCurrentUser()}
      handleFavorite={jest.fn}
      height={200}
      organization={ORGANIZATION}
      project={PROJECT}
      type={type}
    />
  );
}
