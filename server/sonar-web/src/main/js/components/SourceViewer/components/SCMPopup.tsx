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
import { translate } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';
import DateFormatter from '../../intl/DateFormatter';

export interface SCMPopupProps {
  line: SourceLine;
}

export function SCMPopup({ line }: SCMPopupProps) {
  const hasAuthor = line.scmAuthor !== undefined && line.scmAuthor !== '';
  const hasDate = line.scmDate !== undefined;
  return (
    <div className="source-viewer-bubble-popup abs-width-400">
      {hasAuthor && (
        <div>
          <h4>{translate('author')}</h4>
          {line.scmAuthor}
        </div>
      )}
      {hasDate && (
        <div className={classNames({ 'spacer-top': hasAuthor })}>
          <h4>{translate('source_viewer.tooltip.scm.commited_on')}</h4>
          <DateFormatter date={line.scmDate!} />
        </div>
      )}
      {line.scmRevision && (
        <div className={classNames({ 'spacer-top': hasAuthor || hasDate })}>
          <h4>{translate('source_viewer.tooltip.scm.revision')}</h4>
          {line.scmRevision}
        </div>
      )}
    </div>
  );
}

export default React.memo(SCMPopup);
