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
import { Location } from 'sonar-ui-common/helpers/urls';
import { mockBranch } from '../../../../../../../helpers/mocks/branch-like';
import { mockMetric } from '../../../../../../../helpers/testMocks';
import { MetricKey } from '../../../../../../../types/metrics';
import ProjectBadges from '../ProjectBadges';

jest.mock('sonar-ui-common/helpers/urls', () => ({
  getHostUrl: () => 'host',
  getPathUrlAsString: (l: Location) => l.pathname
}));

jest.mock('../../../../../../../helpers/urls', () => ({
  getProjectUrl: () => ({ pathname: '/dashboard' } as Location)
}));

it('should display correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

function shallowRender(overrides = {}) {
  return shallow(
    <ProjectBadges
      branchLike={mockBranch()}
      metrics={{
        [MetricKey.coverage]: mockMetric({ key: MetricKey.coverage }),
        [MetricKey.new_code_smells]: mockMetric({ key: MetricKey.new_code_smells })
      }}
      project="foo"
      qualifier="TRK"
      {...overrides}
    />
  );
}
