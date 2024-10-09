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

import { LinkHighlight, LinkStandalone } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import Link from '../../common/Link';
import Tooltip from '../../controls/Tooltip';
import { SonarLintLogo } from '../../logos/SonarLintLogo';

const SONARLINT_URL =
  'https://www.sonarsource.com/products/sonarlint/features/connected-mode/?referrer=sonarqube-quick-fix';

interface Props {
  compact?: boolean;
}

export default function SonarLintBadge({ compact }: Readonly<Props>) {
  return compact ? <SonarLintBadgeCompact /> : <SonarLintBadgeFull />;
}

function SonarLintBadgeFull() {
  return (
    <Tooltip
      content={translate('issue.quick_fix_available_with_sonarlint_no_link')}
      mouseLeaveDelay={0.5}
    >
      <LinkStandalone
        className="sw-flex sw-items-center"
        highlight={LinkHighlight.Default}
        iconLeft={
          <SonarLintLogo
            className="it__issues-sonarlint-quick-fix"
            size={20}
            description={translate('issue.quick_fix_available_with_sonarlint_no_link')}
          />
        }
        shouldOpenInNewTab
        to={SONARLINT_URL}
      >
        {translate('issue.quick_fix')}
      </LinkStandalone>
    </Tooltip>
  );
}

function SonarLintBadgeCompact() {
  return (
    <Tooltip
      content={
        <FormattedMessage
          id="issue.quick_fix_available_with_sonarlint"
          defaultMessage={translate('issue.quick_fix_available_with_sonarlint')}
          values={{
            link: (
              <Link to={SONARLINT_URL} target="_blank">
                SonarLint
              </Link>
            ),
          }}
        />
      }
      mouseLeaveDelay={0.5}
    >
      <div className="sw-flex sw-items-center">
        <SonarLintLogo
          className="it__issues-sonarlint-quick-fix"
          size={15}
          description={translate('issue.quick_fix_available_with_sonarlint_no_link')}
        />
      </div>
    </Tooltip>
  );
}
