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
import classNames from 'classnames';
import { translate } from '../../../helpers/l10n';
import type { SourceLine } from '../types';

type Props = {
  duplicated: boolean,
  index: number,
  line: SourceLine,
  onClick: (index: number, lineNumber: number) => void
};

export default class LineDuplicationBlock extends React.PureComponent {
  props: Props;

  handleClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    this.props.onClick(this.props.index, this.props.line.line);
  };

  render() {
    const { duplicated, index, line } = this.props;
    const className = classNames('source-meta', 'source-line-duplications-extra', {
      'source-line-duplicated': duplicated
    });

    return (
      <td
        key={index}
        className={className}
        data-line-number={line.line}
        data-index={index}
        title={duplicated ? translate('source_viewer.tooltip.duplicated_block') : undefined}
        data-placement={duplicated ? 'right' : undefined}
        data-toggle={duplicated ? 'tooltip' : undefined}
        role={duplicated ? 'button' : undefined}
        tabIndex={duplicated ? '0' : undefined}
        onClick={duplicated ? this.handleClick : undefined}>
        <div className="source-line-bar" />
      </td>
    );
  }
}
