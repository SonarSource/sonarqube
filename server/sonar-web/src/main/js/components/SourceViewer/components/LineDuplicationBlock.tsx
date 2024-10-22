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

import * as React from 'react';
import { DuplicationBlock, LineMeta, OutsideClickHandler, PopupPlacement } from '~design-system';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';

export interface LineDuplicationBlockProps {
  blocksLoaded: boolean;
  duplicated: boolean;
  index: number;
  line: SourceLine;
  onClick?: (line: SourceLine) => void;
  renderDuplicationPopup: (index: number, line: number) => React.ReactNode;
}

export function LineDuplicationBlock(props: LineDuplicationBlockProps) {
  const { blocksLoaded, duplicated, index, line, onClick } = props;
  const [popupOpen, setPopupOpen] = React.useState(false);

  const tooltip = popupOpen ? undefined : translate('source_viewer.tooltip.duplicated_block');

  const handleClick = React.useCallback(() => {
    setPopupOpen(!popupOpen);
    if (!blocksLoaded && line.duplicated && onClick) {
      onClick(line);
    }
  }, [blocksLoaded, line, onClick, popupOpen]);

  const handleClose = React.useCallback(() => setPopupOpen(false), []);

  return duplicated ? (
    <Tooltip content={tooltip} side={PopupPlacement.Right}>
      <LineMeta
        className="it__source-line-duplicated"
        data-index={index}
        data-line-number={line.line}
      >
        <OutsideClickHandler onClickOutside={handleClose}>
          <Tooltip
            side={PopupPlacement.Right}
            isOpen={popupOpen}
            isInteractive
            content={popupOpen ? props.renderDuplicationPopup(index, line.line) : undefined}
            classNameInner="sw-max-w-abs-400"
          >
            <DuplicationBlock
              aria-label={translate('source_viewer.tooltip.duplicated_block')}
              aria-expanded={popupOpen}
              aria-haspopup="dialog"
              onClick={handleClick}
              role="button"
              tabIndex={0}
            />
          </Tooltip>
        </OutsideClickHandler>
      </LineMeta>
    </Tooltip>
  ) : (
    <LineMeta data-index={index} data-line-number={line.line} />
  );
}

export default React.memo(LineDuplicationBlock);
