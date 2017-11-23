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
import * as React from 'react';
import Helmet from 'react-helmet';
import { connect } from 'react-redux';
import { AppState } from '../../../store/appState/duck';
import { getAppState } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import init from '../init';
import '../styles.css';

interface Props {
  appState: AppState;
  organization?: string;
  params: { organizationKey?: string };
}

class CodingRulesAppContainer extends React.PureComponent<Props> {
  container?: HTMLElement | null;
  stop?: () => void;

  componentDidMount() {
    const { organizationKey } = this.props.params;
    const { defaultOrganization, organizationsEnabled } = this.props.appState;
    if (organizationsEnabled && !organizationKey) {
      // redirect to organization-level rules page
      this.context.router.replace(
        `/organizations/${defaultOrganization}/rules${window.location.hash}`
      );
    } else if (this.container) {
      this.stop = init(this.container, organizationKey);
    }
  }

  componentWillUnmount() {
    if (this.stop) {
      this.stop();
    }
  }

  render() {
    // placing container inside div is required,
    // because when backbone.marionette's layout is destroyed,
    // it also destroys the root element,
    // but react wants it to be there to unmount it
    return (
      <div>
        <Helmet title={translate('coding_rules.page')} />
        <div ref={node => (this.container = node)} />
      </div>
    );
  }
}

const mapStateToProps = (state: any) => ({
  appState: getAppState(state)
});

export default connect(mapStateToProps)(CodingRulesAppContainer);
