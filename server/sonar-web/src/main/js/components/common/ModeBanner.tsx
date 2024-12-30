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
import {
  ButtonIcon,
  ButtonSize,
  ButtonVariety,
  IconX,
  Link,
  LinkHighlight,
} from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import { dismissNotice } from '../../api/users';
import { useCurrentUser } from '../../app/components/current-user/CurrentUserContext';
import { Banner } from '../../design-system';
import { useModeModifiedQuery, useStandardExperienceModeQuery } from '../../queries/mode';
import { Permissions } from '../../types/permissions';
import { NoticeType } from '../../types/users';

interface Props {
  as: 'facetBanner' | 'wideBanner';
}

export default function ModeBanner({ as }: Props) {
  const intl = useIntl();
  const { currentUser, updateDismissedNotices } = useCurrentUser();
  const { data: isStandardMode } = useStandardExperienceModeQuery();
  const { data: isModified, isLoading } = useModeModifiedQuery();

  const onDismiss = () => {
    dismissNotice(NoticeType.MQR_MODE_ADVERTISEMENT_BANNER)
      .then(() => {
        updateDismissedNotices(NoticeType.MQR_MODE_ADVERTISEMENT_BANNER, true);
      })
      .catch(() => {
        /* noop */
      });
  };

  if (
    !currentUser.permissions?.global.includes(Permissions.Admin) ||
    currentUser.dismissedNotices[NoticeType.MQR_MODE_ADVERTISEMENT_BANNER] ||
    isLoading ||
    isModified
  ) {
    return null;
  }

  return as === 'wideBanner' ? (
    <Banner className="sw-mt-8" variant="info" onDismiss={onDismiss}>
      <div>
        {intl.formatMessage(
          { id: `settings.mode.${isStandardMode ? 'standard' : 'mqr'}.advertisement` },
          {
            a: (text) => (
              <Link highlight={LinkHighlight.CurrentColor} to="/admin/settings?category=mode">
                {text}
              </Link>
            ),
          },
        )}
      </div>
    </Banner>
  ) : (
    <FacetBanner>
      <div className="sw-flex sw-gap-2">
        <div>
          {intl.formatMessage(
            { id: `mode.${isStandardMode ? 'standard' : 'mqr'}.advertisement` },
            {
              a: (text) => (
                <Link highlight={LinkHighlight.CurrentColor} to="/admin/settings?category=mode">
                  {text}
                </Link>
              ),
            },
          )}
        </div>
        <ButtonIcon
          className="sw-flex-none"
          Icon={IconX}
          ariaLabel={intl.formatMessage({ id: 'dismiss' })}
          onClick={onDismiss}
          size={ButtonSize.Medium}
          variety={ButtonVariety.DefaultGhost}
        />
      </div>
    </FacetBanner>
  );
}

const FacetBanner = styled.div`
  ${tw`sw-p-2 sw-rounded-2`}
  background-color: var(--echoes-color-background-accent-weak-default);
`;
