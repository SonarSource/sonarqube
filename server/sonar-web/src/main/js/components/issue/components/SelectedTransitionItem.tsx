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

import { IconCheck } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { noop } from 'lodash';
import { ItemButton, Note } from '../../../design-system';
import { translate } from '../../../helpers/l10n';
import DocHelpTooltip from '../../../sonar-aligned/components/controls/DocHelpTooltip';
import { IssueTransition } from '../../../types/issues';
import { isTransitionDeprecated } from '../helpers';

interface Props {
  transition: IssueTransition;
}

export function SelectedTransitionItem({ transition }: Readonly<Props>) {
  return (
    <ItemButton className={classNames('sw-px-3 selected')} key={transition} onClick={noop}>
      <IconCheck className="sw-mr-2" />
      <div className="sw-flex">
        <div className="sw-flex sw-flex-col">
          <div className="sw-font-semibold sw-flex sw-gap-1 sw-items-center">
            {translate('issue.transition', transition)}
            {isTransitionDeprecated(transition) && (
              <DocHelpTooltip
                className="sw-ml-1"
                content={translate('issue.transition', transition, 'deprecated_tooltip')}
              />
            )}
          </div>
          <Note className="sw-whitespace-break-spaces">
            {translate('issue.transition', transition, 'description')}
          </Note>
        </div>
      </div>
    </ItemButton>
  );
}
