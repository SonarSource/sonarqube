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
import ProjectCardLeakMeasures from '../ProjectCardLeakMeasures';

it('should render correctly with all data', () => {
  const measures = {
    alert_status: 'ERROR',
    new_reliability_rating: '1.0',
    new_bugs: '8',
    new_security_rating: '2.0',
    new_vulnerabilities: '2',
    new_maintainability_rating: '1.0',
    new_code_smells: '0',
    new_coverage: '26.55',
    new_duplicated_lines_density: '0.55',
    new_lines: '87'
  };
  const wrapper = shallow(<ProjectCardLeakMeasures measures={measures} />);
  expect(wrapper).toMatchSnapshot();
});

it('should render no data style new coverage, new duplications and new lines', () => {
  const measures = {
    alert_status: 'ERROR',
    new_reliability_rating: '1.0',
    new_bugs: '8',
    new_security_rating: '2.0',
    new_vulnerabilities: '2',
    new_maintainability_rating: '1.0',
    new_code_smells: '0'
  };
  const wrapper = shallow(<ProjectCardLeakMeasures measures={measures} />);
  expect(wrapper).toMatchSnapshot();
});
