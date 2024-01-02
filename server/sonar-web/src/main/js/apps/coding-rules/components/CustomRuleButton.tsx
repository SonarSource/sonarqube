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
import * as React from 'react';
import { RuleDetails } from '../../../types/types';
import CustomRuleFormModal from './CustomRuleFormModal';

interface Props {
  children: (props: { onClick: () => void }) => React.ReactNode;
  customRule?: RuleDetails;
  onDone: (newRuleDetails: RuleDetails) => void;
  templateRule: RuleDetails;
}

interface State {
  modal: boolean;
}

export default class CustomRuleButton extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { modal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClick = () => {
    this.setState({ modal: true });
  };

  handleModalClose = () => {
    if (this.mounted) {
      this.setState({ modal: false });
    }
  };

  handleDone = (newRuleDetails: RuleDetails) => {
    this.handleModalClose();
    this.props.onDone(newRuleDetails);
  };

  render() {
    return (
      <>
        {this.props.children({ onClick: this.handleClick })}
        {this.state.modal && (
          <CustomRuleFormModal
            customRule={this.props.customRule}
            onClose={this.handleModalClose}
            onDone={this.handleDone}
            templateRule={this.props.templateRule}
          />
        )}
      </>
    );
  }
}
