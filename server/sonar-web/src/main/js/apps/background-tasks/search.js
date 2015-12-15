import $ from 'jquery';
import moment from 'moment';
import React from 'react';
import RadioToggle from '../../components/shared/radio-toggle';
import { STATUSES, CURRENTS, DATE, DATE_FORMAT } from './constants';

export default React.createClass({
  componentDidMount() {
    this.attachDatePicker();
  },

  componentDidUpdate() {
    this.attachDatePicker();
  },

  getCurrentsOptions() {
    return [
      { value: CURRENTS.ALL, label: window.t('background_tasks.currents_filter.ALL') },
      { value: CURRENTS.ONLY_CURRENTS, label: window.t('background_tasks.currents_filter.ONLY_CURRENTS') }
    ];
  },

  getStatusOptions() {
    return [
      { value: STATUSES.ALL, label: window.t('background_task.status.ALL') },
      { value: STATUSES.SUCCESS, label: window.t('background_task.status.SUCCESS') },
      { value: STATUSES.FAILED, label: window.t('background_task.status.FAILED') },
      { value: STATUSES.CANCELED, label: window.t('background_task.status.CANCELED') }
    ];
  },

  getDateOptions() {
    return [
      { value: DATE.ANY, label: window.t('background_tasks.date_filter.ALL') },
      { value: DATE.TODAY, label: window.t('background_tasks.date_filter.TODAY') },
      { value: DATE.CUSTOM, label: window.t('background_tasks.date_filter.CUSTOM') }
    ];
  },

  onDateChange(newDate) {
    if (newDate === DATE.CUSTOM) {
      let minDateRaw = this.refs.minDate.value;
      let maxDateRaw = this.refs.maxDate.value;
      let minDate = moment(minDateRaw, DATE_FORMAT, true);
      let maxDate = moment(maxDateRaw, DATE_FORMAT, true);
      this.props.onDateChange(newDate,
          minDate.isValid() ? minDate : null,
          maxDate.isValid() ? maxDate : null);
    } else {
      this.props.onDateChange(newDate);
    }
  },

  onDateInputChange() {
    this.onDateChange(DATE.CUSTOM);
  },

  attachDatePicker() {
    let opts = {
      dateFormat: 'yy-mm-dd',
      changeMonth: true,
      changeYear: true,
      onSelect: this.onDateInputChange
    };
    if ($.fn && $.fn.datepicker) {
      $(this.refs.minDate).datepicker(opts);
      $(this.refs.maxDate).datepicker(opts);
    }
  },

  renderCustomDateInput() {
    let shouldBeVisible = this.props.dateFilter === DATE.CUSTOM;
    let className = shouldBeVisible ? 'spacer-top' : 'spacer-top hidden';
    return (
        <div className={className}>
          from&nbsp;
          <input onChange={this.onDateInputChange} ref="minDate" type="text"/>
          &nbsp;to&nbsp;
          <input onChange={this.onDateInputChange} ref="maxDate" type="text"/>
        </div>
    );
  },

  onSearchFormSubmit(e) {
    e.preventDefault();
    this.onSearch();
  },

  onSearch() {
    let searchInput = this.refs.searchInput;
    let query = searchInput.value;
    this.props.onSearch(query);
  },

  renderSearchBox() {
    if (this.props.options && this.props.options.component) {
      // do not render search form on the project-level page
      return null;
    }
    return (
        <form onSubmit={this.onSearchFormSubmit} className="search-box">
          <button className="search-box-submit button-clean">
            <i className="icon-search"></i>
          </button>
          <input onChange={this.onSearch}
                 value={this.props.searchQuery}
                 ref="searchInput"
                 className="search-box-input"
                 type="search"
                 placeholder="Search"/>
        </form>
    );
  },

  refresh(e) {
    e.preventDefault();
    this.props.refresh();
    let btn = e.target;
    btn.disabled = true;
    setTimeout(() => btn.disabled = false, 500);
  },

  render() {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <ul className="list-inline">
            <li>
              <RadioToggle options={this.getStatusOptions()} value={this.props.statusFilter}
                           name="background-task-status" onCheck={this.props.onStatusChange}/>
            </li>
            <li>
              <RadioToggle options={this.getCurrentsOptions()} value={this.props.currentsFilter}
                           name="background-task-currents" onCheck={this.props.onCurrentsChange}/>
            </li>
            <li>
              <RadioToggle options={this.getDateOptions()} value={this.props.dateFilter}
                           name="background-task-date" onCheck={this.onDateChange}/>
              {this.renderCustomDateInput()}
            </li>
            <li>{this.renderSearchBox()}</li>
            <li className="pull-right">
              <button onClick={this.refresh} ref="reloadButton">{window.t('reload')}</button>
            </li>
          </ul>
        </section>
    );
  }
});
