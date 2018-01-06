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
// @flow
import React from 'react';
import classNames from 'classnames';
import LinksHelp from './LinksHelp';
import LinksHelpSonarCloud from './LinksHelpSonarCloud';
import ShortcutsHelp from './ShortcutsHelp';
import TutorialsHelp from './TutorialsHelp';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {
  currentUser: { isLoggedIn: boolean },
  onClose: () => void,
  onTutorialSelect: () => void,
  onSonarCloud?: boolean
};
*/

/*::
type State = {
  section: string
};
*/

export default class GlobalHelp extends React.PureComponent {
  /*:: props: Props; */
  state /*: State */ = { section: 'shortcuts' };

  handleCloseClick = (event /*: Event */) => {
    event.preventDefault();
    this.props.onClose();
  };

  handleSectionClick = (event /*: Event & { currentTarget: HTMLElement } */) => {
    event.preventDefault();
    const { section } = event.currentTarget.dataset;
    this.setState({ section });
  };

  renderSection = () => {
    switch (this.state.section) {
      case 'shortcuts':
        return <ShortcutsHelp />;
      case 'links':
        return this.props.onSonarCloud ? (
          <LinksHelpSonarCloud onClose={this.props.onClose} />
        ) : (
          <LinksHelp onClose={this.props.onClose} />
        );
      case 'tutorials':
        return <TutorialsHelp onTutorialSelect={this.props.onTutorialSelect} />;
      default:
        return null;
    }
  };

  renderMenuItem = (section /*: string */) => (
    <li key={section}>
      <a
        className={classNames({ active: section === this.state.section })}
        data-section={section}
        href="#"
        onClick={this.handleSectionClick}>
        {translate('help.section', section)}
      </a>
    </li>
  );

  renderMenu = () => (
    <ul className="side-tabs-menu">
      {(this.props.currentUser.isLoggedIn && !this.props.onSonarCloud
        ? ['shortcuts', 'tutorials', 'links']
        : ['shortcuts', 'links']
      ).map(this.renderMenuItem)}
    </ul>
  );

  render() {
    return (
      <Modal contentLabel={translate('help')} medium={true} onRequestClose={this.props.onClose}>
        <div className="modal-head">
          <h2>{translate('help')}</h2>
        </div>

        <div className="side-tabs-layout">
          <div className="side-tabs-side">{this.renderMenu()}</div>
          <div className="side-tabs-main">{this.renderSection()}</div>
        </div>

        <div className="modal-foot">
          <a className="js-modal-close" href="#" onClick={this.handleCloseClick}>
            {translate('close')}
          </a>
        </div>
      </Modal>
    );
  }
}
