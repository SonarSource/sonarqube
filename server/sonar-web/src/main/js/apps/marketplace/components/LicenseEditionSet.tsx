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
import { stringify } from 'querystring';
import * as React from 'react';
import * as classNames from 'classnames';
import { FormattedMessage } from 'react-intl';
import { debounce } from 'lodash';
import Checkbox from '../../../components/controls/Checkbox';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { omitNil } from '../../../helpers/request';
import { Edition, getFormData, getLicensePreview } from '../../../api/marketplace';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export interface Props {
  className?: string;
  edition?: Edition;
  editions: Edition[];
  updateLicense: (license?: string, status?: string) => void;
}

interface State {
  acceptTerms: boolean;
  formData?: {
    serverId?: string;
    ncloc?: number;
  };
  license: string;
  licenseEdition?: Edition;
  loading: boolean;
  previewStatus?: string;
  wrongEdition: boolean;
}

export default class LicenseEditionSet extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { acceptTerms: false, license: '', loading: false, wrongEdition: false };
    this.fetchLicensePreview = debounce(this.fetchLicensePreview, 100);
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchFormData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchLicensePreview = (license: string) => {
    this.setState({ loading: true });
    getLicensePreview({ license }).then(
      ({ previewStatus, nextEditionKey }) => {
        if (this.mounted) {
          const { edition } = this.props;
          const licenseEdition = this.props.editions.find(
            edition => edition.key === nextEditionKey
          );
          const wrongEdition = Boolean(
            !licenseEdition || (edition && edition.key !== nextEditionKey)
          );
          this.setLicense({ license, loading: false, licenseEdition, previewStatus, wrongEdition });
        }
      },
      () => {
        if (this.mounted) {
          this.resetLicense({ license, loading: false });
        }
      }
    );
  };

  fetchFormData = () => {
    getFormData().then(
      formData => {
        if (this.mounted) {
          this.setState({ formData });
        }
      },
      () => {}
    );
  };

  getLicenseFormUrl = (edition: Edition) => {
    let url = edition.licenseRequestUrl;
    if (this.state.formData) {
      const query = stringify(omitNil(this.state.formData));
      if (query) {
        url += '?' + query;
      }
    }
    return url;
  };

  handleLicenseChange = (event: React.SyntheticEvent<HTMLTextAreaElement>) => {
    const license = event.currentTarget.value;
    if (license) {
      this.fetchLicensePreview(license);
      this.setState({ license });
    } else {
      this.resetLicense({});
    }
  };

  handleTermsCheck = (checked: boolean) => {
    this.setLicense({ acceptTerms: checked });
  };

  resetLicense<K extends keyof State>(state: Pick<State, K>) {
    this.setLicense(
      Object.assign(
        {
          license: '',
          licenseEdition: undefined,
          previewStatus: undefined,
          wrongEdition: false
        },
        state
      )
    );
  }

  setLicense<K extends keyof State>(state: Pick<State, K>) {
    this.setState(state, this.updateParentLicense);
  }

  updateParentLicense = () => {
    const { acceptTerms, license, previewStatus, wrongEdition } = this.state;
    this.props.updateLicense(
      previewStatus !== 'NO_INSTALL' && !acceptTerms ? undefined : license,
      wrongEdition ? undefined : previewStatus
    );
  };

  renderAlert() {
    const { licenseEdition, previewStatus, wrongEdition } = this.state;
    if (!previewStatus || wrongEdition) {
      const { edition } = this.props;

      return (
        <div className="spacer-top">
          {wrongEdition && (
            <p className="alert alert-danger">
              {edition
                ? translateWithParameters('marketplace.wrong_license_type_x', edition.name)
                : translate('marketplace.wrong_license_type')}
            </p>
          )}
          {edition && (
            <a href={this.getLicenseFormUrl(edition)} target="_blank">
              {translate('marketplace.i_need_a_license')}
            </a>
          )}
        </div>
      );
    }

    return (
      <div className="spacer-top">
        <p
          className={classNames('alert', {
            'alert-warning': previewStatus === 'AUTOMATIC_INSTALL',
            'alert-success': previewStatus === 'NO_INSTALL',
            'alert-danger': previewStatus === 'MANUAL_INSTALL'
          })}>
          {translateWithParameters(
            'marketplace.license_preview_status.' + previewStatus,
            licenseEdition ? licenseEdition.name : translate('marketplace.commercial_edition')
          )}
          {licenseEdition &&
            licenseEdition.key === 'datacenter' &&
            previewStatus !== 'NO_INSTALL' && (
              <span className="little-spacer-left">
                <FormattedMessage
                  defaultMessage={translate('marketplace.how_to_setup_cluster_url')}
                  id="marketplace.how_to_setup_cluster_url"
                  values={{
                    url: (
                      <a
                        href="https://redirect.sonarsource.com/doc/data-center-edition.html"
                        rel="noopener noreferrer"
                        target="_blank">
                        {licenseEdition.name}
                      </a>
                    )
                  }}
                />
              </span>
            )}
        </p>
        {previewStatus !== 'NO_INSTALL' && (
          <span className="js-edition-tos">
            <Checkbox
              checked={this.state.acceptTerms}
              id="edition-terms"
              onCheck={this.handleTermsCheck}>
              <label className="little-spacer-left" htmlFor="edition-terms">
                {translate('marketplace.i_accept_the')}
              </label>
            </Checkbox>
            <a
              className="nowrap little-spacer-left"
              href="http://dist.sonarsource.com/SonarSource_Terms_And_Conditions.pdf"
              rel="noopener noreferrer"
              target="_blank">
              {translate('marketplace.terms_and_conditions')}
            </a>
          </span>
        )}
      </div>
    );
  }

  render() {
    const { className, edition } = this.props;
    const { license, loading } = this.state;

    return (
      <div className={className}>
        {edition && (
          <label className="display-inline-block spacer-bottom" htmlFor="set-license">
            {translateWithParameters('marketplace.enter_license_for_x', edition.name)}
            <em className="mandatory">*</em>
          </label>
        )}
        <textarea
          autoFocus={true}
          className="display-block input-super-large"
          id="set-license"
          onChange={this.handleLicenseChange}
          required={true}
          rows={8}
          style={{ resize: 'none' }}
          value={license}
        />

        <DeferredSpinner
          customSpinner={
            <p className="spacer-top">
              <i className="spinner spacer-right text-bottom" />
              {translate('marketplace.checking_license')}
            </p>
          }
          loading={loading}>
          {this.renderAlert()}
        </DeferredSpinner>
      </div>
    );
  }
}
