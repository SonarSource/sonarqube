import _ from 'underscore';

const STORAGE_KEY = 'sonar_recent_history';
const HISTORY_LIMIT = 10;

export default class RecentHistory {
  static get () {
    var history = localStorage.getItem(STORAGE_KEY);
    if (history == null) {
      history = [];
    } else {
      try {
        history = JSON.parse(history);
      } catch (e) {
        RecentHistory.clear();
        history = [];
      }
    }
    return history;
  }

  static set (newHistory) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newHistory));
  }

  static clear () {
    localStorage.removeItem(STORAGE_KEY);
  }

  static add (componentKey, componentName, icon) {
    var sonarHistory = RecentHistory.get();

    if (componentKey) {
      var newEntry = { key: componentKey, name: componentName, icon: icon };
      var newHistory = _.reject(sonarHistory, entry => entry.key === newEntry.key);
      newHistory.unshift(newEntry);
      newHistory = _.first(newHistory, HISTORY_LIMIT);
      RecentHistory.set(newHistory);
    }
  }
}
