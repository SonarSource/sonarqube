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
import { TestCase } from '../../../app/types';
import TestStatusIcon from '../../shared/TestStatusIcon';

interface Props {
  onClick: (testId: string) => void;
  testCase: TestCase;
}

export default class MeasuresOverlayTestCase extends React.PureComponent<Props> {
  handleTestCaseClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.props.onClick(this.props.testCase.id);
  };

  render() {
    const { testCase } = this.props;
    const { status } = testCase;
    const hasAdditionalData = status !== 'OK' || (status === 'OK' && testCase.coveredLines);

    return (
      <tr>
        <td className="source-viewer-test-status">
          <TestStatusIcon status={status} />
        </td>
        <td className="source-viewer-test-duration note">
          {status !== 'SKIPPED' && `${testCase.durationInMs}ms`}
        </td>
        <td className="source-viewer-test-name">
          {hasAdditionalData ? (
            <a className="js-show-test" href="#" onClick={this.handleTestCaseClick}>
              {testCase.name}
            </a>
          ) : (
            testCase.name
          )}
        </td>
        <td className="source-viewer-test-covered-lines note">{testCase.coveredLines}</td>
      </tr>
    );
  }
}
