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
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  onFinish: () => void
|};
*/

/*::
type State = {
  OnboardingContainer?: Object
};
*/

export default class OnboardingModal extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {};

  componentDidMount() {
    this.mounted = true;
    import('./OnboardingContainer').then(i => this.receiveComponent(i.default));
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  receiveComponent = (OnboardingContainer /*: Object */) => {
    if (this.mounted) {
      this.setState({ OnboardingContainer });
    }
  };

  render() {
    const { OnboardingContainer } = this.state;

    return (
      <Modal contentLabel={translate('tutorials.onboarding')} large={true}>
        {OnboardingContainer != null && <OnboardingContainer onFinish={this.props.onFinish} />}
      </Modal>
    );
  }
}
