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
import {
  LineMeta,
  LineSCMStyled,
  LineSCMStyledDiv,
  OutsideClickHandler,
  PopupPlacement,
} from 'design-system';
import React, { memo, useCallback, useState } from 'react';
import { translateWithParameters } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';
import Tooltip from '../../controls/Tooltip';
import SCMPopup from './SCMPopup';

interface Props {
  line: SourceLine;
  previousLine: SourceLine | undefined;
}

function LineSCM({ line, previousLine }: Props) {
  const [isOpen, setIsOpen] = useState(false);

  const handleToggle = useCallback(() => {
    setIsOpen(!isOpen);
  }, [isOpen]);
  const handleClose = useCallback(() => {
    setIsOpen(false);
  }, []);

  const isFileIssue = !line.line;
  if (isFileIssue) {
    return (
      <LineMeta>
        <LineSCMStyledDiv>{line.scmAuthor ?? ' '}</LineSCMStyledDiv>
      </LineMeta>
    );
  }

  let ariaLabel = translateWithParameters('source_viewer.click_for_scm_info', line.line);
  if (line.scmAuthor) {
    ariaLabel = `${translateWithParameters(
      'source_viewer.author_X',
      line.scmAuthor,
    )}, ${ariaLabel}`;
  }

  return (
    <LineMeta data-line-number={line.line}>
      <OutsideClickHandler onClickOutside={handleClose}>
        <Tooltip
          content={<SCMPopup line={line} />}
          side={PopupPlacement.Right}
          isOpen={isOpen}
          isInteractive
          classNameInner="sw-max-w-abs-600"
        >
          <LineSCMStyled aria-label={ariaLabel} onClick={handleToggle} role="button">
            {isSCMChanged(line, previousLine) ? line.scmAuthor ?? '…' : ' '}
          </LineSCMStyled>
        </Tooltip>
      </OutsideClickHandler>
    </LineMeta>
  );
}

function isSCMChanged(s: SourceLine, p: SourceLine | undefined) {
  let changed = true;
  if (p != null && s.scmRevision != null && p.scmRevision != null) {
    changed = s.scmRevision !== p.scmRevision || s.scmDate !== p.scmDate;
  }
  return changed;
}

export default memo(LineSCM);
