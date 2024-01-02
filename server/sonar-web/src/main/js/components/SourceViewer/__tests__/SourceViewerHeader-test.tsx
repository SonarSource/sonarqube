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
import { mockMainBranch } from '../../../helpers/mocks/branch-like';
import { mockSourceViewerFile } from '../../../helpers/mocks/sources';
import { ComponentQualifier } from '../../../types/component';
import { MetricKey } from '../../../types/metrics';
import { Measure } from '../../../types/types';
import SourceViewerHeader from '../SourceViewerHeader';

it('should render correctly for a regular file', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly for a unit test', () => {
  expect(
    shallowRender({
      showMeasures: true,
      sourceViewerFile: mockSourceViewerFile('foo/bar.ts', 'my-project', {
        q: ComponentQualifier.TestFile,
        measures: { tests: '12' },
      }),
    })
  ).toMatchSnapshot();
});

it('should render correctly if issue details are passed', () => {
  const componentMeasures: Measure[] = [
    { metric: MetricKey.code_smells, value: '1' },
    { metric: MetricKey.file_complexity_distribution, value: '42' }, // unused, should be ignored
    { metric: MetricKey.security_hotspots, value: '2' },
    { metric: MetricKey.vulnerabilities, value: '2' },
  ];

  expect(
    shallowRender({
      componentMeasures,
      showMeasures: true,
    })
  ).toMatchSnapshot();

  expect(
    shallowRender({
      componentMeasures,
      showMeasures: false,
    })
      .find('.source-viewer-header-measure')
      .exists()
  ).toBe(false);
});

function shallowRender(props: Partial<SourceViewerHeader['props']> = {}) {
  return shallow(
    <SourceViewerHeader
      branchLike={mockMainBranch()}
      openComponent={jest.fn()}
      sourceViewerFile={mockSourceViewerFile()}
      {...props}
    />
  );
}
