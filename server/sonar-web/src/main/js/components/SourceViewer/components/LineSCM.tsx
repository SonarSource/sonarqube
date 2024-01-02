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
import Dropdown from '../../../components/controls/Dropdown';
import { PopupPlacement } from '../../../components/ui/popups';
import { translateWithParameters } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';
import { ButtonPlain } from '../../controls/buttons';
import SCMPopup from './SCMPopup';

export interface LineSCMProps {
  line: SourceLine;
  previousLine: SourceLine | undefined;
}

export function LineSCM({ line, previousLine }: LineSCMProps) {
  const hasPopup = !!line.line;
  const cell = (
    <div className="source-line-scm-inner">
      {isSCMChanged(line, previousLine) ? line.scmAuthor || '…' : ' '}
    </div>
  );

  if (hasPopup) {
    let ariaLabel = translateWithParameters('source_viewer.click_for_scm_info', line.line);
    if (line.scmAuthor) {
      ariaLabel = `${translateWithParameters(
        'source_viewer.author_X',
        line.scmAuthor
      )}, ${ariaLabel}`;
    }

    return (
      <td className="source-meta source-line-scm" data-line-number={line.line}>
        <Dropdown overlay={<SCMPopup line={line} />} overlayPlacement={PopupPlacement.RightTop}>
          <ButtonPlain aria-label={ariaLabel}>{cell}</ButtonPlain>
        </Dropdown>
      </td>
    );
  }
  return (
    <td className="source-meta source-line-scm" data-line-number={line.line}>
      {cell}
    </td>
  );
}

function isSCMChanged(s: SourceLine, p: SourceLine | undefined) {
  let changed = true;
  if (p != null && s.scmRevision != null && p.scmRevision != null) {
    changed = s.scmRevision !== p.scmRevision || s.scmDate !== p.scmDate;
  }
  return changed;
}

export default React.memo(LineSCM);
