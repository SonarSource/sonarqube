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
import $ from 'jquery';
import ModalForm from '../../components/common/modal-form';
import Template from './templates/metrics-form.hbs';

export default ModalForm.extend({
  template: Template,

  onRender() {
    const that = this;
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    this.$('#create-metric-domain')
      .select2({
        width: '250px',
        createSearchChoice(term) {
          return { id: term, text: '+' + term };
        },
        createSearchChoicePosition: 'top',
        initSelection(element, callback) {
          const value = $(element).val();
          callback({ id: value, text: value });
        },
        query(options) {
          const items = that.options.domains.filter(
            d => d.toLowerCase().indexOf(options.term.toLowerCase()) !== -1
          );
          const results = items.map(item => {
            return { id: item, text: item };
          });
          options.callback({ results, more: false });
        }
      })
      .select2('val', this.model && this.model.get('domain'));
    this.$('#create-metric-type').select2({ width: '250px' });
  },

  onDestroy() {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  serializeData() {
    return {
      ...ModalForm.prototype.serializeData.apply(this, arguments),
      domains: this.options.domains,
      types: this.options.types
    };
  }
});
