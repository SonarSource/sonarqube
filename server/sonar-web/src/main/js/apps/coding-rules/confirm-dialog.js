import $ from 'jquery';
import _ from 'underscore';

const DEFAULTS = {
  title: 'Confirmation',
  html: '',
  yesLabel: 'Yes',
  noLabel: 'Cancel',
  yesHandler: function () {
    // no op
  },
  noHandler: function () {
    // no op
  },
  always: function () {
    // no op
  }
};

export default function (options) {
  var settings = _.extend({}, DEFAULTS, options),
      dialog = $('<div><div class="modal-head"><h2>' + settings.title + '</h2></div><div class="modal-body">' +
          settings.html + '</div><div class="modal-foot"><button data-confirm="yes">' + settings.yesLabel +
          '</button> <a data-confirm="no" class="action">' + settings.noLabel + '</a></div></div>');

  $('[data-confirm=yes]', dialog).on('click', function () {
    dialog.dialog('close');
    settings.yesHandler();
    return settings.always();
  });

  $('[data-confirm=no]', dialog).on('click', function () {
    dialog.dialog('close');
    settings.noHandler();
    return settings.always();
  });

  return dialog.dialog({
    modal: true,
    minHeight: null,
    width: 540
  });
}
