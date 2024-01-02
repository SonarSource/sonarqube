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
import Toggler from '../../../components/controls/Toggler';
import { translateWithParameters } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';
import { ButtonPlain } from '../../controls/buttons';
import LineOptionsPopup from './LineOptionsPopup';

export interface LineNumberProps {
  displayOptions: boolean;
  firstLineNumber: number;
  line: SourceLine;
}

export function LineNumber({ displayOptions, firstLineNumber, line }: LineNumberProps) {
  const [isOpen, setOpen] = React.useState<boolean>(false);
  const { line: lineNumber } = line;
  const hasLineNumber = !!lineNumber;

  return hasLineNumber ? (
    <td className="source-meta source-line-number" data-line-number={lineNumber}>
      {displayOptions ? (
        <Toggler
          closeOnClickOutside={true}
          onRequestClose={() => setOpen(false)}
          open={isOpen}
          overlay={<LineOptionsPopup firstLineNumber={firstLineNumber} line={line} />}
        >
          <ButtonPlain
            aria-expanded={isOpen}
            aria-haspopup={true}
            aria-label={translateWithParameters('source_viewer.line_X', lineNumber)}
            onClick={() => setOpen(true)}
          >
            {lineNumber}
          </ButtonPlain>
        </Toggler>
      ) : (
        lineNumber
      )}
    </td>
  ) : (
    <td className="source-meta source-line-number" />
  );
}

export default React.memo(LineNumber);
