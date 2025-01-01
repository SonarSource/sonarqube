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

import { Button, ButtonVariety, RadioButtonGroup } from '@sonarsource/echoes-react';
import * as React from 'react';
import { FlagMessage, Modal } from '~design-system';
import { setLogLevel } from '../../../api/system';
import { translate } from '../../../helpers/l10n';
import { LOGS_LEVELS } from '../utils';

interface Props {
  infoMsg: string;
  logLevel: string;
  onChange: () => void;
  onClose: () => void;
}

interface State {
  newLevel: string;
  updating: boolean;
}

const FORM_ID = 'set-log-level-form';
export default class ChangeLogLevelForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { newLevel: props.logLevel, updating: false };
  }

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { newLevel } = this.state;
    if (!this.state.updating) {
      this.setState({ updating: true });
      setLogLevel(newLevel).then(
        () => this.props.onChange(),
        () => this.setState({ updating: false }),
      );
    }
  };

  handleLevelChange = (value: string) => this.setState({ newLevel: value });

  render() {
    const { updating, newLevel } = this.state;
    const header = translate('system.set_log_level');
    return (
      <Modal
        headerTitle={header}
        onClose={this.props.onClose}
        body={
          <form id={FORM_ID} onSubmit={this.handleFormSubmit}>
            <div className="sw-mt-1">
              <RadioButtonGroup
                ariaLabel={header}
                id="system-loglevel-radio"
                options={LOGS_LEVELS.map((level) => ({ label: level, value: level }))}
                value={newLevel}
                onChange={this.handleLevelChange}
              />
            </div>

            <FlagMessage className="sw-mt-2" variant="info">
              {this.props.infoMsg}
            </FlagMessage>
            {newLevel !== 'INFO' && (
              <FlagMessage className="sw-mt-2" variant="warning">
                {translate('system.log_level.warning')}
              </FlagMessage>
            )}
          </form>
        }
        primaryButton={
          <Button
            isDisabled={updating}
            id="set-log-level-submit"
            type="submit"
            form={FORM_ID}
            variety={ButtonVariety.Primary}
          >
            {translate('save')}
          </Button>
        }
        secondaryButtonLabel={translate('cancel')}
        loading={updating}
      />
    );
  }
}
