/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { getRedirectUrlForZoho } from "../../api/codescan";
import withCurrentUserContext from "../../app/components/current-user/withCurrentUserContext";
import { CurrentUserContextInterface } from "../../app/components/current-user/CurrentUserContext";
import { LoggedInUser } from "../../types/users";
import { Button } from "../controls/buttons";
import { GlobalSettingKeys } from '../../types/settings';
import { getValue } from '../../api/settings';

interface Props {
  currentUser: LoggedInUser;
  onClose: () => void;
  showAboutCodescanPopup: () => void;
}

type State = {
  zohoUrl: string;
}

class EmbedDocsPopup extends React.PureComponent<Props & CurrentUserContextInterface, State> {
  firstItem: HTMLAnchorElement | null = null;

  state: State = {
    zohoUrl: '',
    enableZoho: true
  };

  componentDidMount() {
    this.getSetting();
  }

  getSetting = async () => {
    const enabledSupportLink = await getValue({ key: GlobalSettingKeys.CodescanSupportLink });
    if (enabledSupportLink.value === undefined || enabledSupportLink.value === "true") {
      this.getZohoDeskUrl();
      this.setState({ enableZoho : true });
    } else {
      this.setState({ enableZoho : false });
    }
  }
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

  getZohoDeskUrl() {
    const { currentUser } = this.props;

  // get zoho re-direct url
  return getRedirectUrlForZoho().then(response => {
     const zohoUrl = response.redirectUrl;
     this.setState({ zohoUrl });
    })
  }

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
            <Link onClick={this.props.onClose} target="_blank" to="https://knowledgebase.autorabit.com/codescan">
              {translate('embed_docs.documentation')}
            </Link>
          </li>
          <li>
            <Link onClick={this.props.onClose} to="/web_api">
              {translate('api_documentation.page')}
            </Link>
          </li>
        </ul>
        <ul className="menu abs-width-240">
          <li style={{ display: this.state.enableZoho == false ? "none" : "list-item" }}>
            <Link
              className="display-flex-center"
              to={this.state.zohoUrl}
              target="_blank"
            >
              {translate('docs.get_help')}
            </Link>
          </li>
          <li>
            <Button onClick={() => this.props.showAboutCodescanPopup()}>
              {translate('embed_docs.about_codescan')}
            </Button>
          </li>
        </ul>
        {this.renderTitle(translate('docs.stay_connected'), 'stay_connected')}
        <ul className="menu abs-width-240" aria-labelledby="stay_connected">
          <li>
            {this.renderIconLink(
              'https://twitter.com/CodeScanforSFDC',
              'embed-doc/twitter-icon.svg',
              'Twitter'
            )}
          </li>
          <li>
            {this.renderIconLink(
                'https://www.codescan.io/blog',
                'sonarcloud-square-logo.svg',
                translate('embed_docs.blog')
            )}
          </li>
        </ul>
      </DropdownOverlay>
    );
  }
}

export default withCurrentUserContext(EmbedDocsPopup)
