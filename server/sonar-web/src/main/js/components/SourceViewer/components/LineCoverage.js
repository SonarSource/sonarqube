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
// @flow
import React from 'react';
import Tooltip from '../../controls/Tooltip';
import { translate } from '../../../helpers/l10n';
/*:: import type { SourceLine } from '../types'; */

/*::
type Props = {
  line: SourceLine,
  onClick: (SourceLine, HTMLElement) => void
};
*/

export default class LineCoverage extends React.PureComponent {
  /*:: props: Props; */

  handleClick = (e /*: SyntheticInputEvent */) => {
    e.preventDefault();
    this.props.onClick(this.props.line, e.target);
  };

  render() {
    const { line } = this.props;
    const className =
      'source-meta source-line-coverage' +
      (line.coverageStatus != null ? ` source-line-${line.coverageStatus}` : '');
    const hasPopup =
      line.coverageStatus === 'covered' || line.coverageStatus === 'partially-covered';
    const cell = (
      <td
        className={className}
        data-line-number={line.line}
        role={hasPopup ? 'button' : undefined}
        tabIndex={hasPopup ? 0 : undefined}
        onClick={hasPopup ? this.handleClick : undefined}>
        <div className="source-line-bar" />
      </td>
    );

    return line.coverageStatus != null ? (
      <Tooltip overlay={translate('source_viewer.tooltip', line.coverageStatus)} placement="right">
        {cell}
      </Tooltip>
    ) : (
      cell
    );
  }
}
