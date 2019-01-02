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
import * as classNames from 'classnames';
import { DropdownOverlay } from '../../controls/Dropdown';
import DateFormatter from '../../intl/DateFormatter';
import { PopupPlacement } from '../../ui/popups';

interface Props {
  line: T.SourceLine;
}

export default function SCMPopup({ line }: Props) {
  const hasAuthor = line.scmAuthor !== '';
  return (
    <DropdownOverlay placement={PopupPlacement.RightTop}>
      <div className="source-viewer-bubble-popup abs-width-400">
        {hasAuthor && <div>{line.scmAuthor}</div>}
        {line.scmDate && (
          <div className={classNames({ 'spacer-top': hasAuthor })}>
            <DateFormatter date={line.scmDate} />
          </div>
        )}
        {line.scmRevision && <div className="spacer-top">{line.scmRevision}</div>}
      </div>
    </DropdownOverlay>
  );
}
