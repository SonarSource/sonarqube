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
import { FormattedMessage } from 'react-intl';
import EditionBox from './components/EditionBox';
import LicenseEditionForm from './components/LicenseEditionForm';
import { Edition, Editions, EditionStatus, getEditionsList } from '../../api/marketplace';
import { translate } from '../../helpers/l10n';

export interface Props {
  editionStatus?: EditionStatus;
  updateCenterActive: boolean;
}

interface State {
  editions: Editions;
  editionsError: boolean;
  loading: boolean;
  installEdition?: Edition;
}

export default class EditionBoxes extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { editions: {}, editionsError: false, loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchEditions();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchEditions = () => {
    this.setState({ loading: true });
    getEditionsList().then(
      editions => {
        if (this.mounted) {
          this.setState({
            loading: false,
            editions,
            editionsError: false
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ editionsError: true, loading: false });
        }
      }
    );
  };

  handleOpenLicenseForm = (edition: Edition) => this.setState({ installEdition: edition });
  handleCloseLicenseForm = () => this.setState({ installEdition: undefined });

  render() {
    const { editions, loading, installEdition } = this.state;
    if (loading) {
      return null;
    }
    return (
      <div className="spacer-bottom marketplace-editions">
        {this.state.editionsError ? (
          <span className="alert alert-info">
            <FormattedMessage
              defaultMessage={translate('marketplace.editions_unavailable')}
              id="marketplace.editions_unavailable"
              values={{
                url: (
                  <a href="https://www.sonarsource.com" target="_blank">
                    SonarSource.com
                  </a>
                )
              }}
            />
          </span>
        ) : (
          Object.keys(editions).map(key => (
            <EditionBox
              edition={editions[key]}
              editionKey={key}
              editionStatus={this.props.editionStatus}
              key={key}
              onInstall={this.handleOpenLicenseForm}
            />
          ))
        )}

        {installEdition && (
          <LicenseEditionForm edition={installEdition} onClose={this.handleCloseLicenseForm} />
        )}
      </div>
    );
  }
}
