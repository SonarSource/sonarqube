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
/*:: import type { SourceLine } from '../types'; */

/*::
type Props = {
  line: SourceLine,
  previousLine?: SourceLine,
  onClick: (SourceLine, HTMLElement) => void
};
*/

export default class LineSCM extends React.PureComponent {
  /*:: props: Props; */

  handleClick = (e /*: SyntheticInputEvent */) => {
    e.preventDefault();
    this.props.onClick(this.props.line, e.target);
  };

  isSCMChanged(s /*: SourceLine */, p /*: ?SourceLine */) {
    let changed = true;
    if (p != null && s.scmAuthor != null && p.scmAuthor != null) {
      changed = s.scmAuthor !== p.scmAuthor || s.scmDate !== p.scmDate;
    }
    return changed;
  }

  render() {
    const { line, previousLine } = this.props;
    const clickable = !!line.line;
    return (
      <td
        className="source-meta source-line-scm"
        data-line-number={line.line}
        role={clickable ? 'button' : undefined}
        tabIndex={clickable ? 0 : undefined}
        onClick={clickable ? this.handleClick : undefined}>
        {this.isSCMChanged(line, previousLine) && (
          <div className="source-line-scm-inner" data-author={line.scmAuthor} />
        )}
      </td>
    );
  }
}
