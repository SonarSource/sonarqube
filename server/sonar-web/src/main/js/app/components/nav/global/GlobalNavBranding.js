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
import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'react-router';
import { connect } from 'react-redux';
import { getGlobalSettingValue } from '../../../../store/rootReducer';
import { translate } from '../../../../helpers/l10n';

class GlobalNavBranding extends React.PureComponent {
  static propTypes = {
    customLogoUrl: PropTypes.string,
    customLogoWidth: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
  };

  renderLogo() {
    const url = this.props.customLogoUrl || `${window.baseUrl}/images/logo.svg?v=6.6`;
    const width = this.props.customLogoUrl ? this.props.customLogoWidth || 100 : 83;
    const height = 30;
    const title = translate('layout.sonar.slogan');
    return <img src={url} width={width} height={height} alt={title} title={title} />;
  }

  render() {
    return (
      <div className="pull-left">
        <Link to="/" className="navbar-brand">
          {this.renderLogo()}
        </Link>
      </div>
    );
  }
}

const mapStateToProps = state => ({
  customLogoUrl: (getGlobalSettingValue(state, 'sonar.lf.logoUrl') || {}).value,
  customLogoWidth: (getGlobalSettingValue(state, 'sonar.lf.logoWidthPx') || {}).value
});

export default connect(mapStateToProps)(GlobalNavBranding);
