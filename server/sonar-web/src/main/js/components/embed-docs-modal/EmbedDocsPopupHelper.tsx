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
import { ButtonLink, ClearButton } from '../controls/buttons';
import Toggler from '../controls/Toggler';
import HelpIcon from '../icons/HelpIcon';
import EmbedDocsPopup from './EmbedDocsPopup';
import Modal from "../controls/Modal";
import { getBaseUrl } from "../../helpers/system";

interface State {
  helpOpen: boolean;
  aboutCodescanOpen: boolean;
}

export default class EmbedDocsPopupHelper extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = { helpOpen: false, aboutCodescanOpen: false };

  setHelpDisplay = (helpOpen: boolean) => {
    this.setState({ helpOpen });
  };

  handleClick = () => {
    this.toggleHelp();
  };

  toggleHelp = () => {
    this.setState((state) => {
      return { helpOpen: !state.helpOpen };
    });
  };

  closeHelp = () => {
    this.setState({ helpOpen: false });
  };

  renderAboutCodescan(link: string, icon: string, text: string) {
    return (
        <Modal
            className="abs-width-auto"
            onRequestClose={() => this.setState({ aboutCodescanOpen: false })}
            contentLabel=''
        >
          <a href={link} rel="noopener noreferrer" target="_blank">
            <img alt={text} src={`${getBaseUrl()}/images/${icon}`}/>
          </a>
          <span className="cross-button">
            <ClearButton onClick={() => this.setState({ aboutCodescanOpen: false })}/>
          </span>
        </Modal>
    );
  }

  render() {
    return (
      <div className="dropdown">
        <Toggler
          onRequestClose={this.closeHelp}
          open={this.state.helpOpen}
          overlay={<EmbedDocsPopup onClose={this.closeHelp} showAboutCodescanPopup={() => this.setState({aboutCodescanOpen: true})} />}
        >
          <ButtonLink
            aria-expanded={this.state.helpOpen}
            aria-haspopup={true}
            className="navbar-help navbar-icon"
            onClick={this.handleClick}
            title={translate('help')}
          >
            <HelpIcon />
          </ButtonLink>
        </Toggler>

        {this.state.aboutCodescanOpen && this.renderAboutCodescan(
            'https://knowledgebase.autorabit.com/codescan/docs/codescan-release-notes',
            'embed-doc/codescan-version-23_2_8.png',
            translate('embed_docs.codescan_version')
        )}
      </div>
    );
  }
}
