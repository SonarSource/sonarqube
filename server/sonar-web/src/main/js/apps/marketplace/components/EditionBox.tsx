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
import CheckIcon from '../../../components/icons-components/CheckIcon';
import { translate } from '../../../helpers/l10n';
import DocInclude from '../../../components/docs/DocInclude';
import { Edition, getEditionUrl } from '../utils';

interface Props {
  currentEdition: string;
  edition: Edition;
  ncloc?: number;
  serverId?: string;
}

export default function EditionBox({ currentEdition, edition, ncloc, serverId }: Props) {
  const isInstalled = currentEdition === edition.key;
  const url = getEditionUrl(edition, { ncloc, serverId, sourceEdition: currentEdition });
  return (
    <div className="boxed-group boxed-group-inner marketplace-edition">
      {isInstalled && (
        <span className="marketplace-edition-badge badge badge-normal-size display-flex-center">
          <CheckIcon className="little-spacer-right" size={14} />
          {translate('marketplace.installed')}
        </span>
      )}
      <div>
        <DocInclude path={'/tooltips/editions/' + edition.key} />
      </div>
      <div className="marketplace-edition-action spacer-top">
        <a href={url} target="_blank">
          {translate('marketplace.learn_more')}
        </a>
      </div>
    </div>
  );
}
