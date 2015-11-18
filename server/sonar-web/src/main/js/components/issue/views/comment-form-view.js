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

  onRender: function () {
    var that = this;
    PopupView.prototype.onRender.apply(this, arguments);
    setTimeout(function () {
      that.ui.textarea.focus();
    }, 100);
  },

  toggleSubmit: function () {
    this.ui.submitButton.prop('disabled', this.ui.textarea.val().length === 0);
  },

  onClick: function (e) {
    e.stopPropagation();
  },

  onKeydown: function (e) {
    if (e.keyCode === 27) {
      this.destroy();
    }
    if (e.keyCode === 13 && (e.metaKey || e.ctrlKey)) {
      this.submit();
    }
  },

  cancel: function () {
    this.options.detailView.updateAfterAction(false);
  },

  disableForm: function () {
    this.$(':input').prop('disabled', true);
  },

  enableForm: function () {
    this.$(':input').prop('disabled', false);
  },

  submit: function () {
    var that = this;
    var text = this.ui.textarea.val(),
        update = this.model && this.model.has('key'),
        method = update ? 'edit_comment' : 'add_comment',
        url = baseUrl + '/api/issues/' + method,
        data = { text: text };
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

  serializeData: function () {
    var options = _.defaults(this.options.additionalOptions, { fromTransition: false });
    return _.extend(PopupView.prototype.serializeData.apply(this, arguments), {
      options: options
    });
  }
});


