export default {
  getLocalizedDashboardName(baseName) {
    var l10nKey = 'dashboard.' + baseName + '.name';
    var l10nLabel = window.t(l10nKey);
    if (l10nLabel !== l10nKey) {
      return l10nLabel;
    } else {
      return baseName;
    }
  }
};
