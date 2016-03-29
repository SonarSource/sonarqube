/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
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

  onRender () {
    const that = this;
    PopupView.prototype.onRender.apply(this, arguments);
    setTimeout(function () {
      that.ui.textarea.focus();
    }, 100);
  },

  toggleSubmit () {
    this.ui.submitButton.prop('disabled', this.ui.textarea.val().length === 0);
  },

  onClick (e) {
    e.stopPropagation();
  },

  onKeydown (e) {
    if (e.keyCode === 27) {
      this.destroy();
    }
    if (e.keyCode === 13 && (e.metaKey || e.ctrlKey)) {
      this.submit();
    }
  },

  cancel () {
    this.options.detailView.updateAfterAction(false);
  },

  disableForm () {
    this.$(':input').prop('disabled', true);
  },

  enableForm () {
    this.$(':input').prop('disabled', false);
  },

  submit () {
    const that = this;
    const text = this.ui.textarea.val();
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
    return $.post(url, data)
        .done(function () {
          that.options.detailView.updateAfterAction(true);
        }).fail(function () {
          that.enableForm();
          that.options.detailView.enableControls();
        });
  },

  serializeData () {
    const options = _.defaults(this.options.additionalOptions, { fromTransition: false });
    return _.extend(PopupView.prototype.serializeData.apply(this, arguments), {
      options
    });
  }
});


