/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import BaseView from './base-viewer-view';
import SourceViewer from '../../source-viewer/main';
import Template from '../templates/workspace-viewer.hbs';

export default BaseView.extend({
  template: Template,

  onRender () {
    BaseView.prototype.onRender.apply(this, arguments);
    this.showViewer();
  },

  showViewer () {
    const that = this;
    const viewer = new SourceViewer();
    const options = this.model.toJSON();
    viewer.open(this.model.get('uuid'), { workspace: true });
    viewer.on('loaded', () => {
      that.model.set({
        name: viewer.model.get('name'),
        q: viewer.model.get('q')
      });
      if (options.line != null) {
        viewer.highlightLine(options.line);
        viewer.scrollToLine(options.line);
      }
    });
    this.viewerRegion.show(viewer);
  }
});

