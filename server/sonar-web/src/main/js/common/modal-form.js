define(['common/modals'], function (ModalView) {

  return ModalView.extend({

    ui: function () {
      return {
        messagesContainer: '.js-modal-messages'
      };
    },

    events: function () {
      return _.extend(ModalView.prototype.events.apply(this, arguments), {
        'submit form': 'onFormSubmit'
      });
    },

    onFormSubmit: function (e) {
      e.preventDefault();
    },

    showErrors: function (errors, warnings) {
      var container = this.ui.messagesContainer.empty();
      if (_.isArray(errors)) {
        errors.forEach(function (error) {
          var html = '<div class="message-error">' + error.msg + '</div>';
          container.append(html);
        });
      }
      if (_.isArray(warnings)) {
        warnings.forEach(function (warn) {
          var html = '<div class="message-alert">' + warn.msg + '</div>';
          container.append(html);
        });
      }
    }
  });

});
