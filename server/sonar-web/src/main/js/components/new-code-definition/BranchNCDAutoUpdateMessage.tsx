/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { DismissableFlagMessage, Link } from 'design-system';
import React, { useCallback, useEffect, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { MessageTypes, checkMessageDismissed, setMessageDismissed } from '../../api/messages';
import { useDocUrl } from '../../helpers/docs';
import { Component } from '../../types/types';
import { PreviouslyNonCompliantBranchNCD } from './utils';

interface NCDAutoUpdateMessageProps {
  component: Component;
  previouslyNonCompliantBranchNCDs: PreviouslyNonCompliantBranchNCD[];
}

export default function NCDAutoUpdateMessage(props: NCDAutoUpdateMessageProps) {
  const { component, previouslyNonCompliantBranchNCDs } = props;
  const intl = useIntl();
  const toUrl = useDocUrl(
    '/project-administration/clean-as-you-code-settings/defining-new-code/#new-code-definition-options',
  );

  const [dismissed, setDismissed] = useState(true);

  const handleBannerDismiss = useCallback(async () => {
    await setMessageDismissed({ messageType: MessageTypes.BranchNcd90, projectKey: component.key });
    setDismissed(true);
  }, [component]);

  useEffect(() => {
    async function checkBranchMessageDismissed() {
      if (previouslyNonCompliantBranchNCDs.length > 0) {
        const messageStatus = await checkMessageDismissed({
          messageType: MessageTypes.BranchNcd90,
          projectKey: component.key,
        });
        setDismissed(messageStatus.dismissed);
      }
    }

    if (previouslyNonCompliantBranchNCDs.length > 0) {
      checkBranchMessageDismissed();
    }
  }, [component, previouslyNonCompliantBranchNCDs]);

  if (dismissed || previouslyNonCompliantBranchNCDs.length === 0) {
    return null;
  }

  const branchesList = (
    <ul className="sw-list-disc sw-my-4 sw-list-inside">
      {previouslyNonCompliantBranchNCDs.map((branchNCD) => (
        <li key={branchNCD.branchKey}>
          <FormattedMessage
            id="new_code_definition.auto_update.branch.list_item"
            values={{
              branchName: branchNCD.branchKey,
              days: branchNCD.value,
              previousDays: branchNCD.previousNonCompliantValue,
            }}
          />
        </li>
      ))}
    </ul>
  );

  return (
    <DismissableFlagMessage className="sw-my-4" onDismiss={handleBannerDismiss} variant="info">
      <div>
        <FormattedMessage
          id="new_code_definition.auto_update.branch.message"
          values={{
            date: new Date(previouslyNonCompliantBranchNCDs[0].updatedAt).toLocaleDateString(),
            branchesList,
            link: <Link to={toUrl}>{intl.formatMessage({ id: 'learn_more' })}</Link>,
          }}
        />
      </div>
    </DismissableFlagMessage>
  );
}
