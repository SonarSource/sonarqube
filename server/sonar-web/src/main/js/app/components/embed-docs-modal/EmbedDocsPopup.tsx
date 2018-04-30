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
import { Link } from 'react-router';
import { SuggestionLink } from './SuggestionsProvider';
import * as theme from '../../../app/theme';
import BubblePopup, { BubblePopupPosition } from '../../../components/common/BubblePopup';
import DetachIcon from '../../../components/icons-components/DetachIcon';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/urls';

interface Props {
  onClose: () => void;
  popupPosition?: BubblePopupPosition;
  suggestions: Array<SuggestionLink>;
}

export default class EmbedDocsPopup extends React.PureComponent<Props> {
  static contextTypes = {
    onSonarCloud: PropTypes.bool,
    openOnboardingTutorial: PropTypes.func
  };

  onAnalyzeProjectClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.context.openOnboardingTutorial();
  };

  renderTitle(text: string) {
    return <li className="dropdown-header">{text}</li>;
  }

  renderSuggestions() {
    if (this.props.suggestions.length === 0) {
      return null;
    }
    return (
      <>
        {this.renderTitle(translate('embed_docs.suggestion'))}
        {this.props.suggestions.map((suggestion, index) => (
          <li key={index}>
            <Link className="display-flex-center" target="_blank" to={suggestion.link}>
              {suggestion.text}
              <DetachIcon className="spacer-left" fill={theme.gray80} size={12} />
            </Link>
          </li>
        ))}
        <li className="divider" />
      </>
    );
  }

  renderIconLink(link: string, icon: string, text: string) {
    return (
      <a href={link} rel="noopener noreferrer" target="_blank">
        <img
          alt={text}
          className="spacer-right"
          height="18"
          src={`${getBaseUrl()}/images/embed-doc/${icon}`}
          width="18"
        />
        {text}
      </a>
    );
  }

  renderSonarCloudLinks() {
    return (
      <React.Fragment>
        <li className="divider" />
        {this.renderTitle(translate('embed_docs.get_support'))}
        <li>
          {this.renderIconLink(
            'https://about.sonarcloud.io/contact/',
            'sc-icon.svg',
            translate('embed_docs.contact_form')
          )}
        </li>
        {this.renderTitle(translate('embed_docs.stay_connected'))}
        <li>
          {this.renderIconLink('https://about.sonarcloud.io/news/', 'sc-icon.svg', 'Product News')}
        </li>
        <li>
          {this.renderIconLink('https://twitter.com/sonarcloud', 'twitter-icon.svg', 'Twitter')}
        </li>
      </React.Fragment>
    );
  }

  renderSonarQubeLinks() {
    return (
      <React.Fragment>
        <li>
          <a href="#" onClick={this.onAnalyzeProjectClick}>
            {translate('embed_docs.analyze_new_project')}
          </a>
        </li>
        <li className="divider" />
        {this.renderTitle(translate('embed_docs.get_support'))}
        <li>
          {this.renderIconLink(
            'https://groups.google.com/forum/#!forum/sonarqube',
            'google-group-icon.svg',
            'Google Groups'
          )}
        </li>
        <li>
          {this.renderIconLink(
            'http://stackoverflow.com/questions/tagged/sonarqube',
            'so-icon.svg',
            'Stack Overflow'
          )}
        </li>
        {this.renderTitle(translate('embed_docs.stay_connected'))}
        <li>
          {this.renderIconLink('https://blog.sonarsource.com/', 'sq-icon.svg', 'Product News')}
        </li>
        <li>
          {this.renderIconLink('https://twitter.com/SonarQube', 'twitter-icon.svg', 'Twitter')}
        </li>
      </React.Fragment>
    );
  }

  render() {
    return (
      <BubblePopup
        customClass="bubble-popup-bottom bubble-popup-menu abs-width-240 embed-docs-popup"
        position={this.props.popupPosition}>
        <ul className="menu">
          {this.renderSuggestions()}
          <li>
            <Link className="display-flex-center" target="_blank" to="/documentation">
              {translate('embed_docs.documentation_index')}
              <DetachIcon className="spacer-left" fill={theme.gray80} size={12} />
            </Link>
          </li>
          <li>
            <Link onClick={this.props.onClose} to="/web_api">
              {translate('api_documentation.page')}
            </Link>
          </li>
          {this.context.onSonarCloud && this.renderSonarCloudLinks()}
          {!this.context.onSonarCloud && this.renderSonarQubeLinks()}
        </ul>
      </BubblePopup>
    );
  }
}
