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
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import { PopupPlacement } from '../../../components/ui/popups';
import { translate } from '../../../helpers/l10n';
import { getCodeUrl, getPathUrlAsString } from '../../../helpers/urls';
import { SourceLine } from '../../../types/types';
import { ClipboardButton } from '../../controls/clipboard';
import { SourceViewerContext } from '../SourceViewerContext';

export interface LineOptionsPopupProps {
  firstLineNumber: number;
  line: SourceLine;
}

export function LineOptionsPopup({ firstLineNumber, line }: LineOptionsPopupProps) {
  return (
    <SourceViewerContext.Consumer>
      {({ branchLike, file }) => {
        const codeLocation = getCodeUrl(file.project, branchLike, file.key, line.line);
        const codeUrl = getPathUrlAsString(codeLocation, false);
        const isAtTop = line.line - 4 < firstLineNumber;
        return (
          <DropdownOverlay
            className="big-spacer-left"
            noPadding={true}
            placement={isAtTop ? PopupPlacement.BottomLeft : PopupPlacement.TopLeft}
          >
            <div className="padded source-viewer-bubble-popup nowrap">
              <ClipboardButton
                className="button-link"
                copyValue={codeUrl}
                aria-label={translate('component_viewer.copy_permalink')}
              >
                {translate('component_viewer.copy_permalink')}
              </ClipboardButton>
            </div>
          </DropdownOverlay>
        );
      }}
    </SourceViewerContext.Consumer>
  );
}

export default React.memo(LineOptionsPopup);
