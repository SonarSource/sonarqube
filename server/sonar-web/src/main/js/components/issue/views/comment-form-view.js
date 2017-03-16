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
import $ from 'jquery';
import PopupView from '../../common/popup';
import Template from '../templates/comment-form.hbs';

export default PopupView.extend({
  className: 'bubble-popup issue-comment-bubble-popup',
  template: Template,

  ui: {
    textarea: '.issue-comment-form-text textarea',
    cancelButton: '.js-issue-comment-cancel',
    submitButton: '.js-issue-comment-submit'
  },

  events: {
    'click': 'onClick',
    'keydown @ui.textarea': 'onKeydown',
    'keyup @ui.textarea': 'toggleSubmit',
    'click @ui.cancelButton': 'cancel',
    'click @ui.submitButton': 'submit'
  },

  onRender() {
    const that = this;
    PopupView.prototype.onRender.apply(this, arguments);
    setTimeout(
      () => {
        that.ui.textarea.focus();
      },
      100
    );
  },

  toggleSubmit() {
    this.ui.submitButton.prop('disabled', this.ui.textarea.val().length === 0);
  },

  onClick(e) {
    e.stopPropagation();
  },

  onKeydown(e) {
    if (e.keyCode === 27) {
      this.destroy();
    }
    if (e.keyCode === 13 && (e.metaKey || e.ctrlKey)) {
      this.submit();
    }
  },

  cancel() {
    this.options.detailView.updateAfterAction();
  },

  disableForm() {
    this.$(':input').prop('disabled', true);
  },

  enableForm() {
    this.$(':input').prop('disabled', false);
  },

  submit() {
    const text = this.ui.textarea.val();

    if (!text.length) {
      return;
    }

    const update = this.model && this.model.has('key');
    const method = update ? 'edit_comment' : 'add_comment';
    const url = window.baseUrl + '/api/issues/' + method;
    const data = { text };
    if (update) {
      data.key = this.model.get('key');
    } else {
      data.issue = this.options.issue.id;
    }
    this.disableForm();
    this.options.detailView.disableControls();
    $.post(url, data).done(r => this.options.detailView.updateAfterAction(r)).fail(() => {
      this.enableForm();
      this.options.detailView.enableControls();
    });
  },

  serializeData() {
    const options = { fromTransition: false, ...this.options.additionalOptions };
    return {
      ...PopupView.prototype.serializeData.apply(this, arguments),
      options
    };
  }
});
