/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import CoverageRating from '../../../../../components/ui/CoverageRating';
import { ComponentQualifier } from '../../../../../types/component';
import { MetricKey } from '../../../../../types/metrics';
import { Dict } from '../../../../../types/types';
import ProjectCardMeasures, { ProjectCardMeasuresProps } from '../ProjectCardMeasures';

jest.mock('date-fns', () => ({
  differenceInMilliseconds: () => 1000 * 60 * 60 * 24 * 30 * 8, // ~ 8 months
}));

describe('Overall measures', () => {
  it('should be rendered properly', () => {
    const wrapper = shallowRender();
    expect(wrapper).toMatchSnapshot();
  });

  it("should be not be rendered if there's no line of code", () => {
    let wrapper = shallowRender({ [MetricKey.ncloc]: undefined });
    expect(wrapper).toMatchSnapshot('project');

    wrapper = shallowRender(
      { ncloc: undefined },
      { componentQualifier: ComponentQualifier.Application }
    );
    expect(wrapper).toMatchSnapshot('application');
  });

  it('should not render coverage graph if there is no value for it', () => {
    const wrapper = shallowRender({ [MetricKey.coverage]: undefined });
    expect(wrapper.find(CoverageRating).exists()).toBe(false);
  });
});

describe('New code measures', () => {
  it('should be rendered properly', () => {
    const wrapper = shallowRender({}, { isNewCode: true });
    expect(wrapper).toMatchSnapshot();
  });
});

function shallowRender(
  measuresOverride: Dict<string | undefined> = {},
  props: Partial<ProjectCardMeasuresProps> = {}
) {
  const measures = {
    [MetricKey.alert_status]: 'ERROR',
    [MetricKey.bugs]: '17',
    [MetricKey.code_smells]: '132',
    [MetricKey.coverage]: '88.3',
    [MetricKey.duplicated_lines_density]: '9.8',
    [MetricKey.ncloc]: '2053',
    [MetricKey.reliability_rating]: '1.0',
    [MetricKey.security_rating]: '1.0',
    [MetricKey.sqale_rating]: '1.0',
    [MetricKey.vulnerabilities]: '0',
    [MetricKey.new_reliability_rating]: '1.0',
    [MetricKey.new_bugs]: '8',
    [MetricKey.new_security_rating]: '2.0',
    [MetricKey.new_vulnerabilities]: '2',
    [MetricKey.new_maintainability_rating]: '1.0',
    [MetricKey.new_code_smells]: '0',
    [MetricKey.new_coverage]: '26.55',
    [MetricKey.new_duplicated_lines_density]: '0.55',
    [MetricKey.new_lines]: '87',
    ...measuresOverride,
  };

  return shallow<ProjectCardMeasuresProps>(
    <ProjectCardMeasures
      componentQualifier={ComponentQualifier.Project}
      isNewCode={false}
      measures={measures}
      newCodeStartingDate="2018-01-01"
      {...props}
    />
  );
}
