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
import MetaTagsSelector from './MetaTagsSelector';
import { setProjectTags } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import TagsList from '../../../components/tags/TagsList';
import { BubblePopupPosition } from '../../../components/common/BubblePopup';
import { Component } from '../../../app/types';

interface Props {
  component: Component;
  onComponentChange: (changes: {}) => void;
}

interface State {
  popupOpen: boolean;
  popupPosition: BubblePopupPosition;
}

export default class MetaTags extends React.PureComponent<Props, State> {
  card?: HTMLDivElement | null;
  tagsList?: HTMLButtonElement | null;
  tagsSelector?: HTMLDivElement | null;
  state: State = { popupOpen: false, popupPosition: { top: 0, right: 0 } };

  componentDidMount() {
    if (this.canUpdateTags() && this.tagsList && this.card) {
      const buttonPos = this.tagsList.getBoundingClientRect();
      const cardPos = this.card.getBoundingClientRect();
      this.setState({ popupPosition: this.getPopupPos(buttonPos, cardPos) });

      window.addEventListener('keydown', this.handleKey, false);
      window.addEventListener('click', this.handleOutsideClick, false);
    }
  }

  componentWillUnmount() {
    window.removeEventListener('keydown', this.handleKey);
    window.removeEventListener('click', this.handleOutsideClick);
  }

  handleKey = (evt: KeyboardEvent) => {
    // Escape key
    if (evt.keyCode === 27) {
      this.setState({ popupOpen: false });
    }
  };

  handleOutsideClick = (evt: Event) => {
    if (!this.tagsSelector || !this.tagsSelector.contains(evt.target as Node)) {
      this.setState({ popupOpen: false });
    }
  };

  handleClick = (evt: React.SyntheticEvent<HTMLButtonElement>) => {
    evt.stopPropagation();
    this.setState(state => ({ popupOpen: !state.popupOpen }));
  };

  canUpdateTags = () => {
    const { configuration } = this.props.component;
    return configuration && configuration.showSettings;
  };

  getPopupPos = (eltPos: ClientRect, containerPos: ClientRect) => ({
    top: eltPos.height,
    right: containerPos.width - eltPos.width
  });

  handleSetProjectTags = (tags: string[]) => {
    setProjectTags({ project: this.props.component.key, tags: tags.join(',') }).then(
      () => this.props.onComponentChange({ tags }),
      () => {}
    );
  };

  render() {
    const { key } = this.props.component;
    const { popupOpen, popupPosition } = this.state;
    const tags = this.props.component.tags || [];

    if (this.canUpdateTags()) {
      return (
        <div className="big-spacer-top overview-meta-tags" ref={card => (this.card = card)}>
          <button
            className="button-link"
            onClick={this.handleClick}
            ref={tagsList => (this.tagsList = tagsList)}>
            <TagsList allowUpdate={true} tags={tags.length ? tags : [translate('no_tags')]} />
          </button>
          {popupOpen && (
            <div ref={tagsSelector => (this.tagsSelector = tagsSelector)}>
              <MetaTagsSelector
                position={popupPosition}
                project={key}
                selectedTags={tags}
                setProjectTags={this.handleSetProjectTags}
              />
            </div>
          )}
        </div>
      );
    } else {
      return (
        <div className="big-spacer-top overview-meta-tags">
          <TagsList
            allowUpdate={false}
            className="note"
            tags={tags.length ? tags : [translate('no_tags')]}
          />
        </div>
      );
    }
  }
}
