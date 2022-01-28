/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { SuggestionLink } from '../../../types/types';
import { SuggestionsContext } from './SuggestionsContext';

interface Props {
  onClose: () => void;
}

export default class EmbedDocsPopup extends React.PureComponent<Props> {
  renderTitle(text: string) {
    return <li className="menu-header">{text}</li>;
  }

  renderSuggestions = ({ suggestions }: { suggestions: SuggestionLink[] }) => {
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

  render() {
    return (
      <DropdownOverlay>
        <ul className="menu abs-width-240">
          <SuggestionsContext.Consumer>{this.renderSuggestions}</SuggestionsContext.Consumer>
          <li>
            <Link onClick={this.props.onClose} target="_blank" to="/documentation">
              {translate('embed_docs.documentation')}
            </Link>
          </li>
          <li>
            <Link onClick={this.props.onClose} to="/web_api">
              {translate('api_documentation.page')}
            </Link>
          </li>
          <li className="divider" />
          <li>
            <a href="https://community.sonarsource.com/" rel="noopener noreferrer" target="_blank">
              {translate('embed_docs.get_help')}
            </a>
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
        </ul>
      </DropdownOverlay>
    );
  }
}
