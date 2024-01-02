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
import { setApplicationTags, setProjectTags } from '../../../../../../api/components';
import { ButtonLink } from '../../../../../../components/controls/buttons';
import Dropdown from '../../../../../../components/controls/Dropdown';
import TagsList from '../../../../../../components/tags/TagsList';
import { PopupPlacement } from '../../../../../../components/ui/popups';
import { translate } from '../../../../../../helpers/l10n';
import { ComponentQualifier } from '../../../../../../types/component';
import { Component } from '../../../../../../types/types';
import MetaTagsSelector from './MetaTagsSelector';

interface Props {
  component: Component;
  onComponentChange: (changes: {}) => void;
}

export default class MetaTags extends React.PureComponent<Props> {
  card?: HTMLDivElement | null;
  tagsList?: HTMLElement | null;
  tagsSelector?: HTMLDivElement | null;

  canUpdateTags = () => {
    const { configuration } = this.props.component;
    return configuration && configuration.showSettings;
  };

  getPopupPos = (eltPos: ClientRect, containerPos: ClientRect) => ({
    top: eltPos.height,
    right: containerPos.width - eltPos.width,
  });

  setTags = (values: string[]) => {
    const { component } = this.props;

    if (component.qualifier === ComponentQualifier.Application) {
      return setApplicationTags({
        application: component.key,
        tags: values.join(','),
      });
    } else {
      return setProjectTags({
        project: component.key,
        tags: values.join(','),
      });
    }
  };

  handleSetProjectTags = (values: string[]) => {
    this.setTags(values).then(
      () => this.props.onComponentChange({ tags: values }),
      () => {}
    );
  };

  render() {
    const tags = this.props.component.tags || [];

    if (this.canUpdateTags()) {
      return (
        <div className="big-spacer-top project-info-tags" ref={(card) => (this.card = card)}>
          <Dropdown
            closeOnClick={false}
            closeOnClickOutside={true}
            overlay={
              <MetaTagsSelector selectedTags={tags} setProjectTags={this.handleSetProjectTags} />
            }
            overlayPlacement={PopupPlacement.BottomLeft}
          >
            <ButtonLink innerRef={(tagsList) => (this.tagsList = tagsList)} stopPropagation={true}>
              <TagsList allowUpdate={true} tags={tags.length ? tags : [translate('no_tags')]} />
            </ButtonLink>
          </Dropdown>
        </div>
      );
    } else {
      return (
        <div className="big-spacer-top project-info-tags">
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
