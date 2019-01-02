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
import LineOptionsPopup from './LineOptionsPopup';
import Toggler from '../../controls/Toggler';

interface Props {
  line: T.SourceLine;
  onPopupToggle: (x: { index?: number; line: number; name: string; open?: boolean }) => void;
  popupOpen: boolean;
}

export default class LineNumber extends React.PureComponent<Props> {
  handleClick = (event: React.MouseEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    this.props.onPopupToggle({ line: this.props.line.line, name: 'line-number' });
  };

  handleTogglePopup = (open: boolean) => {
    this.props.onPopupToggle({ line: this.props.line.line, name: 'line-number', open });
  };

  closePopup = () => {
    this.handleTogglePopup(false);
  };

  render() {
    const { line, popupOpen } = this.props;
    const { line: lineNumber } = line;
    const hasLineNumber = !!lineNumber;
    return hasLineNumber ? (
      <td
        className="source-meta source-line-number"
        data-line-number={lineNumber}
        onClick={this.handleClick}
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role
        role="button"
        tabIndex={0}>
        <Toggler
          onRequestClose={this.closePopup}
          open={popupOpen}
          overlay={<LineOptionsPopup line={line} />}
        />
      </td>
    ) : (
      <td className="source-meta source-line-number" />
    );
  }
}
