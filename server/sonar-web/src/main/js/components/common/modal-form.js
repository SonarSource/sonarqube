import _ from 'underscore';
import ModalView from './modals';

export default ModalView.extend({

  ui: function () {
    return {
      messagesContainer: '.js-modal-messages'
    };
  },

  events: function () {
    return _.extend(ModalView.prototype.events.apply(this, arguments), {
      'keydown input,textarea,select': 'onInputKeydown',
      'submit form': 'onFormSubmit'
    });
  },

  onRender: function () {
    ModalView.prototype.onRender.apply(this, arguments);
    var that = this;
    setTimeout(function () {
      that.$(':tabbable').first().focus();
    }, 0);
  },

  onInputKeydown: function (e) {
    if (e.keyCode === 27) {
      // escape
      this.destroy();
    }
  },

  onFormSubmit: function (e) {
    e.preventDefault();
  },

  showErrors: function (errors, warnings) {
    var container = this.ui.messagesContainer.empty();
    if (_.isArray(errors)) {
      errors.forEach(function (error) {
        var html = '<div class="alert alert-danger">' + error.msg + '</div>';
        container.append(html);
      });
    }
    if (_.isArray(warnings)) {
      warnings.forEach(function (warn) {
        var html = '<div class="alert alert-warning">' + warn.msg + '</div>';
        container.append(html);
      });
    }
    this.ui.messagesContainer.scrollParent().scrollTop(0);
  },

  disableForm: function () {
    var form = this.$('form');
    this.disabledFields = form.find(':input:not(:disabled)');
    this.disabledFields.prop('disabled', true);
  },

  enableForm: function () {
    if (this.disabledFields != null) {
      this.disabledFields.prop('disabled', false);
    }
  }
});


