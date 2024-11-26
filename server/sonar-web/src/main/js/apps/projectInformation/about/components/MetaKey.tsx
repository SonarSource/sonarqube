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

import {
  Button,
  ButtonVariety,
  Heading,
  IconQuestionMark,
  Popover,
} from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import { ClipboardIconButton, CodeSnippet } from '~design-system';
import { translate } from '../../../../helpers/l10n';

interface MetaKeyProps {
  componentKey: string;
  qualifier: string;
}

export default function MetaKey({ componentKey, qualifier }: MetaKeyProps) {
  const intl = useIntl();
  return (
    <>
      <div className="sw-flex sw-items-baseline">
        <Heading as="h3">{translate('overview.project_key', qualifier)}</Heading>
        <Popover
          title={intl.formatMessage(
            { id: 'about_x' },
            { x: translate('overview.project_key', qualifier) },
          )}
          description={translate('overview.project_key.tooltip', qualifier)}
        >
          <Button
            className="sw-ml-1 sw-p-0 sw-h-fit sw-min-h-fit"
            aria-label={intl.formatMessage({ id: 'help' })}
            variety={ButtonVariety.DefaultGhost}
          >
            <IconQuestionMark color="echoes-color-icon-subdued" />
          </Button>
        </Popover>
      </div>
      <div className="sw-mt-2 sw-w-full sw-flex sw-gap-2 sw-items-center sw-break-words sw-min-w-0">
        <CodeSnippet
          className="sw-min-w-0 sw-px-1 sw-max-w-10/12"
          noCopy
          isOneLine
          snippet={componentKey}
        />
        <ClipboardIconButton copyValue={componentKey} />
      </div>
    </>
  );
}
