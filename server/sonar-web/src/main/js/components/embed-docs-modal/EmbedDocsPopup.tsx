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
import { translate } from '../../helpers/l10n';
import { getBaseUrl } from '../../helpers/system';
import { SuggestionLink } from '../../types/types';
import DocLink from '../common/DocLink';
import Link from '../common/Link';
import { DropdownOverlay } from '../controls/Dropdown';
import { SuggestionsContext } from './SuggestionsContext';

interface Props {
  onClose: () => void;
}

export default class EmbedDocsPopup extends React.PureComponent<Props> {
  firstItem: HTMLAnchorElement | null = null;

  /*
   * Will be called by the first suggestion (if any), as well as the first link (documentation)
   * Since we don't know if we have any suggestions, we need to allow both to make the call.
   * If we have at least 1 suggestion, it will make the call first, and prevent 'documentation' from
   * getting the focus.
   */
  focusFirstItem: React.Ref<HTMLAnchorElement> = (node: HTMLAnchorElement | null) => {
    if (node && !this.firstItem) {
      this.firstItem = node;
      this.firstItem.focus();
    }
  };

  renderTitle(text: string, labelId: string) {
    return (
      <h2 className="menu-header" id={labelId}>
        {text}
      </h2>
    );
  }

  renderSuggestions = ({ suggestions }: { suggestions: SuggestionLink[] }) => {
    if (suggestions.length === 0) {
      return null;
    }
    return (
      <>
        {this.renderTitle(translate('docs.suggestion'), 'suggestion')}
        <ul className="menu abs-width-240" aria-labelledby="suggestion">
          {suggestions.map((suggestion, i) => (
            <li key={suggestion.link}>
              <DocLink
                innerRef={i === 0 ? this.focusFirstItem : undefined}
                onClick={this.props.onClose}
                to={suggestion.link}
              >
                {suggestion.text}
              </DocLink>
            </li>
          ))}
        </ul>
      </>
    );
  };

  renderIconLink(link: string, icon: string, text: string) {
    return (
      <a href={link} rel="noopener noreferrer" target="_blank">
        <img
          alt={text}
          aria-hidden={true}
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
        <SuggestionsContext.Consumer>{this.renderSuggestions}</SuggestionsContext.Consumer>
        <ul className="menu abs-width-240">
          <li>
            <DocLink innerRef={this.focusFirstItem} onClick={this.props.onClose} to="/">
              {translate('docs.documentation')}
            </DocLink>
          </li>
          <li>
            <Link onClick={this.props.onClose} to="/web_api">
              {translate('api_documentation.page')}
            </Link>
          </li>
        </ul>
        <ul className="menu abs-width-240">
          <li>
            <Link
              className="display-flex-center"
              to="https://community.sonarsource.com/"
              target="_blank"
            >
              {translate('docs.get_help')}
            </Link>
          </li>
        </ul>
        {this.renderTitle(translate('docs.stay_connected'), 'stay_connected')}
        <ul className="menu abs-width-240" aria-labelledby="stay_connected">
          <li>
            {this.renderIconLink(
              'https://www.sonarqube.org/whats-new/?referrer=sonarqube',
              'embed-doc/sq-icon.svg',
              translate('docs.news')
            )}
          </li>
          <li>
            {this.renderIconLink(
              'https://www.sonarqube.org/roadmap/?referrer=sonarqube',
              'embed-doc/sq-icon.svg',
              translate('docs.roadmap')
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
