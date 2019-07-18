/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as React from 'react';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { Profile as BaseProfile } from '../../../api/quality-profiles';
import ActivationFormModal from './ActivationFormModal';

interface Props {
  activation?: T.RuleActivation;
  buttonText: string;
  className?: string;
  modalHeader: string;
  onDone: (severity: string) => Promise<void>;
  organization: string | undefined;
  profiles: BaseProfile[];
  rule: T.Rule | T.RuleDetails;
}

interface State {
  modal: boolean;
}

export default class ActivationButton extends React.PureComponent<Props, State> {
  state: State = { modal: false };

  handleButtonClick = () => {
    this.setState({ modal: true });
  };

  handleCloseModal = () => {
    this.setState({ modal: false });
  };

  render() {
    return (
      <>
        <Button
          className={this.props.className}
          id="coding-rules-quality-profile-activate"
          onClick={this.handleButtonClick}>
          {this.props.buttonText}
        </Button>

        {this.state.modal && (
          <ActivationFormModal
            activation={this.props.activation}
            modalHeader={this.props.modalHeader}
            onClose={this.handleCloseModal}
            onDone={this.props.onDone}
            organization={this.props.organization}
            profiles={this.props.profiles}
            rule={this.props.rule}
          />
        )}
      </>
    );
  }
}
