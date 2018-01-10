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
import Modal from '../../../components/controls/Modal';
import StickerButton from './StickerButton';
import StickerSnippet from './StickerSnippet';
import { translate } from '../../../helpers/l10n';
import { getStickerUrl, StickerType, StickerOptions } from './utils';
import './styles.css';

interface State {
  open: boolean;
  selectedType: StickerType;
  stickerOptions: StickerOptions;
}

export default class StickersModal extends React.PureComponent<{}, State> {
  mounted: boolean;
  state: State = {
    open: false,
    selectedType: StickerType.marketing,
    stickerOptions: { color: 'white' }
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleClose = () => this.setState({ open: false });

  handleOpen = () => this.setState({ open: true });

  handleSelectSticker = (selectedType: StickerType) => this.setState({ selectedType });

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.handleClose();
  };

  render() {
    const { selectedType, stickerOptions } = this.state;
    const header = translate('overview.stickers.title');
    return (
      <>
        <button onClick={this.handleOpen}>{translate('overview.stickers.get_badge')}</button>
        {this.state.open && (
          <Modal contentLabel={header} onRequestClose={this.handleClose}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>
            <div className="modal-body">
              <p className="big-spacer-bottom">{translate('overview.stickers.description')}</p>
              <div className="stickers-list spacer-bottom">
                <StickerButton
                  onClick={this.handleSelectSticker}
                  selected={StickerType.marketing === selectedType}
                  type={StickerType.marketing}
                  url={getStickerUrl(StickerType.marketing, stickerOptions)}
                />
              </div>
              <p className="text-center note big-spacer-bottom">
                {translate('overview.stickers', selectedType, 'description')}
              </p>
              <StickerSnippet snippet={getStickerUrl(selectedType, stickerOptions)} />
            </div>
            <footer className="modal-foot">
              <a className="js-modal-close" href="#" onClick={this.handleCancelClick}>
                {translate('close')}
              </a>
            </footer>
          </Modal>
        )}
      </>
    );
  }
}
