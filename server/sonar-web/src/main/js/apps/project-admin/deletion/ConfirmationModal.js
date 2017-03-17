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
import ModalForm from '../../../components/common/modal-form';
import Template from './ConfirmationModalTemplate.hbs';
import { deleteProject } from '../../../api/components';

export default ModalForm.extend({
  template: Template,

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.showSpinner();

    deleteProject(this.options.project.key)
      .then(() => {
        this.trigger('done');
      })
      .catch(function(e) {
        e.response.json().then(r => {
          this.hideSpinner();
          this.showErrors(r.errors, r.warnings);
          this.enableForm();
        });
      });
  },

  serializeData() {
    return { project: this.options.project };
  }
});
