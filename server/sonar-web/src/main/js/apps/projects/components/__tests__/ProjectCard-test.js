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
import ProjectCard from '../ProjectCard';

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

describe('overall status project card', () => {
  it('should never display analysis date', () => {
    expect(
      shallow(<ProjectCard measures={{}} project={PROJECT} />).find('.project-card-dates').exists()
    ).toBeFalsy();
  });

  it('should display loading', () => {
    const measures = { ...MEASURES, sqale_rating: undefined };
    expect(
      shallow(<ProjectCard project={PROJECT} />)
        .find('.boxed-group')
        .hasClass('boxed-group-loading')
    ).toBeTruthy();
    expect(
      shallow(<ProjectCard measures={measures} project={PROJECT} />)
        .find('.boxed-group')
        .hasClass('boxed-group-loading')
    ).toBeTruthy();
  });

  it('should not display the quality gate', () => {
    const project = { ...PROJECT, analysisDate: undefined };
    expect(
      shallow(<ProjectCard measures={MEASURES} project={project} />)
        .find('ProjectCardQualityGate')
        .exists()
    ).toBeFalsy();
  });

  it('should display tags', () => {
    const project = { ...PROJECT, tags: ['foo', 'bar'] };
    expect(shallow(<ProjectCard project={project} />).find('TagsList').exists()).toBeTruthy();
  });

  it('should private badge', () => {
    const project = { ...PROJECT, visibility: 'private' };
    expect(
      shallow(<ProjectCard type="overall" project={project} />).find('PrivateBadge').exists()
    ).toBeTruthy();
  });

  it('should display the overall measures and quality gate', () => {
    expect(shallow(<ProjectCard measures={MEASURES} project={PROJECT} />)).toMatchSnapshot();
  });
});

describe('leak project card', () => {
  it('should display analysis date and leak start date', () => {
    const project = { ...PROJECT, leakPeriodDate: undefined, visibility: 'private' };
    const card = shallow(<ProjectCard type="leak" measures={MEASURES} project={PROJECT} />);
    const card2 = shallow(<ProjectCard type="leak" measures={MEASURES} project={project} />);
    expect(card.find('.project-card-dates').exists()).toBeTruthy();
    expect(card.find('.project-card-dates').find('span').getNodes()).toHaveLength(2);
    expect(card.find('.project-card-dates').hasClass('width-100')).toBeFalsy();
    expect(card2.find('.project-card-dates').find('span').getNodes()).toHaveLength(1);
    expect(card2.find('.project-card-dates').hasClass('width-100')).toBeTruthy();
  });

  it('should not display analysis date or leak start date', () => {
    const project = { ...PROJECT, analysisDate: undefined };
    const card = shallow(<ProjectCard type="leak" measures={MEASURES} project={project} />);
    expect(card.find('.project-card-dates').exists()).toBeFalsy();
  });

  it('should display loading', () => {
    const measures = { ...MEASURES, new_bugs: undefined };
    expect(
      shallow(<ProjectCard type="leak" measures={measures} project={PROJECT} />)
        .find('.boxed-group')
        .hasClass('boxed-group-loading')
    ).toBeTruthy();
  });

  it('should display tags', () => {
    const project = { ...PROJECT, tags: ['foo', 'bar'] };
    expect(
      shallow(<ProjectCard type="leak" project={project} />).find('TagsList').exists()
    ).toBeTruthy();
  });

  it('should private badge', () => {
    const project = { ...PROJECT, visibility: 'private' };
    expect(
      shallow(<ProjectCard type="leak" project={project} />).find('PrivateBadge').exists()
    ).toBeTruthy();
  });

  it('should display the leak measures and quality gate', () => {
    expect(
      shallow(<ProjectCard type="leak" measures={MEASURES} project={PROJECT} />)
    ).toMatchSnapshot();
  });
});
