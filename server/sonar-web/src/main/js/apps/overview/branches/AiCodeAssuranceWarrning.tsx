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

import styled from '@emotion/styled';
import { ButtonIcon, ButtonVariety, IconX } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { themeBorder } from '~design-system';
import { MessageTypes } from '../../../api/messages';
import { translate } from '../../../helpers/l10n';
import {
  useMessageDismissedMutation,
  useMessageDismissedQuery,
} from '../../../queries/dismissed-messages';

export function AiCodeAssuranceWarrning({ projectKey }: Readonly<{ projectKey: string }>) {
  const messageType = MessageTypes.UnresolvedFindingsInAIGeneratedCode;
  const { data: isDismissed } = useMessageDismissedQuery(
    { messageType, projectKey },
    { select: (r) => r.dismissed },
  );

  const { mutate: dismiss } = useMessageDismissedMutation();

  if (isDismissed !== false) {
    return null;
  }

  return (
    <StyledSnowflakes className="sw-my-6">
      <StyleSnowflakesInner>
        <StyleSnowflakesContent>
          <FormattedMessage id="overview.ai_assurance.unsolved_overall.title" tagName="h3" />
          <FormattedMessage id="overview.ai_assurance.unsolved_overall.description" tagName="p" />
        </StyleSnowflakesContent>

        <ButtonIcon
          Icon={IconX}
          ariaLabel={translate('overview.ai_assurance.unsolved_overall.dismiss')}
          onClick={() => dismiss({ projectKey, messageType })}
          variety={ButtonVariety.DefaultGhost}
        />
      </StyleSnowflakesInner>
    </StyledSnowflakes>
  );
}

const StyleSnowflakesContent = styled.div`
  display: flex;
  padding: var(--echoes-dimension-space-100) 0px;

  flex-direction: column;
  align-items: flex-start;
  gap: var(--echoes-dimension-space-200);
  flex: 1 0 0;
`;

const StyleSnowflakesInner = styled.div`
  display: flex;
  padding: var(--echoes-dimension-space-100) 0px;

  align-items: flex-start;
  gap: var(--echoes-dimension-space-300);
  flex: 1 0 0;
`;

const StyledSnowflakes = styled.div`
  display: flex;
  justify-content: space-between;
  width: 641px;
  padding: 0 var(--echoes-dimension-space-100) 0 var(--echoes-dimension-space-300);

  align-items: flex-start;
  gap: 12px;

  border-radius: 8px;
  border: ${themeBorder('default', 'projectCardBorder')};

  background: var(--echoes-color-background-warning-weak);
`;
