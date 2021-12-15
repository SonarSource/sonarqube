/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Link } from 'react-router';
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getBaseUrl } from 'sonar-ui-common/helpers/urls';
import { isSonarCloud } from '../../../helpers/system';
import ProductNewsMenuItem from './ProductNewsMenuItem';
import { SuggestionsContext } from './SuggestionsContext';
import Modal from 'sonar-ui-common/components/controls/Modal';
import { ClearButton } from 'sonar-ui-common/components/controls/buttons';
import { getCurrentUser, Store } from '../../../store/rootReducer';
import { connect } from 'react-redux';
import { getApiKeyForZoho } from '../../../api/codescan';

interface Props {
  onClose: () => void;
  currentUser: T.LoggedInUser;
}

type State = {
  reseting: boolean;
  zohoUrl: string;
}

export class EmbedDocsPopup extends React.PureComponent<Props, State> {

  state: State = {
    reseting: false,
    zohoUrl: ''
  };

  componentDidMount() {
    this.getZohoDeskUrl();
  }

  handleClosePopup = () => {
    this.setState({ reseting: false });
  };

  handleResetPopup = () => {
    this.setState({ reseting: true });
  };

  renderTitle(text: string) {
    return <li className="menu-header">{text}</li>;
  }

  renderSuggestions = ({ suggestions }: { suggestions: T.SuggestionLink[] }) => {
    if (suggestions.length === 0) {
      return null;
    }
    return (
      <>
        {this.renderTitle(translate('embed_docs.suggestion'))}
        {suggestions.map((suggestion, index) => (
          <li key={index}>
            <Link onClick={this.props.onClose} target="_blank" to={suggestion.link}>
              {suggestion.text}
            </Link>
          </li>
        ))}
        <li className="divider" />
      </>
    );
  };

  renderIconLink(link: string, icon: string, text: string) {
    return (
      <a href={link} rel="noopener noreferrer" target="_blank">
        <img
          alt={text}
          className="spacer-right"
          height="18"
          src={`${getBaseUrl()}/images/${icon}`}
          width="18"
        />
        {text}
      </a>
    );
  }

  renderAboutCodescan(link: string, icon: string, text: string) {
    return (
      <Modal className="abs-width-auto" onRequestClose={this.handleClosePopup} contentLabel={''}>
        <a href={link} rel="noopener noreferrer" target="_blank">
          <img alt={text} src={`${getBaseUrl()}/images/${icon}`} />
        </a>
        <span className="cross-button">
          <ClearButton onClick={this.handleClosePopup} />
        </span>
      </Modal>
    );
  }

  getZohoDeskUrl() {
    const { currentUser } = this.props;
    const payLoad = {
      "operation": "signup",
      "email": currentUser.email,
      "loginName": "support.autorabit",
      "fullName": currentUser.name,
      "utype": "portal",
    }

    // get zohoApiKey
    return getApiKeyForZoho(payLoad).then((response: any) => {
      const zohoUrl = `https://support.autorabit.com/support/RemoteAuth?operation=${payLoad.operation}&email=${payLoad.email}&fullname=${payLoad.fullName}&loginname=${payLoad.loginName}&utype=${payLoad.utype}&ts=${response.ts}&apikey=${response.apiKey}`;
      this.setState({ zohoUrl: zohoUrl });
    })
  }

  renderSonarCloudLinks() {
    return (
      <>
        <li className="divider" />
        <li>
          <a
            href={this.state.zohoUrl}
            rel="noopener noreferrer"
            target="_blank">
            {translate('embed_docs.get_help')}
          </a>
        </li>
        <li>
          <a onClick={this.handleResetPopup}>
          {translate('embed_docs.about_codescan')}
          </a>
          {this.state.reseting && this.renderAboutCodescan(
            'https://knowledgebase.autorabit.com/codescan/docs/codescan-release-notes',
            'embed-doc/codescan-version.png',
            translate('embed_docs.codescan_version')
          )}
        </li>
        <li className="divider" />
        {this.renderTitle(translate('embed_docs.stay_connected'))}
        <li>
          {this.renderIconLink(
            'https://twitter.com/CodeScanforSFDC',
            'embed-doc/twitter-icon.svg',
            'Twitter'
          )}
        </li>
        <li>
          {this.renderIconLink(
            'https://www.codescan.io/blog/',
            'sonarcloud-square-logo.svg',
            translate('embed_docs.blog')
          )}
        </li>
        <li>
          <ProductNewsMenuItem tag="SonarCloud" />
        </li>
      </>
    );
  }

  renderSonarQubeLinks() {
    return (
      <>
        <li className="divider" />
        <li>
          <a href={this.state.zohoUrl} rel="noopener noreferrer" target="_blank">
            {translate('embed_docs.get_help')}
          </a>
        </li>
        <li>
          <a onClick={this.handleResetPopup}>
          {translate('embed_docs.about_codescan')}
          </a>
          {this.state.reseting && this.renderAboutCodescan(
            'https://knowledgebase.autorabit.com/codescan/docs/codescan-release-notes',
            'embed-doc/codescan-version.png',
            translate('embed_docs.codescan_version')
          )}
        </li>
        <li className="divider" />
        {this.renderTitle(translate('embed_docs.stay_connected'))}
        <li>
          {this.renderIconLink(
            'https://www.sonarqube.org/whats-new/?referrer=sonarqube',
            'embed-doc/sq-icon.svg',
            translate('embed_docs.news')
          )}
        </li>
        <li>
          {this.renderIconLink(
            'https://twitter.com/SonarQube',
            'embed-doc/twitter-icon.svg',
            'Twitter'
          )}
        </li>
      </>
    );
  }

  render() {
    return (
      <DropdownOverlay>
        <ul className="menu abs-width-240">
          <SuggestionsContext.Consumer>{this.renderSuggestions}</SuggestionsContext.Consumer>
          <li>
            <Link onClick={this.props.onClose} target="_blank" to="https://knowledgebase.autorabit.com/codescan/docs">
              {translate('embed_docs.documentation')}
            </Link>
          </li>
          <li>
            <Link onClick={this.props.onClose} to="/web_api">
              {translate('api_documentation.page')}
            </Link>
          </li>
          {isSonarCloud() ? this.renderSonarCloudLinks() : this.renderSonarQubeLinks()}
        </ul>
      </DropdownOverlay>
    );
  }
}

const mapStateToProps = (state: Store) => {
  return {
    currentUser: getCurrentUser(state) as T.LoggedInUser
  };
};

export default connect(mapStateToProps)(EmbedDocsPopup);
