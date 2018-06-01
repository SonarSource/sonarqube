/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Edition, getEditionUrl, EditionKey } from '../utils';
import DocInclude from '../../../components/docs/DocInclude';
import { translate } from '../../../helpers/l10n';

interface Props {
  currentEdition?: EditionKey;
  edition: Edition;
  ncloc?: number;
  serverId?: string;
}

export default function EditionBox({ edition, ncloc, serverId, currentEdition }: Props) {
  return (
    <div className="boxed-group boxed-group-inner marketplace-edition">
      <DocInclude path={'/tooltips/editions/' + edition.key} />
      <div className="marketplace-edition-action spacer-top">
        <a
          href={getEditionUrl(edition, { ncloc, serverId, sourceEdition: currentEdition })}
          rel="noopener noreferrer"
          target="_blank">
          {translate('marketplace.ask_for_information')}
        </a>
      </div>
    </div>
  );
}
