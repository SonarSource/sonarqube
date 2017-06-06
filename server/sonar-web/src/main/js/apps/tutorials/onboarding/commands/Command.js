/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Clipboard from 'clipboard';
import Tooltip from '../../../../components/controls/Tooltip';
import { translate } from '../../../../helpers/l10n';

type Props = {
  command: string | Array<?string>
};

type State = {
  tooltipShown: boolean
};

const s = ' \\' + '\n  ';

export default class Command extends React.PureComponent {
  clipboard: Object;
  copyButton: HTMLButtonElement;
  mounted: boolean;
  props: Props;
  state: State = { tooltipShown: false };

  componentDidMount() {
    this.mounted = true;
    this.clipboard = new Clipboard(this.copyButton);
    this.clipboard.on('success', this.showTooltip);
  }

  componentWillUnmount() {
    this.mounted = false;
    this.clipboard.destroy();
  }

  showTooltip = () => {
    if (this.mounted) {
      this.setState({ tooltipShown: true });
      setTimeout(this.hideTooltip, 1000);
    }
  };

  hideTooltip = () => {
    if (this.mounted) {
      this.setState({ tooltipShown: false });
    }
  };

  render() {
    const { command } = this.props;
    const commandArray = Array.isArray(command) ? command.filter(line => line != null) : [command];
    const finalCommand = commandArray.join(s);

    const button = (
      <button data-clipboard-text={finalCommand} ref={node => (this.copyButton = node)}>
        {translate('copy')}
      </button>
    );

    return (
      <div className="onboarding-command">
        <pre>{finalCommand}</pre>
        {this.state.tooltipShown
          ? <Tooltip defaultVisible={true} placement="top" overlay="Copied!" trigger="manual">
              {button}
            </Tooltip>
          : button}
      </div>
    );
  }
}
