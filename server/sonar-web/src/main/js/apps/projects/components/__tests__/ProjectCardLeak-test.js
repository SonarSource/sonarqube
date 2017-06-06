/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import ProjectCardLeak from '../ProjectCardLeak';

const PROJECT = {
  analysisDate: '2017-01-01',
  leakPeriodDate: '2016-12-01',
  key: 'foo',
  name: 'Foo',
  tags: []
};
const MEASURES = {
  alert_status: 'OK',
  reliability_rating: '1.0',
  sqale_rating: '1.0',
  new_bugs: 12
};

jest.mock('moment', () => () => ({
  format: () => 'March 1, 2017 9:36 AM',
  fromNow: () => 'a month ago'
}));

it('should display analysis date and leak start date', () => {
  const card = shallow(<ProjectCardLeak type="leak" measures={MEASURES} project={PROJECT} />);
  expect(card.find('.project-card-dates').exists()).toBeTruthy();
  expect(card.find('.project-card-dates').find('span').getNodes()).toHaveLength(2);
});

it('should not display analysis date or leak start date', () => {
  const project = { ...PROJECT, analysisDate: undefined };
  const card = shallow(<ProjectCardLeak type="leak" measures={MEASURES} project={project} />);
  expect(card.find('.project-card-dates').exists()).toBeFalsy();
});

it('should display loading', () => {
  const measures = { ...MEASURES, new_bugs: undefined };
  expect(
    shallow(<ProjectCardLeak type="leak" measures={measures} project={PROJECT} />)
      .find('.boxed-group')
      .hasClass('boxed-group-loading')
  ).toBeTruthy();
});

it('should display tags', () => {
  const project = { ...PROJECT, tags: ['foo', 'bar'] };
  expect(
    shallow(<ProjectCardLeak type="leak" project={project} />).find('TagsList').exists()
  ).toBeTruthy();
});

it('should private badge', () => {
  const project = { ...PROJECT, visibility: 'private' };
  expect(
    shallow(<ProjectCardLeak type="leak" project={project} />).find('PrivateBadge').exists()
  ).toBeTruthy();
});

it('should display the leak measures and quality gate', () => {
  expect(
    shallow(<ProjectCardLeak type="leak" measures={MEASURES} project={PROJECT} />)
  ).toMatchSnapshot();
});
