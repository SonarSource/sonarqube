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

import { memo } from 'react';
import { DropdownMenu, ItemCopy } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';
import { getLineCodeAsPlainText } from '../helpers/lines';

interface Props {
  line: SourceLine;
  permalink: string;
}

export function LineOptionsPopup({ line, permalink }: Props) {
  const lineCodeAsPlainText = getLineCodeAsPlainText(line.code);
  return (
    <DropdownMenu>
      <ItemCopy
        copyValue={permalink}
        tooltipOverlay={translate('source_viewer.copied_to_clipboard')}
      >
        {translate('source_viewer.copy_permalink')}
      </ItemCopy>

      {lineCodeAsPlainText && (
        <ItemCopy
          copyValue={lineCodeAsPlainText}
          tooltipOverlay={translate('source_viewer.copied_to_clipboard')}
        >
          {translate('source_viewer.copy_line')}
        </ItemCopy>
      )}
    </DropdownMenu>
  );
}

export default memo(LineOptionsPopup);
