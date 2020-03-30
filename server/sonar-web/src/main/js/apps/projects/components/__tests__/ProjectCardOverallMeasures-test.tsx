/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { ComponentQualifier } from '../../../../types/component';
import ProjectCardOverallMeasures from '../ProjectCardOverallMeasures';

it('should render correctly with all data', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should not render coverage', () => {
  expect(
    shallowRender({ coverage: undefined })
      .find('[data-key="coverage"]')
      .exists()
  ).toBe(false);
});

it('should render empty', () => {
  expect(shallowRender({ ncloc: undefined })).toMatchSnapshot('project');
  expect(shallowRender({ ncloc: undefined }, ComponentQualifier.Application)).toMatchSnapshot(
    'application'
  );
});

it('should render ncloc correctly', () => {
  expect(shallowRender({ ncloc: '16549887' }).find('[data-key="ncloc"]')).toMatchSnapshot();
});

function shallowRender(
  overriddenMeasures: T.Dict<string | undefined> = {},
  componentQualifier?: ComponentQualifier
) {
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
    vulnerabilities: '0',
    ...overriddenMeasures
  };
  return shallow(
    <ProjectCardOverallMeasures
      componentQualifier={componentQualifier ?? ComponentQualifier.Project}
      measures={measures}
    />
  );
}
