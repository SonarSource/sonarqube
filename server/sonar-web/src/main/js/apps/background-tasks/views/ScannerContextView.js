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
import Modal from '../../../components/common/modals';
import Template from './ScannerContextView.hbs';
import { getTask } from '../../../api/ce';

export default Modal.extend({
  template: Template,
  className: 'modal modal-large',

  initialize () {
    this.scannerContext = null;
    this.loadScannerContext();
  },

  loadScannerContext () {
    getTask(this.options.task.id, ['scannerContext']).then(task => {
      this.scannerContext = task.scannerContext;
      this.render();
    });
  },

  serializeData () {
    return {
      task: this.options.task,
      scannerContext: this.scannerContext
    };
  }
});

