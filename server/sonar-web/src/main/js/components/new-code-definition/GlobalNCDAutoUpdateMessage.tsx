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
import { Banner } from 'design-system';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { FormattedMessage } from 'react-intl';
import { MessageTypes, checkMessageDismissed, setMessageDismissed } from '../../api/messages';
import { getNewCodePeriod } from '../../api/newCodePeriod';
import { CurrentUserContextInterface } from '../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { NEW_CODE_PERIOD_CATEGORY } from '../../apps/settings/constants';
import { translate } from '../../helpers/l10n';
import { queryToSearch } from '../../helpers/urls';
import { hasGlobalPermission } from '../../helpers/users';
import { NewCodeDefinition, NewCodeDefinitionType } from '../../types/new-code-definition';
import { Permissions } from '../../types/permissions';
import { isLoggedIn } from '../../types/users';
import Link from '../common/Link';

interface Props extends Pick<CurrentUserContextInterface, 'currentUser'> {}

export function GlobalNCDAutoUpdateMessage(props: Props) {
  const { currentUser } = props;

  const [newCodeDefinition, setNewCodeDefinition] = useState<NewCodeDefinition | undefined>(
    undefined
  );
  const [dismissed, setDismissed] = useState(false);

  const isSystemAdmin = useMemo(
    () => isLoggedIn(currentUser) && hasGlobalPermission(currentUser, Permissions.Admin),
    [currentUser]
  );

  useEffect(() => {
    async function fetchNewCodeDefinition() {
      const newCodeDefinition = await getNewCodePeriod();
      if (
        newCodeDefinition?.previousNonCompliantValue &&
        newCodeDefinition?.type === NewCodeDefinitionType.NumberOfDays
      ) {
        setNewCodeDefinition(newCodeDefinition);
        const messageStatus = await checkMessageDismissed({
          messageType: MessageTypes.GlobalNcd90,
        });
        setDismissed(messageStatus.dismissed);
      }
    }

    if (isSystemAdmin) {
      fetchNewCodeDefinition();
    }
  }, [isSystemAdmin]);

  const handleBannerDismiss = useCallback(async () => {
    await setMessageDismissed({ messageType: MessageTypes.GlobalNcd90 });
    setDismissed(true);
  }, []);

  if (!isSystemAdmin || !newCodeDefinition || dismissed || !newCodeDefinition.updatedAt) {
    return null;
  }

  return (
    <Banner onDismiss={handleBannerDismiss} variant="info">
      <FormattedMessage
        defaultMessage="new_code_definition.auto_update.message"
        id="new_code_definition.auto_update.message"
        tagName="span"
        values={{
          previousDays: newCodeDefinition.previousNonCompliantValue,
          days: newCodeDefinition.value,
          date: new Date(newCodeDefinition.updatedAt).toLocaleDateString(),
          link: (
            <Link
              to={{
                pathname: '/admin/settings',
                search: queryToSearch({
                  category: NEW_CODE_PERIOD_CATEGORY,
                }),
              }}
            >
              {translate('new_code_definition.auto_update.review_link')}
            </Link>
          ),
        }}
      />
    </Banner>
  );
}

export default withCurrentUserContext(GlobalNCDAutoUpdateMessage);
