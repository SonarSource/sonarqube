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
import CoveragePopup from './CoveragePopup';
import { SourceLine } from '../../../app/types';
import Tooltip from '../../controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import BubblePopupHelper from '../../common/BubblePopupHelper';

interface Props {
  branch: string | undefined;
  componentKey: string;
  line: SourceLine;
  onPopupToggle: (x: { index?: number; line: number; name: string; open?: boolean }) => void;
  popupOpen: boolean;
}

export default class LineCoverage extends React.PureComponent<Props> {
  handleClick = (event: React.MouseEvent<HTMLElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    this.props.onPopupToggle({ line: this.props.line.line, name: 'coverage' });
  };

  handleTogglePopup = (open: boolean) => {
    this.props.onPopupToggle({ line: this.props.line.line, name: 'coverage', open });
  };

  closePopup = () => {
    this.props.onPopupToggle({ line: this.props.line.line, name: 'coverage', open: false });
  };

  render() {
    const { branch, componentKey, line, popupOpen } = this.props;

    const className =
      'source-meta source-line-coverage' +
      (line.coverageStatus != null ? ` source-line-${line.coverageStatus}` : '');

    const hasPopup =
      line.coverageStatus === 'covered' || line.coverageStatus === 'partially-covered';

    const cell = line.coverageStatus ? (
      <Tooltip overlay={translate('source_viewer.tooltip', line.coverageStatus)} placement="right">
        <div className="source-line-bar" />
      </Tooltip>
    ) : (
      <div className="source-line-bar" />
    );

    if (hasPopup) {
      return (
        <td
          className={className}
          data-line-number={line.line}
          onClick={this.handleClick}
          // eslint-disable-next-line jsx-a11y/no-noninteractive-element-to-interactive-role
          role="button"
          tabIndex={0}>
          {cell}
          <BubblePopupHelper
            isOpen={popupOpen}
            popup={
              <CoveragePopup
                branch={branch}
                componentKey={componentKey}
                line={line}
                onClose={this.closePopup}
              />
            }
            position="bottomright"
            togglePopup={this.handleTogglePopup}
          />
        </td>
      );
    }

    return (
      <td className={className} data-line-number={line.line}>
        {cell}
      </td>
    );
  }
}
