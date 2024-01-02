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
import { mockBranch } from '../../../../helpers/mocks/branch-like';
import { click, waitAndUpdate } from '../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { SourceViewerFile } from '../../../../types/types';
import MeasuresOverlay from '../MeasuresOverlay';

jest.mock('../../../../api/issues', () => ({
  getFacets: () =>
    Promise.resolve({
      facets: [
        {
          property: 'types',
          values: [
            { val: 'CODE_SMELL', count: 2 },
            { val: 'BUG', count: 1 },
            { val: 'VULNERABILITY', count: 0 },
          ],
        },
        {
          property: 'severities',
          values: [
            { val: 'MAJOR', count: 1 },
            { val: 'INFO', count: 2 },
            { val: 'MINOR', count: 3 },
            { val: 'CRITICAL', count: 4 },
            { val: 'BLOCKER', count: 5 },
          ],
        },
        {
          property: 'tags',
          values: [
            { val: 'bad-practice', count: 1 },
            { val: 'cert', count: 3 },
            { val: 'design', count: 1 },
          ],
        },
      ],
    }),
}));

jest.mock('../../../../api/measures', () => ({
  getMeasures: () =>
    Promise.resolve([
      { metric: MetricKey.vulnerabilities, value: '0' },
      { metric: MetricKey.complexity, value: '1' },
      { metric: MetricKey.test_errors, value: '1' },
      { metric: MetricKey.comment_lines_density, value: '20.0' },
      { metric: MetricKey.wont_fix_issues, value: '0' },
      { metric: MetricKey.uncovered_lines, value: '1' },
      { metric: MetricKey.functions, value: '1' },
      { metric: MetricKey.duplicated_files, value: '1' },
      { metric: MetricKey.duplicated_blocks, value: '3' },
      { metric: MetricKey.line_coverage, value: '75.0' },
      { metric: MetricKey.duplicated_lines_density, value: '0.0' },
      { metric: MetricKey.comment_lines, value: '2' },
      { metric: MetricKey.ncloc, value: '8' },
      { metric: MetricKey.reliability_rating, value: '1.0' },
      { metric: MetricKey.false_positive_issues, value: '0' },
      { metric: MetricKey.reliability_remediation_effort, value: '0' },
      { metric: MetricKey.code_smells, value: '2' },
      { metric: MetricKey.security_rating, value: '1.0' },
      { metric: MetricKey.test_success_density, value: '100.0' },
      { metric: MetricKey.cognitive_complexity, value: '0' },
      { metric: MetricKey.files, value: '1' },
      { metric: MetricKey.duplicated_lines, value: '0' },
      { metric: MetricKey.lines, value: '18' },
      { metric: MetricKey.classes, value: '1' },
      { metric: MetricKey.bugs, value: '0' },
      { metric: MetricKey.lines_to_cover, value: '4' },
      { metric: MetricKey.sqale_index, value: '40' },
      { metric: MetricKey.sqale_debt_ratio, value: '16.7' },
      { metric: MetricKey.coverage, value: '75.0' },
      { metric: MetricKey.security_remediation_effort, value: '0' },
      { metric: MetricKey.statements, value: '3' },
      { metric: MetricKey.skipped_tests, value: '0' },
      { metric: MetricKey.test_failures, value: '0' },
      { metric: MetricKey.violations, value: '1' },
    ]),
}));

jest.mock('../../../../api/metrics', () => ({
  getAllMetrics: () =>
    Promise.resolve([
      { key: MetricKey.vulnerabilities, type: 'INT', domain: 'Security' },
      { key: MetricKey.complexity, type: 'INT', domain: 'Complexity' },
      { key: MetricKey.test_errors, type: 'INT', domain: 'Tests' },
      { key: MetricKey.comment_lines_density, type: 'PERCENT', domain: 'Size' },
      { key: MetricKey.wont_fix_issues, type: 'INT', domain: 'Issues' },
      { key: MetricKey.uncovered_lines, type: 'INT', domain: 'Coverage' },
      { key: MetricKey.functions, type: 'INT', domain: 'Size' },
      { key: MetricKey.duplicated_files, type: 'INT', domain: 'Duplications' },
      { key: MetricKey.duplicated_blocks, type: 'INT', domain: 'Duplications' },
      { key: MetricKey.line_coverage, type: 'PERCENT', domain: 'Coverage' },
      { key: MetricKey.duplicated_lines_density, type: 'PERCENT', domain: 'Duplications' },
      { key: MetricKey.comment_lines, type: 'INT', domain: 'Size' },
      { key: MetricKey.ncloc, type: 'INT', domain: 'Size' },
      { key: MetricKey.reliability_rating, type: 'RATING', domain: 'Reliability' },
      { key: MetricKey.false_positive_issues, type: 'INT', domain: 'Issues' },
      { key: MetricKey.code_smells, type: 'INT', domain: 'Maintainability' },
      { key: MetricKey.security_rating, type: 'RATING', domain: 'Security' },
      { key: MetricKey.test_success_density, type: 'PERCENT', domain: 'Tests' },
      { key: MetricKey.cognitive_complexity, type: 'INT', domain: 'Complexity' },
      { key: MetricKey.files, type: 'INT', domain: 'Size' },
      { key: MetricKey.duplicated_lines, type: 'INT', domain: 'Duplications' },
      { key: MetricKey.lines, type: 'INT', domain: 'Size' },
      { key: MetricKey.classes, type: 'INT', domain: 'Size' },
      { key: MetricKey.bugs, type: 'INT', domain: 'Reliability' },
      { key: MetricKey.lines_to_cover, type: 'INT', domain: 'Coverage' },
      { key: MetricKey.sqale_index, type: 'WORK_DUR', domain: 'Maintainability' },
      { key: MetricKey.sqale_debt_ratio, type: 'PERCENT', domain: 'Maintainability' },
      { key: MetricKey.coverage, type: 'PERCENT', domain: 'Coverage' },
      { key: MetricKey.statements, type: 'INT', domain: 'Size' },
      { key: MetricKey.skipped_tests, type: 'INT', domain: 'Tests' },
      { key: MetricKey.test_failures, type: 'INT', domain: 'Tests' },
      { key: MetricKey.violations, type: 'INT', domain: 'Issues' },
      // next two must be filtered out
      { key: 'data', type: 'DATA' },
      { key: 'hidden', hidden: true },
    ]),
}));

const sourceViewerFile: SourceViewerFile = {
  key: 'component-key',
  measures: {},
  path: 'src/file.js',
  project: 'project-key',
  projectName: 'Project Name',
  q: ComponentQualifier.File,
  uuid: 'abcd123',
};

const branchLike = mockBranch({ name: 'feature' });

it('should render source file', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-show-all-measures'));
  expect(wrapper).toMatchSnapshot();
});

it('should render test file', async () => {
  const wrapper = shallowRender({
    sourceViewerFile: { ...sourceViewerFile, q: ComponentQualifier.TestFile },
  });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props: Partial<MeasuresOverlay['props']> = {}) {
  return shallow(
    <MeasuresOverlay
      branchLike={branchLike}
      onClose={jest.fn()}
      sourceViewerFile={sourceViewerFile}
      {...props}
    />
  );
}
