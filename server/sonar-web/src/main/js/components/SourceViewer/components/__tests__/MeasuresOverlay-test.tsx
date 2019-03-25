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
import MeasuresOverlay from '../MeasuresOverlay';
import { waitAndUpdate, click } from '../../../../helpers/testUtils';

jest.mock('../../../../api/issues', () => ({
  getFacets: () =>
    Promise.resolve({
      facets: [
        {
          property: 'types',
          values: [
            { val: 'CODE_SMELL', count: 2 },
            { val: 'BUG', count: 1 },
            { val: 'VULNERABILITY', count: 0 }
          ]
        },
        {
          property: 'severities',
          values: [
            { val: 'MAJOR', count: 1 },
            { val: 'INFO', count: 2 },
            { val: 'MINOR', count: 3 },
            { val: 'CRITICAL', count: 4 },
            { val: 'BLOCKER', count: 5 }
          ]
        },
        {
          property: 'tags',
          values: [
            { val: 'bad-practice', count: 1 },
            { val: 'cert', count: 3 },
            { val: 'design', count: 1 }
          ]
        }
      ]
    })
}));

jest.mock('../../../../api/measures', () => ({
  getMeasures: () =>
    Promise.resolve([
      { metric: 'vulnerabilities', value: '0' },
      { metric: 'complexity', value: '1' },
      { metric: 'test_errors', value: '1' },
      { metric: 'comment_lines_density', value: '20.0' },
      { metric: 'wont_fix_issues', value: '0' },
      { metric: 'uncovered_lines', value: '1' },
      { metric: 'functions', value: '1' },
      { metric: 'duplicated_files', value: '1' },
      { metric: 'duplicated_blocks', value: '3' },
      { metric: 'line_coverage', value: '75.0' },
      { metric: 'duplicated_lines_density', value: '0.0' },
      { metric: 'comment_lines', value: '2' },
      { metric: 'ncloc', value: '8' },
      { metric: 'reliability_rating', value: '1.0' },
      { metric: 'false_positive_issues', value: '0' },
      { metric: 'reliability_remediation_effort', value: '0' },
      { metric: 'code_smells', value: '2' },
      { metric: 'security_rating', value: '1.0' },
      { metric: 'test_success_density', value: '100.0' },
      { metric: 'cognitive_complexity', value: '0' },
      { metric: 'files', value: '1' },
      { metric: 'duplicated_lines', value: '0' },
      { metric: 'lines', value: '18' },
      { metric: 'classes', value: '1' },
      { metric: 'bugs', value: '0' },
      { metric: 'lines_to_cover', value: '4' },
      { metric: 'sqale_index', value: '40' },
      { metric: 'sqale_debt_ratio', value: '16.7' },
      { metric: 'coverage', value: '75.0' },
      { metric: 'security_remediation_effort', value: '0' },
      { metric: 'statements', value: '3' },
      { metric: 'skipped_tests', value: '0' },
      { metric: 'test_failures', value: '0' },
      { metric: 'violations', value: '1' }
    ])
}));

jest.mock('../../../../api/metrics', () => ({
  getAllMetrics: () =>
    Promise.resolve([
      { key: 'vulnerabilities', type: 'INT', domain: 'Security' },
      { key: 'complexity', type: 'INT', domain: 'Complexity' },
      { key: 'test_errors', type: 'INT', domain: 'Tests' },
      { key: 'comment_lines_density', type: 'PERCENT', domain: 'Size' },
      { key: 'wont_fix_issues', type: 'INT', domain: 'Issues' },
      { key: 'uncovered_lines', type: 'INT', domain: 'Coverage' },
      { key: 'functions', type: 'INT', domain: 'Size' },
      { key: 'duplicated_files', type: 'INT', domain: 'Duplications' },
      { key: 'duplicated_blocks', type: 'INT', domain: 'Duplications' },
      { key: 'line_coverage', type: 'PERCENT', domain: 'Coverage' },
      { key: 'duplicated_lines_density', type: 'PERCENT', domain: 'Duplications' },
      { key: 'comment_lines', type: 'INT', domain: 'Size' },
      { key: 'ncloc', type: 'INT', domain: 'Size' },
      { key: 'reliability_rating', type: 'RATING', domain: 'Reliability' },
      { key: 'false_positive_issues', type: 'INT', domain: 'Issues' },
      { key: 'code_smells', type: 'INT', domain: 'Maintainability' },
      { key: 'security_rating', type: 'RATING', domain: 'Security' },
      { key: 'test_success_density', type: 'PERCENT', domain: 'Tests' },
      { key: 'cognitive_complexity', type: 'INT', domain: 'Complexity' },
      { key: 'files', type: 'INT', domain: 'Size' },
      { key: 'duplicated_lines', type: 'INT', domain: 'Duplications' },
      { key: 'lines', type: 'INT', domain: 'Size' },
      { key: 'classes', type: 'INT', domain: 'Size' },
      { key: 'bugs', type: 'INT', domain: 'Reliability' },
      { key: 'lines_to_cover', type: 'INT', domain: 'Coverage' },
      { key: 'sqale_index', type: 'WORK_DUR', domain: 'Maintainability' },
      { key: 'sqale_debt_ratio', type: 'PERCENT', domain: 'Maintainability' },
      { key: 'coverage', type: 'PERCENT', domain: 'Coverage' },
      { key: 'statements', type: 'INT', domain: 'Size' },
      { key: 'skipped_tests', type: 'INT', domain: 'Tests' },
      { key: 'test_failures', type: 'INT', domain: 'Tests' },
      { key: 'violations', type: 'INT', domain: 'Issues' },
      // next two must be filtered out
      { key: 'data', type: 'DATA' },
      { key: 'hidden', hidden: true }
    ])
}));

const sourceViewerFile: T.SourceViewerFile = {
  key: 'component-key',
  measures: {},
  path: 'src/file.js',
  project: 'project-key',
  projectName: 'Project Name',
  q: 'FIL',
  subProject: 'sub-project-key',
  subProjectName: 'Sub-Project Name',
  uuid: 'abcd123'
};

const branchLike: T.ShortLivingBranch = {
  isMain: false,
  mergeBranch: 'master',
  name: 'feature',
  type: 'SHORT'
};

it('should render source file', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  click(wrapper.find('.js-show-all-measures'));
  expect(wrapper).toMatchSnapshot();
});

it('should render test file', async () => {
  const wrapper = shallowRender({ sourceViewerFile: { ...sourceViewerFile, q: 'UTS' } });
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
