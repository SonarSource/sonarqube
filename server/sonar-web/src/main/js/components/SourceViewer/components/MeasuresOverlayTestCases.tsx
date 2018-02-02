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
import * as classNames from 'classnames';
import { orderBy } from 'lodash';
import MeasuresOverlayCoveredFiles from './MeasuresOverlayCoveredFiles';
import MeasuresOverlayTestCase from './MeasuresOverlayTestCase';
import { getTests } from '../../../api/tests';
import { TestCase, BranchLike } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import { getBranchLikeQuery } from '../../../helpers/branches';

interface Props {
  branchLike: BranchLike | undefined;
  componentKey: string;
}

interface State {
  loading: boolean;
  selectedTestId?: string;
  sort?: string;
  sortAsc?: boolean;
  testCases?: TestCase[];
}

export default class MeasuresOverlayTestCases extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchTests();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.branchLike !== this.props.branchLike ||
      prevProps.componentKey !== this.props.componentKey
    ) {
      this.fetchTests();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchTests = () => {
    // TODO implement pagination one day...
    this.setState({ loading: true });
    getTests({
      ps: 500,
      testFileKey: this.props.componentKey,
      ...getBranchLikeQuery(this.props.branchLike)
    }).then(
      ({ tests: testCases }) => {
        if (this.mounted) {
          this.setState({ loading: false, testCases });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleTestCasesSortClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    const { sort } = event.currentTarget.dataset;
    if (sort) {
      this.setState((state: State) => ({
        sort,
        sortAsc: sort === state.sort ? !state.sortAsc : true
      }));
    }
  };

  handleTestCaseClick = (selectedTestId: string) => {
    this.setState({ selectedTestId });
  };

  render() {
    const { selectedTestId, sort = 'name', sortAsc = true, testCases } = this.state;

    if (!testCases) {
      return null;
    }

    const selectedTest = testCases.find(test => test.id === selectedTestId);

    return (
      <div className="source-viewer-measures">
        <div className="source-viewer-measures-section source-viewer-measures-section-big">
          <div className="source-viewer-measures-card source-viewer-measures-card-fixed-height js-test-list">
            <div className="measures">
              <table className="source-viewer-tests-list">
                <tbody>
                  <tr>
                    <td className="source-viewer-test-status note" colSpan={3}>
                      {translate('component_viewer.measure_section.unit_tests')}
                      <br />
                      <span className="spacer-right">
                        {translate('component_viewer.tests.ordered_by')}
                      </span>
                      <a
                        className={classNames('js-sort-tests-by-duration', {
                          'active-link': sort === 'duration'
                        })}
                        data-sort="duration"
                        href="#"
                        onClick={this.handleTestCasesSortClick}>
                        {translate('component_viewer.tests.duration')}
                      </a>
                      <span className="slash-separator" />
                      <a
                        className={classNames('js-sort-tests-by-name', {
                          'active-link': sort === 'name'
                        })}
                        data-sort="name"
                        href="#"
                        onClick={this.handleTestCasesSortClick}>
                        {translate('component_viewer.tests.test_name')}
                      </a>
                      <span className="slash-separator" />
                      <a
                        className={classNames('js-sort-tests-by-status', {
                          'active-link': sort === 'status'
                        })}
                        data-sort="status"
                        href="#"
                        onClick={this.handleTestCasesSortClick}>
                        {translate('component_viewer.tests.status')}
                      </a>
                    </td>
                    <td className="source-viewer-test-covered-lines note">
                      {translate('component_viewer.covered_lines')}
                    </td>
                  </tr>
                  {sortTestCases(testCases, sort, sortAsc).map(testCase => (
                    <MeasuresOverlayTestCase
                      key={testCase.id}
                      onClick={this.handleTestCaseClick}
                      testCase={testCase}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
        {selectedTest && <MeasuresOverlayCoveredFiles testCase={selectedTest} />}
      </div>
    );
  }
}

function sortTestCases(testCases: TestCase[], sort: string, sortAsc: boolean) {
  const mainOrder = sortAsc ? 'asc' : 'desc';
  if (sort === 'duration') {
    return orderBy(testCases, ['durationInMs', 'name'], [mainOrder, 'asc']);
  } else if (sort === 'status') {
    return orderBy(testCases, ['status', 'name'], [mainOrder, 'asc']);
  } else {
    return orderBy(testCases, ['name'], [mainOrder]);
  }
}
