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
import { Helmet } from 'react-helmet-async';
import { LargeCenteredLayout, PageContentFontWrapper, Spinner, Title } from '~design-system';
import { checkSecretKey, generateSecretKey } from '../../../api/settings';
import { translate } from '../../../helpers/l10n';
import EncryptionForm from './EncryptionForm';
import GenerateSecretKeyForm from './GenerateSecretKeyForm';

interface State {
  loading: boolean;
  secretKey?: string;
  secretKeyAvailable?: boolean;
}

export default class EncryptionApp extends React.PureComponent<{}, State> {
  state: State = { loading: true };
  mounted = false;

  componentDidMount() {
    this.mounted = true;
    this.checkSecretKey();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkSecretKey = () => {
    checkSecretKey().then(
      ({ secretKeyAvailable }) => {
        if (this.mounted) {
          this.setState({ loading: false, secretKeyAvailable });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  generateSecretKey = () => {
    return generateSecretKey().then(({ secretKey }) => {
      if (this.mounted) {
        this.setState({ secretKey, secretKeyAvailable: false });
      }
    });
  };

  render() {
    const { loading, secretKey, secretKeyAvailable } = this.state;

    return (
      <LargeCenteredLayout id="encryption-page">
        <PageContentFontWrapper className="sw-my-8 sw-typo-default">
          <Helmet defer={false} title={translate('property.category.security.encryption')} />
          <header>
            <Title>{translate('property.category.security.encryption')}</Title>
            <Spinner loading={loading} />
          </header>

          {!loading && !secretKeyAvailable && (
            <GenerateSecretKeyForm
              generateSecretKey={this.generateSecretKey}
              secretKey={secretKey}
            />
          )}

          {secretKeyAvailable && <EncryptionForm generateSecretKey={this.generateSecretKey} />}
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}
