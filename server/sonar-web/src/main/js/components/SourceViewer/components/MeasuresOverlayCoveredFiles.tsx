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
import { Link } from 'react-router';
import { getCoveredFiles } from '../../../api/tests';
import { TestCase, CoveredFile } from '../../../app/types';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectUrl } from '../../../helpers/urls';
import DeferredSpinner from '../../common/DeferredSpinner';

interface Props {
  testCase: TestCase;
}

interface State {
  coveredFiles?: CoveredFile[];
  loading: boolean;
}

export default class MeasuresOverlayCoveredFiles extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchCoveredFiles();
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.testCase.id !== prevProps.testCase.id) {
      this.fetchCoveredFiles();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchCoveredFiles = () => {
    this.setState({ loading: true });
    getCoveredFiles({ testId: this.props.testCase.id }).then(
      coveredFiles => {
        if (this.mounted) {
          this.setState({ coveredFiles, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { testCase } = this.props;
    const { loading, coveredFiles } = this.state;

    return (
      <div className="source-viewer-measures-section source-viewer-measures-section-big js-selected-test">
        <DeferredSpinner loading={loading}>
          <div className="source-viewer-measures-card source-viewer-measures-card-fixed-height">
            {testCase.status !== 'ERROR' &&
              testCase.status !== 'FAILURE' &&
              coveredFiles !== undefined && (
                <>
                  <div className="bubble-popup-title">
                    {translate('component_viewer.transition.covers')}
                  </div>
                  {coveredFiles.length > 0
                    ? coveredFiles.map(coveredFile => (
                        <div className="bubble-popup-section" key={coveredFile.key}>
                          <Link to={getProjectUrl(coveredFile.key)}>{coveredFile.longName}</Link>
                          <span className="note spacer-left">
                            {translateWithParameters(
                              'component_viewer.x_lines_are_covered',
                              coveredFile.coveredLines
                            )}
                          </span>
                        </div>
                      ))
                    : translate('none')}
                </>
              )}

            {testCase.status !== 'OK' && (
              <>
                <div className="bubble-popup-title">{translate('component_viewer.details')}</div>
                {testCase.message && <pre>{testCase.message}</pre>}
                <pre>{testCase.stacktrace}</pre>
              </>
            )}
          </div>
        </DeferredSpinner>
      </div>
    );
  }
}
