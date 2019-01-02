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
import * as React from 'react';
import { shallow } from 'enzyme';
import ProjectCardLeak from '../ProjectCardLeak';
import { Project } from '../../types';

jest.mock(
  'date-fns/difference_in_milliseconds',
  () => () => 1000 * 60 * 60 * 24 * 30 * 8 // ~ 8 months
);

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

it('should display analysis date and leak start date', () => {
  const card = shallow(<ProjectCardLeak height={100} organization={undefined} project={PROJECT} />);
  expect(card.find('.project-card-dates').exists()).toBeTruthy();
  expect(card.find('.project-card-dates').find('.project-card-leak-date')).toHaveLength(1);
  expect(card.find('.project-card-dates').find('DateTimeFormatter')).toHaveLength(1);
});

it('should not display analysis date or leak start date', () => {
  const project = { ...PROJECT, analysisDate: undefined };
  const card = shallow(<ProjectCardLeak height={100} organization={undefined} project={project} />);
  expect(card.find('.project-card-dates').exists()).toBeFalsy();
});

it('should display tags', () => {
  const project = { ...PROJECT, tags: ['foo', 'bar'] };
  expect(
    shallow(<ProjectCardLeak height={100} organization={undefined} project={project} />)
      .find('TagsList')
      .exists()
  ).toBeTruthy();
});

it('should display private badge', () => {
  const project: Project = { ...PROJECT, visibility: 'private' };
  expect(
    shallow(<ProjectCardLeak height={100} organization={undefined} project={project} />)
      .find('Connect(PrivacyBadge)')
      .exists()
  ).toBeTruthy();
});

it('should display the leak measures and quality gate', () => {
  expect(
    shallow(<ProjectCardLeak height={100} organization={undefined} project={PROJECT} />)
  ).toMatchSnapshot();
});

it('should display not analyzed yet', () => {
  expect(
    shallow(
      <ProjectCardLeak
        height={100}
        organization={undefined}
        project={{ ...PROJECT, analysisDate: undefined }}
      />
    )
  ).toMatchSnapshot();
});
