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
import SCMPopup from './SCMPopup';
import Toggler from '../../controls/Toggler';

interface Props {
  line: T.SourceLine;
  onPopupToggle: (x: { index?: number; line: number; name: string; open?: boolean }) => void;
  popupOpen: boolean;
  previousLine: T.SourceLine | undefined;
}

export default class LineSCM extends React.PureComponent<Props> {
  handleClick = (event: React.MouseEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    this.props.onPopupToggle({ line: this.props.line.line, name: 'scm' });
  };

  handleTogglePopup = (open: boolean) => {
    this.props.onPopupToggle({ line: this.props.line.line, name: 'scm', open });
  };

  closePopup = () => {
    this.handleTogglePopup(false);
  };

  render() {
    const { line, popupOpen, previousLine } = this.props;
    const hasPopup = !!line.line;
    const cell = isSCMChanged(line, previousLine) && (
      <div className="source-line-scm-inner" data-author={line.scmAuthor || '…'} />
    );
    return hasPopup ? (
      <td
        className="source-meta source-line-scm"
        data-line-number={line.line}
        onClick={this.handleClick}
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role
        role="button"
        tabIndex={0}>
        <Toggler
          onRequestClose={this.closePopup}
          open={popupOpen}
          overlay={<SCMPopup line={line} />}>
          {cell}
        </Toggler>
      </td>
    ) : (
      <td className="source-meta source-line-scm" data-line-number={line.line}>
        {cell}
      </td>
    );
  }
}

function isSCMChanged(s: T.SourceLine, p: T.SourceLine | undefined) {
  let changed = true;
  if (p != null && s.scmRevision != null && p.scmRevision != null) {
    changed = s.scmRevision !== p.scmRevision || s.scmDate !== p.scmDate;
  }
  return changed;
}
