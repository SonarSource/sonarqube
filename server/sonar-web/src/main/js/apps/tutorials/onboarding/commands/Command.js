/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import ClipboardButton from '../../../../components/controls/ClipboardButton';
import { translate } from '../../../../helpers/l10n';

/*::
type Props = {
  command: string | Array<?string>,
  isOneLine?: boolean
};
*/

// keep this "useless" concatentation for the readability reason
// eslint-disable-next-line no-useless-concat
const s = ' \\' + '\n  ';

export default class Command extends React.PureComponent {
  /*:: props: Props; */

  render() {
    const { command, isOneLine } = this.props;
    const commandArray = Array.isArray(command) ? command.filter(line => line != null) : [command];
    const finalCommand = isOneLine ? commandArray.join(' ') : commandArray.join(s);

    return (
      <div
        className={classNames('onboarding-command', { 'onboarding-command-oneline': isOneLine })}>
        <pre>{finalCommand}</pre>
        <ClipboardButton copyValue={finalCommand} tooltipPlacement="top" />
      </div>
    );
  }
}
