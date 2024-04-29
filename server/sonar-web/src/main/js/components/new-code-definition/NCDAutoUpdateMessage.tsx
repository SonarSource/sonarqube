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
import { Banner, Link } from 'design-system';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { queryToSearchString } from '~sonar-aligned/helpers/urls';
import { MessageTypes, checkMessageDismissed, setMessageDismissed } from '../../api/messages';
import { CurrentUserContextInterface } from '../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { NEW_CODE_PERIOD_CATEGORY } from '../../apps/settings/constants';
import { useNewCodeDefinitionQuery } from '../../queries/newCodeDefinition';
import { Component } from '../../types/types';
import {
  PreviouslyNonCompliantNCD,
  isGlobalOrProjectAdmin,
  isPreviouslyNonCompliantDaysNCD,
} from './utils';

interface NCDAutoUpdateMessageProps extends Pick<CurrentUserContextInterface, 'currentUser'> {
  branchName?: string;
  component?: Component;
}

function NCDAutoUpdateMessage(props: Readonly<NCDAutoUpdateMessageProps>) {
  const { branchName, component, currentUser } = props;
  const isGlobalBanner = component === undefined;
  const intl = useIntl();

  const [dismissed, setDismissed] = useState(true);
  const [previouslyNonCompliantNewCodeDefinition, setPreviouslyNonCompliantNewCodeDefinition] =
    useState<PreviouslyNonCompliantNCD | undefined>(undefined);

  const isAdmin = isGlobalOrProjectAdmin(currentUser, component);

  const { data: newCodeDefinition } = useNewCodeDefinitionQuery({
    branchName,
    enabled: isAdmin,
    projectKey: component?.key,
  });

  const ncdReviewLinkTo = useMemo(
    () =>
      isGlobalBanner
        ? {
            pathname: '/admin/settings',
            search: queryToSearchString({
              category: NEW_CODE_PERIOD_CATEGORY,
            }),
          }
        : {
            pathname: '/project/baseline',
            search: queryToSearchString({
              id: component.key,
            }),
          },
    [component, isGlobalBanner],
  );

  const handleBannerDismiss = useCallback(async () => {
    await setMessageDismissed(
      isGlobalBanner
        ? { messageType: MessageTypes.GlobalNcd90 }
        : { messageType: MessageTypes.ProjectNcd90, projectKey: component.key },
    );
    setDismissed(true);
  }, [component, isGlobalBanner]);

  useEffect(() => {
    async function updateMessageStatus() {
      const messageStatus = await checkMessageDismissed(
        isGlobalBanner
          ? {
              messageType: MessageTypes.GlobalNcd90,
            }
          : {
              messageType: MessageTypes.ProjectNcd90,
              projectKey: component.key,
            },
      );

      setDismissed(messageStatus.dismissed);
    }

    if (newCodeDefinition && isPreviouslyNonCompliantDaysNCD(newCodeDefinition)) {
      setPreviouslyNonCompliantNewCodeDefinition(newCodeDefinition);
      updateMessageStatus();
    } else {
      setPreviouslyNonCompliantNewCodeDefinition(undefined);
    }
  }, [component?.key, isGlobalBanner, newCodeDefinition]);

  if (dismissed || !previouslyNonCompliantNewCodeDefinition) {
    return null;
  }

  const { updatedAt, previousNonCompliantValue, value } = previouslyNonCompliantNewCodeDefinition;
  const bannerMessageId = isGlobalBanner
    ? 'new_code_definition.auto_update.global.message'
    : 'new_code_definition.auto_update.project.message';

  return (
    <Banner onDismiss={handleBannerDismiss} variant="info">
      <p>
        <FormattedMessage
          id={bannerMessageId}
          values={{
            date: new Date(updatedAt).toLocaleDateString(),
            days: value,
            link: (
              <Link to={ncdReviewLinkTo}>
                {intl.formatMessage({ id: 'new_code_definition.auto_update.review_link' })}
              </Link>
            ),
            previousDays: previousNonCompliantValue,
          }}
        />
      </p>
    </Banner>
  );
}

export default withCurrentUserContext(NCDAutoUpdateMessage);
