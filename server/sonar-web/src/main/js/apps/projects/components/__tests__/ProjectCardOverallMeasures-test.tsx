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
import * as React from 'react';
import { shallow } from 'enzyme';
import ProjectCardOverallMeasures from '../ProjectCardOverallMeasures';

it('should render correctly with all data', () => {
  const measures = {
    alert_status: 'ERROR',
    bugs: '17',
    code_smells: '132',
    coverage: '88.3',
    duplicated_lines_density: '9.8',
    ncloc: '2053',
    reliability_rating: '1.0',
    security_rating: '1.0',
    sqale_rating: '1.0',
    vulnerabilities: '0'
  };
  const wrapper = shallow(<ProjectCardOverallMeasures measures={measures} />);
  expect(wrapper).toMatchSnapshot();
});

it('should not render coverage', () => {
  const measures = {
    alert_status: 'ERROR',
    bugs: '17',
    code_smells: '132',
    duplicated_lines_density: '9.8',
    ncloc: '2053',
    reliability_rating: '1.0',
    security_rating: '1.0',
    sqale_rating: '1.0',
    vulnerabilities: '0'
  };
  const wrapper = shallow(<ProjectCardOverallMeasures measures={measures} />);
  expect(wrapper.find('[data-key="coverage"]')).toMatchSnapshot();
});

it('should not render duplications', () => {
  const measures = {
    alert_status: 'ERROR',
    bugs: '17',
    code_smells: '132',
    coverage: '88.3',
    ncloc: '2053',
    reliability_rating: '1.0',
    security_rating: '1.0',
    sqale_rating: '1.0',
    vulnerabilities: '0'
  };
  const wrapper = shallow(<ProjectCardOverallMeasures measures={measures} />);
  expect(wrapper.find('[data-key="duplicated_lines_density"]')).toMatchSnapshot();
});

it('should not render ncloc', () => {
  const measures = {
    alert_status: 'ERROR',
    bugs: '17',
    code_smells: '132',
    coverage: '88.3',
    duplicated_lines_density: '9.8',
    reliability_rating: '1.0',
    security_rating: '1.0',
    sqale_rating: '1.0',
    vulnerabilities: '0'
  };
  const wrapper = shallow(<ProjectCardOverallMeasures measures={measures} />);
  expect(wrapper.find('[data-key="ncloc"]').length).toBe(0);
});

it('should render ncloc correctly', () => {
  const measures = {
    alert_status: 'ERROR',
    bugs: '17',
    code_smells: '132',
    coverage: '88.3',
    ncloc: '16549887',
    duplicated_lines_density: '9.8',
    reliability_rating: '1.0',
    security_rating: '1.0',
    sqale_rating: '1.0',
    vulnerabilities: '0'
  };
  const wrapper = shallow(<ProjectCardOverallMeasures measures={measures} />);
  expect(wrapper.find('[data-key="ncloc"]')).toMatchSnapshot();
});
