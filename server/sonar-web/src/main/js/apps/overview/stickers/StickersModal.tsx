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
import StickerParams from './StickerParams';
import { getStickerUrl, StickerType, StickerOptions } from './utils';
import { Component } from '../../../app/types';
import { translate } from '../../../helpers/l10n';
import './styles.css';

interface Props {
  component: Component;
}

interface State {
  open: boolean;
  selectedType: StickerType;
  stickerOptions: StickerOptions;
}

export default class StickersModal extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      open: false,
      selectedType: StickerType.measure,
      stickerOptions: {
        color: 'white',
        component: props.component.key,
        metric: 'alert_status'
      }
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState(state => ({
      stickerOptions: { ...state.stickerOptions, component: nextProps.component.key }
    }));
  }

  handleClose = () => this.setState({ open: false });

  handleOpen = () => this.setState({ open: true });

  handleSelectSticker = (selectedType: StickerType) => this.setState({ selectedType });

  handleUpdateOptions = (options: Partial<StickerOptions>) =>
    this.setState(state => ({
      stickerOptions: { ...state.stickerOptions, ...options }
    }));

  handleCancelClick = () => this.handleClose();

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
              <p className="huge-spacer-bottom">{translate('overview.stickers.description')}</p>
              <div className="stickers-list spacer-bottom">
                <StickerButton
                  onClick={this.handleSelectSticker}
                  selected={StickerType.measure === selectedType}
                  type={StickerType.measure}
                  url={getStickerUrl(StickerType.measure, stickerOptions)}
                />
                <StickerButton
                  onClick={this.handleSelectSticker}
                  selected={StickerType.marketing === selectedType}
                  type={StickerType.marketing}
                  url={getStickerUrl(StickerType.marketing, stickerOptions)}
                />
              </div>
              <p className="text-center note huge-spacer-bottom">
                {translate('overview.stickers', selectedType, 'description')}
              </p>
              <StickerParams
                className="big-spacer-bottom"
                options={stickerOptions}
                type={selectedType}
                updateOptions={this.handleUpdateOptions}
              />
              <StickerSnippet snippet={getStickerUrl(selectedType, stickerOptions)} />
            </div>
            <footer className="modal-foot">
              <button className="button-link js-modal-close" onClick={this.handleCancelClick}>
                {translate('close')}
              </button>
            </footer>
          </Modal>
        )}
      </>
    );
  }
}
