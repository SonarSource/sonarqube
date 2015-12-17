module.exports = function (diff) {
  var message = '';
  if (diff.newValue != null) {
    message = window.tp('issue.changelog.changed_to', window.t('issue.changelog.field', diff.key), diff.newValue);
  } else {
    message = window.tp('issue.changelog.removed', window.t('issue.changelog.field', diff.key));
  }
  if (diff.oldValue != null) {
    message += ' (';
    message += window.tp('issue.changelog.was', diff.oldValue);
    message += ')';
  }
  return message;
};
