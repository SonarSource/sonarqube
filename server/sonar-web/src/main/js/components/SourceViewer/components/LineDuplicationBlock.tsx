/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import classNames from 'classnames';
import * as React from 'react';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import Toggler from '../../../components/controls/Toggler';
import Tooltip from '../../../components/controls/Tooltip';
import { PopupPlacement } from '../../../components/ui/popups';
import { translate } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';
import { ButtonPlain } from '../../controls/buttons';

export interface LineDuplicationBlockProps {
  blocksLoaded: boolean;
  duplicated: boolean;
  index: number;
  line: SourceLine;
  onClick?: (line: SourceLine) => void;
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
}

export function LineDuplicationBlock(props: LineDuplicationBlockProps) {
  const { blocksLoaded, duplicated, index, line } = props;
  const [dropdownOpen, setDropdownOpen] = React.useState(false);

  const className = classNames('source-meta', 'source-line-duplications', {
    'source-line-duplicated': duplicated,
  });

  const tooltip = dropdownOpen ? undefined : translate('source_viewer.tooltip.duplicated_block');

  return duplicated ? (
    <td className={className} data-index={index} data-line-number={line.line}>
      <Tooltip overlay={tooltip} placement="right" accessible={false}>
        <div>
          <Toggler
            onRequestClose={() => setDropdownOpen(false)}
            open={dropdownOpen}
            overlay={
              <DropdownOverlay placement={PopupPlacement.RightTop}>
                {props.renderDuplicationPopup(index, line.line)}
              </DropdownOverlay>
            }
          >
            <ButtonPlain
              aria-label={translate('source_viewer.tooltip.duplicated_block')}
              className="source-line-bar"
              onClick={() => {
                setDropdownOpen(true);
                if (!blocksLoaded && line.duplicated && props.onClick) {
                  props.onClick(line);
                }
              }}
            />
          </Toggler>
        </div>
      </Tooltip>
    </td>
  ) : (
    <td className={className} data-index={index} data-line-number={line.line}>
      <div className="source-line-bar" />
    </td>
  );
}

export default React.memo(LineDuplicationBlock);
