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
import { groupBy } from 'lodash';
import * as PropTypes from 'prop-types';
import { getTests } from '../../../api/components';
import { BranchLike, SourceLine, TestCase } from '../../../app/types';
import { DropdownOverlay } from '../../controls/Dropdown';
import TestStatusIcon from '../../shared/TestStatusIcon';
import { PopupPlacement } from '../../ui/popups';
import { WorkspaceContext } from '../../workspace/context';
import { isSameBranchLike, getBranchLikeQuery } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import { collapsePath } from '../../../helpers/path';

interface Props {
  branchLike: BranchLike | undefined;
  componentKey: string;
  line: SourceLine;
  onClose: () => void;
}

interface State {
  loading: boolean;
  testCases: TestCase[];
}

export default class CoveragePopup extends React.PureComponent<Props, State> {
  context!: { workspace: WorkspaceContext };
  mounted = false;

  static contextTypes = {
    workspace: PropTypes.object.isRequired
  };

  state: State = { loading: true, testCases: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchTests();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
      prevProps.componentKey !== this.props.componentKey ||
      prevProps.line.line !== this.props.line.line
    ) {
      this.fetchTests();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchTests = () => {
    this.setState({ loading: true });
    getTests({
      sourceFileKey: this.props.componentKey,
      sourceFileLineNumber: this.props.line.line,
      ...getBranchLikeQuery(this.props.branchLike)
    }).then(
      testCases => {
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

  handleTestClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    const { key } = event.currentTarget.dataset;
    if (key) {
      this.context.workspace.openComponent({ branchLike: this.props.branchLike, key });
    }
    this.props.onClose();
  };

  render() {
    const { line } = this.props;
    const testCasesByFile = groupBy(this.state.testCases || [], 'fileKey');
    const testFiles = Object.keys(testCasesByFile).map(fileKey => {
      const testSet = testCasesByFile[fileKey];
      const test = testSet[0];
      return {
        file: { key: test.fileKey, longName: test.fileName },
        tests: testSet
      };
    });

    return (
      <DropdownOverlay placement={PopupPlacement.RightTop}>
        <div className="abs-width-400">
          <h6 className="spacer-bottom">
            {translate('source_viewer.covered')}
            {!!line.conditions && (
              <div>
                {'('}
                {line.coveredConditions || '0'}
                {' of '}
                {line.conditions} {translate('source_viewer.conditions')}
                {')'}
              </div>
            )}
          </h6>
          {this.state.loading ? (
            <i className="spinner" />
          ) : (
            <>
              {testFiles.length === 0 &&
                translate('source_viewer.tooltip.no_information_about_tests')}
              {testFiles.map(testFile => (
                <div className="spacer-top text-ellipsis" key={testFile.file.key}>
                  <a
                    data-key={testFile.file.key}
                    href="#"
                    onClick={this.handleTestClick}
                    title={testFile.file.longName}>
                    <span>{collapsePath(testFile.file.longName)}</span>
                  </a>
                  <ul>
                    {testFile.tests.map(testCase => (
                      <li
                        className="display-flex-center little-spacer-top"
                        key={testCase.id}
                        title={testCase.name}>
                        <TestStatusIcon className="spacer-right" status={testCase.status} />
                        <div className="display-inline-block text-ellipsis">{testCase.name}</div>
                        {testCase.status !== 'SKIPPED' && (
                          <span className="spacer-left note">{testCase.durationInMs}ms</span>
                        )}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </>
          )}
        </div>
      </DropdownOverlay>
    );
  }
}
