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
import Template from './CreationModalTemplate.hbs';
import ModalForm from '../../../../components/common/modal-form';
import { parseError } from '../../../../helpers/request';

export default ModalForm.extend({
  template: Template,

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();

    const name = this.$('#create-link-name').val();
    const url = this.$('#create-link-url').val();

    this.options
      .onCreate(name, url)
      .then(() => this.destroy())
      .catch(e => {
        parseError(e).then(msg => this.showSingleError(msg));
        this.enableForm();
      });
  }
});
