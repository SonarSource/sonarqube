export function getLocalizedDashboardName (baseName) {
  var l10nKey = 'dashboard.' + baseName + '.name';
  var l10nLabel = window.t(l10nKey);
  return l10nLabel !== l10nKey ? l10nLabel : baseName;
}
