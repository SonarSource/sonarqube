module.exports = function (dashboardName) {
  var l10nKey = 'dashboard.' + dashboardName + '.name';
  var l10nLabel = window.t(l10nKey);
  if (l10nLabel !== l10nKey) {
    return l10nLabel;
  } else {
    return dashboardName;
  }
};
