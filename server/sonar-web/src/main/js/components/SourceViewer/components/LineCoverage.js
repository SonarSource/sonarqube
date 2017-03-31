/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { translate } from '../../../helpers/l10n';
import type { SourceLine } from '../types';

type Props = {
  line: SourceLine,
  onClick: (SourceLine, HTMLElement) => void
};

export default class LineCoverage extends React.PureComponent {
  props: Props;

  handleClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.props.onClick(this.props.line, e.target);
  };

  render() {
    const { line } = this.props;
    const className = 'source-meta source-line-coverage' +
      (line.coverageStatus != null ? ` source-line-${line.coverageStatus}` : '');
    const title = line.coverageStatus != null
      ? translate('source_viewer.tooltip', line.coverageStatus)
      : undefined;
    return (
      <td
        className={className}
        data-line-number={line.line}
        title={title}
        data-placement={line.coverageStatus != null ? 'right' : undefined}
        data-toggle={line.coverageStatus != null ? 'tooltip' : undefined}
        role={line.coverageStatus != null ? 'button' : undefined}
        tabIndex={line.coverageStatus != null ? 0 : undefined}
        onClick={line.coverageStatus != null ? this.handleClick : undefined}>
        <div className="source-line-bar" />
      </td>
    );
  }
}
