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
import * as PropTypes from 'prop-types';
import { connect } from 'react-redux';
import OnboardingModal from './OnboardingModal';
import { skipOnboarding } from '../../../store/users/actions';

interface DispatchProps {
  skipOnboarding: () => void;
}

export class OnboardingPage extends React.PureComponent<DispatchProps> {
  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  onSkipOnboardingTutorial = () => {
    this.props.skipOnboarding();
    this.context.router.replace('/');
  };

  render() {
    return <OnboardingModal onFinish={this.onSkipOnboardingTutorial} />;
  }
}

const mapDispatchToProps: DispatchProps = { skipOnboarding };

export default connect<{}, DispatchProps, {}>(null, mapDispatchToProps)(OnboardingPage);
