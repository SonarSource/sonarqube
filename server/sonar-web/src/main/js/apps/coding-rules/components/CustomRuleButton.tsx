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
import * as React from 'react';
import CustomRuleFormModal from './CustomRuleFormModal';
import { RuleDetails } from '../../../app/types';

interface Props {
  children: (
    props: { onClick: (event: React.SyntheticEvent<HTMLButtonElement>) => void }
  ) => React.ReactNode;
  customRule?: RuleDetails;
  onDone: (newRuleDetails: RuleDetails) => void;
  organization: string | undefined;
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

  handleClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
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
            organization={this.props.organization}
            templateRule={this.props.templateRule}
          />
        )}
      </>
    );
  }
}
