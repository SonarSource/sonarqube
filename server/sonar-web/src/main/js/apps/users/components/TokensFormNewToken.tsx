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

import { ClipboardIconButton, CodeSnippet, FlagMessage } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  token: { name: string; token: string };
}

export default function TokensFormNewToken({ token }: Readonly<Props>) {
  return (
    <div className="sw-mt-4">
      <FlagMessage variant="success">
        {translateWithParameters('users.tokens.new_token_created', token.name)}
      </FlagMessage>

      <div aria-label={translate('users.new_token')} className="sw-flex sw-items-center sw-mt-3">
        <CodeSnippet className="sw-p-1" isOneLine noCopy snippet={token.token} />

        <ClipboardIconButton className="sw-ml-4" copyValue={token.token} />
      </div>
    </div>
  );
}
