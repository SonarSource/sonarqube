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

import React from 'react';
import { Link, Note } from '~design-system';
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { getFormattingHelpUrl } from '../../helpers/urls';

export interface FormattingTipsProps {
  className?: string;
}

export default function FormattingTips({ className }: FormattingTipsProps) {
  const handleClick = React.useCallback((evt: React.MouseEvent<HTMLAnchorElement>) => {
    evt.preventDefault();
    window.open(
      `${getBaseUrl()}${getFormattingHelpUrl()}`,
      'Formatting',
      'height=300,width=600,scrollbars=1,resizable=1',
    );
  }, []);

  return (
    <Note className={className}>
      <Link className="sw-mr-1" onClick={handleClick} to={getFormattingHelpUrl()}>
        {translate('formatting.helplink')}
      </Link>
      {':'}
      <span className="sw-ml-2">*{translate('bold')}*</span>
      <span className="sw-ml-2">
        ``
        {translate('code')}
        ``
      </span>
      <span className="sw-ml-2">* {translate('bulleted_point')}</span>
    </Note>
  );
}
