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

import { LinkStandalone } from '@sonarsource/echoes-react';
import { LightPrimary, Title } from 'design-system';
import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { useLocation } from '~sonar-aligned/components/hoc/withRouter';
import { DocLink } from '../../../../helpers/doc-links';
import { useDocUrl } from '../../../../helpers/docs';

export function MonorepoProjectHeader() {
  const { formatMessage } = useIntl();
  const { query } = useLocation();
  const almKey = query.mode as string;

  return (
    <>
      <Title>
        <FormattedMessage
          id="onboarding.create_project.monorepo.title"
          values={{
            almName: formatMessage({ id: `alm.${almKey}` }),
          }}
        />
      </Title>
      <div>
        <LightPrimary>
          <FormattedMessage id="onboarding.create_project.monorepo.subtitle" />
        </LightPrimary>
      </div>
      <div className="sw-mt-3">
        <LinkStandalone shouldOpenInNewTab to={useDocUrl(DocLink.Monorepos)}>
          <FormattedMessage id="onboarding.create_project.monorepo.doc_link" />
        </LinkStandalone>
      </div>
    </>
  );
}
