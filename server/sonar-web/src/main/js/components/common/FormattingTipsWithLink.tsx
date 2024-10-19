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
import { CodeSnippet } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../helpers/l10n';
import { getFormattingHelpUrl } from '../../helpers/urls';

interface Props {
  className?: string;
}

export default class FormattingTipsWithLink extends React.PureComponent<Props> {
  handleClick(evt: React.SyntheticEvent<HTMLAnchorElement>) {
    evt.preventDefault();

    window.open(
      getFormattingHelpUrl(),
      'Formatting',
      'height=300,width=600,scrollbars=1,resizable=1',
    );
  }

  render() {
    return (
      <div className={this.props.className}>
        <LinkStandalone onClick={this.handleClick} to="#">
          {translate('formatting.helplink')}
        </LinkStandalone>

        <p className="sw-mt-2">
          <FormattedMessage
            id="formatting.example.link"
            values={{
              example: (
                <>
                  <br />

                  <CodeSnippet
                    isOneLine
                    noCopy
                    snippet={translate('formatting.example.link.example')}
                  />
                </>
              ),
            }}
          />
        </p>
      </div>
    );
  }
}
