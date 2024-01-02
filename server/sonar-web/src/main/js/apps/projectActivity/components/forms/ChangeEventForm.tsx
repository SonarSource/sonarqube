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
import { ButtonPrimary, InputField, Modal } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { AnalysisEvent } from '../../../../types/project-activity';

interface Props {
  changeEvent: (event: string, name: string) => Promise<void>;
  event: AnalysisEvent;
  header: string;
  onClose: () => void;
}

interface State {
  name: string;
}

export default class ChangeEventForm extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { name: props.event.name };
  }

  changeInput = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.target.value });
  };

  handleSubmit = () => {
    this.props.changeEvent(this.props.event.key, this.state.name);
    this.props.onClose();
  };

  render() {
    const { name } = this.state;
    return (
      <Modal
        headerTitle={this.props.header}
        onClose={this.props.onClose}
        body={
          <form id="change-event-form">
            <label htmlFor="name">{translate('name')}</label>
            <InputField
              id="name"
              className="sw-my-2"
              autoFocus
              onChange={this.changeInput}
              type="text"
              value={name}
              size="full"
            />
          </form>
        }
        primaryButton={
          <ButtonPrimary
            id="change-event-submit"
            form="change-event-form"
            type="submit"
            disabled={!name || name === this.props.event.name}
            onClick={this.handleSubmit}
          >
            {translate('change_verb')}
          </ButtonPrimary>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
