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
import ModalView from './modals';

export default ModalView.extend({
  ui() {
    return {
      messagesContainer: '.js-modal-messages'
    };
  },

  events() {
    return {
      ...ModalView.prototype.events.apply(this, arguments),
      'keydown input,textarea,select': 'onInputKeydown',
      'submit form': 'onFormSubmit'
    };
  },

  onRender() {
    ModalView.prototype.onRender.apply(this, arguments);
    const that = this;
    setTimeout(() => {
      that
        .$(':tabbable')
        .first()
        .focus();
    }, 0);
  },

  onInputKeydown(e) {
    if (e.keyCode === 27) {
      // escape
      this.destroy();
    }
  },

  onFormSubmit(e) {
    e.preventDefault();
  },

  showErrors(errors, warnings) {
    const container = this.ui.messagesContainer.empty();
    if (Array.isArray(errors)) {
      errors.forEach(error => {
        const html = `<div class="alert alert-danger">${error.msg}</div>`;
        container.append(html);
      });
    }
    if (Array.isArray(warnings)) {
      warnings.forEach(warn => {
        const html = `<div class="alert alert-warning">${warn.msg}</div>`;
        container.append(html);
      });
    }
    this.ui.messagesContainer.scrollParent().scrollTop(0);
  },

  showSingleError(msg) {
    this.showErrors([{ msg }], []);
  },

  disableForm() {
    const form = this.$('form');
    this.disabledFields = form.find(':input:not(:disabled)');
    this.disabledFields.prop('disabled', true);
  },

  enableForm() {
    if (this.disabledFields != null) {
      this.disabledFields.prop('disabled', false);
    }
  },

  showSpinner() {
    this.$('.js-modal-spinner').removeClass('hidden');
  },

  hideSpinner() {
    this.$('.js-modal-spinner').addClass('hidden');
  }
});
