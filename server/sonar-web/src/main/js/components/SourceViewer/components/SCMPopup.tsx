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
import { SCMHighlight } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { SourceLine } from '../../../types/types';
import DateFormatter from '../../intl/DateFormatter';

interface Props {
  line: SourceLine;
}

export function SCMPopup({ line }: Props) {
  const hasAuthor = line.scmAuthor !== undefined && line.scmAuthor !== '';
  const hasDate = line.scmDate !== undefined;
  return (
    <div className="sw-select-text sw-text-left">
      {hasAuthor && (
        <div className="sw-flex sw-items-center">
          <SCMHighlight>{translate('author')}</SCMHighlight>
          <div className="sw-whitespace-nowrap sw-mr-2">{line.scmAuthor}</div>
        </div>
      )}
      {hasDate && (
        <div className="sw-flex sw-items-center">
          <SCMHighlight>{translate('source_viewer.tooltip.scm.commited_on')}</SCMHighlight>
          <div className="sw-whitespace-nowrap sw-mr-2">
            <DateFormatter date={line.scmDate!} />
          </div>
        </div>
      )}
      {line.scmRevision && (
        <div className="sw-flex sw-items-center">
          <SCMHighlight>{translate('source_viewer.tooltip.scm.revision')}</SCMHighlight>
          <div className="sw-whitespace-nowrap sw-mr-2">{line.scmRevision}</div>
        </div>
      )}
    </div>
  );
}

export default memo(SCMPopup);
