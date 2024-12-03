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

import { Heading, LinkHighlight } from '@sonarsource/echoes-react';
import { useIntl } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { Component } from '../../../types/types';
import Workers from './Workers';

interface Props {
  component?: Component;
}

export default function Header(props: Readonly<Props>) {
  const intl = useIntl();
  return (
    <header className="sw-mb-12 sw-flex sw-justify-between">
      <div className="sw-flex-1">
        <Heading as="h2" className="sw-mb-4">
          {translate('background_tasks.page')}
        </Heading>
        <p className="sw-max-w-3/4">
          {intl.formatMessage(
            { id: 'background_tasks.page.description' },
            {
              link: (text) => (
                <DocumentationLink
                  shouldOpenInNewTab
                  highlight={LinkHighlight.CurrentColor}
                  to={DocLink.BackgroundTasks}
                >
                  {text}
                </DocumentationLink>
              ),
            },
          )}
        </p>
      </div>
      {!props.component && (
        <div>
          <Workers />
        </div>
      )}
    </header>
  );
}
