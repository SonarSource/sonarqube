#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
class Metric < ActiveRecord::Base

  DOMAIN_RULES = 'Rules'

  VALUE_TYPE_INT = 'INT'
  VALUE_TYPE_BOOLEAN = 'BOOL'
  VALUE_TYPE_FLOAT = 'FLOAT'
  VALUE_TYPE_PERCENT = 'PERCENT'
  VALUE_TYPE_STRING = 'STRING'
  VALUE_TYPE_DATA = 'DATA'
  VALUE_TYPE_MILLISEC = 'MILLISEC'
  VALUE_TYPE_LEVEL = 'LEVEL'
  VALUE_TYPE_DISTRIB = 'DISTRIB'
  VALUE_TYPE_RATING = 'RATING'
  VALUE_TYPE_WORK_DUR = 'WORK_DUR'

  TYPE_LEVEL_OK = 'OK'
  TYPE_LEVEL_WARN = 'WARN'
  TYPE_LEVEL_ERROR = 'ERROR'

  CACHE_KEY='metrics'
  I18N_DOMAIN_CACHE_KEY='i18n_domains'
  I18N_SHORT_NAME_CACHE_KEY='i18n_metric_short_names'

  validates_format_of       :name,   :with => /\A\w+\z/
  validates_length_of       :name,   :within => 1..64
  validates_uniqueness_of   :name
  validates_length_of       :short_name, :within => 1..64
  validates_inclusion_of    :val_type, :in => [VALUE_TYPE_INT,VALUE_TYPE_BOOLEAN,VALUE_TYPE_FLOAT,VALUE_TYPE_PERCENT,VALUE_TYPE_STRING,VALUE_TYPE_MILLISEC,VALUE_TYPE_LEVEL,
                                               VALUE_TYPE_DATA,VALUE_TYPE_DISTRIB,VALUE_TYPE_WORK_DUR], :message => "wrong value type"


  @@metrics_type_names = { VALUE_TYPE_INT => 'Integer', VALUE_TYPE_FLOAT => 'Float', VALUE_TYPE_PERCENT => 'Percent',
    VALUE_TYPE_BOOLEAN => 'Yes/No', VALUE_TYPE_STRING => 'Text', VALUE_TYPE_LEVEL => 'Level', VALUE_TYPE_WORK_DUR => 'Work Duration' }

  attr_accessible :name, :description, :direction, :domain, :short_name, :qualitative, :val_type, :user_managed

  def ==(other)
    name==other.name
  end

  def <=>(other)
    short_name<=>other.short_name
  end

  def self.domains(translate=true)
    all.collect{|metric|
      metric.domain(translate)
    }.compact.uniq.sort
  end

  # Localized domain name
  def self.domain_for(domain_key)
    return nil if domain_key.nil?

    localeMap = Metric.i18n_domain_cache[domain_key]
    locale = I18n.locale

    return localeMap[locale] if localeMap && localeMap.has_key?(locale)

    i18n_key = 'metric_domain.' + domain_key
    result = Api::Utils.message(i18n_key, :default => domain_key)
    localeMap[locale] = result if localeMap
    result
  end

  def self.name_for(metric_key)
    m=by_key(metric_key)
    m && m.short_name
  end

  def key
    name
  end

  def domain(translate=true)
    default_string = read_attribute(:domain)
    return default_string unless translate
    Metric.domain_for(default_string)
  end

  def domain=(value)
    write_attribute(:domain, value)
  end

  def short_name(translate=true)
    default_string = read_attribute(:short_name)
    return default_string unless translate

    metric_key = read_attribute(:name)
    return nil if metric_key.nil?

    localeMap = Metric.i18n_short_name_cache[metric_key]
    locale = I18n.locale

    return localeMap[locale] if localeMap && localeMap.has_key?(locale)

    i18n_key = 'metric.' + metric_key + '.name'
    result = Api::Utils.message(i18n_key, :default => default_string)
    localeMap[locale] = result if localeMap
    result
  end

  def short_name=(value)
    write_attribute(:short_name, value)
  end

  def abbreviation
    label = Api::Utils.message("metric.#{key}.abbreviation", :default => '')
    label = Api::Utils.message("metric.#{key}.name", :default => short_name) if label==''
    label
  end

  def description(translate=true)
    default_string = read_attribute(:description) || ''
    return default_string unless translate

    metric_name = read_attribute(:name)

    return nil if metric_name.nil?

    i18n_key = 'metric.' + metric_name + '.description'
    result = Api::Utils.message(i18n_key, :default => default_string)
    result
  end

  def description=(value)
    write_attribute(:description, value)
  end

  def user_managed?
    user_managed==true
  end

  def value_type
    val_type
  end

  def numeric?
    val_type==VALUE_TYPE_INT || val_type==VALUE_TYPE_FLOAT || val_type==VALUE_TYPE_PERCENT || val_type==VALUE_TYPE_MILLISEC || val_type==VALUE_TYPE_RATING || val_type==VALUE_TYPE_WORK_DUR
  end

  def data?
    val_type==VALUE_TYPE_DATA || val_type==VALUE_TYPE_DISTRIB
  end

  def quantitative?
    !qualitative?
  end

  def qualitative?
    qualitative
  end

  def hidden
    read_attribute(:hidden)
  end

  def value_type_name
    @@metrics_type_names[value_type]
  end

  def self.value_type_names
    # return a copy of the hash
    metrics_type_names_clone = {}
    @@metrics_type_names.each_pair {|type, descr|
      metrics_type_names_clone[type] = descr
    }
    metrics_type_names_clone
  end

  def timemachine?
    enabled && !hidden && (val_type != VALUE_TYPE_DATA)
  end

  def display?
    enabled && !data? && !hidden
  end

  def alertable?
    (!data?) && (!hidden) && (key!=Metric::ALERT_STATUS) && (val_type != VALUE_TYPE_RATING)
  end

  def suffix
    case val_type
      when VALUE_TYPE_PERCENT
        '%'
      when VALUE_TYPE_MILLISEC
        'ms'
      else ''
    end
  end

  def self.all
    cache['ALL']
  end

  def self.by_id(id)
    cache[id.to_s]
  end

  def self.by_name(key)
    cache[key]
  end

  def self.by_key(key)
    cache[key.to_s]
  end

  def self.by_domain(domain, translate=true)
    all.select{|metric| metric.domain(translate)==domain}.sort
  end

  def self.clear_cache
    Caches.clear(CACHE_KEY)
    Caches.clear(I18N_DOMAIN_CACHE_KEY)
    Caches.clear(I18N_SHORT_NAME_CACHE_KEY)
  end

  def self.by_keys(keys)
    result=[]
    keys.each do |k|
      result<<by_key(k)
    end
    result.compact.uniq
  end

  def self.ids_from_keys(keys_array)
    keys_array.collect{ |key| Metric.by_name(key).id if Metric.by_name(key) }
  end

  def self.delete_with_manual_measures(id)
    ManualMeasure.delete_all(["metric_id = ?", id])
    self.deactivate(id)
  end

  def self.deactivate(id)
    metric = by_id(id)
    metric.enabled = false
    metric.save!
  end

  def to_hash_json(options={})
    return {'key' => name, 'name' => short_name, 'description' => description, 'domain' => domain,
      'qualitative' => qualitative, 'user_managed' => self.user_managed,
      'direction' => direction.to_i, 'val_type' => val_type.to_s, 'hidden' => hidden}
  end

  def treemap_color?
    enabled && !hidden && ((numeric? && worst_value && best_value) || val_type==Metric::VALUE_TYPE_LEVEL)
  end

  def treemap_size?
    enabled && !hidden && numeric? && !domain.blank?
  end

  # temporary method since 2.7. Will replace it by a field in database.
  def on_new_code?
    @on_new_code ||=
      begin
        key.start_with?('new_')
      end
  end

  def to_xml(options={})
    xml = Builder::XmlMarkup.new
    xml.metric do
      xml.key(name)
      xml.name(short_name)
      xml.description(description)
      xml.domain(domain)
      xml.qualitative(qualitative)
      xml.direction(direction)
      xml.user_managed(self.user_managed)
      xml.val_type(val_type)
      xml.hidden(hidden)
    end
  end

  HUMANIZED_ATTRIBUTES = {
      :name => "key",
      :short_name => "name",
  }

  def self.human_attribute_name(attr)
    HUMANIZED_ATTRIBUTES[attr.to_sym] || super
  end

  # METRIC DEFINITIONS
  # WARNING if you edit this file do not forget to also change sonar-commons/src/main/java/ch/hortis/sonar/model/Metrics.java
  LINES = 'lines'
  NCLOC = 'ncloc'
  CLASSES = 'classes'
  PACKAGES = 'packages'
  FILES = 'files'
  FUNCTIONS = 'functions'
  DIRECTORIES = 'directories'
  ACCESSORS = 'accessors'
  PUBLIC_API = 'public_api'

  COMPLEXITY = 'complexity'
  STATEMENTS = 'statements'
  AVG_CMPX_BY_CLASS = 'class_complexity'
  AVG_CMPX_BY_FUNCTION = 'function_complexity'
  AVG_CMPX_BY_FILE = 'file_complexity'
  CLASSES_CMPX_DISTRIBUTION = 'class_complexity_distribution'
  FUNCTIONS_CMPX_DISTRIBUTION = 'function_complexity_distribution'

  DUPLICATED_FILES = 'duplicated_files'
  DUPLICATED_LINES = 'duplicated_lines'
  DUPLICATED_BLOCKS = 'duplicated_blocks'
  DUPLICATED_LINES_DENSITY = 'duplicated_lines_density'

  TESTS = 'tests'
  TEST_ERRORS = 'test_errors'
  SKIPPED_TESTS = 'skipped_tests'
  TEST_FAILURES = 'test_failures'
  TEST_EXECUTION_TIME = 'test_execution_time'
  TEST_SUCCESS_DENSITY = 'test_success_density'
  COVERAGE = 'coverage'
  LINE_COVERAGE = 'line_coverage'
  BRANCH_COVERAGE = 'branch_coverage'
  UNCOVERED_LINES='uncovered_lines'
  UNCOVERED_CONDITIONS='uncovered_conditions'

  VIOLATIONS = 'violations'
  BLOCKER_VIOLATIONS='blocker_violations'
  CRITICAL_VIOLATIONS='critical_violations'
  MAJOR_VIOLATIONS='major_violations'
  MINOR_VIOLATIONS='minor_violations'
  INFO_VIOLATIONS='info_violations'

  TECH_DEBT='sqale_index'

  MAINTAINABILITY='maintainability'
  EFFICIENCY='efficiency'
  RELIABILITY='reliability'
  USABILITY='usability'
  PORTABILITY='portability'

  COMMENT_LINES_DENSITY = 'comment_lines_density'
  COMMENT_LINES = 'comment_lines'
  PUBLIC_DOCUMENTED_API_DENSITY = 'public_documented_api_density'
  PUBLIC_UNDOCUMENTED_API = 'public_undocumented_api'
  COMMENTED_OUT_CODE_LINES = 'commented_out_code_lines'

  ALERT_STATUS = 'alert_status'
  QUALITY_GATE_DETAILS = 'quality_gate_details'
  PROFILE='profile'
  QUALITY_PROFILES='quality_profiles'

  private

  def self.cache
    c = Caches.cache(CACHE_KEY)
    if c.size==0
      metrics=Metric.find(:all, :conditions => {:enabled => true})
      metrics.each do |metric|
        c[metric.name]=metric
        c[metric.id.to_s]=metric
      end
      c['ALL']=metrics
    end
    c
  end

  def self.i18n_domain_cache
    c = Caches.cache(I18N_DOMAIN_CACHE_KEY)
    if c.size==0
      domains(false).each do |domain|
        locale_map={}
        c[domain]=locale_map
      end
    end
    c
  end

  def self.i18n_short_name_cache
    c = Caches.cache(I18N_SHORT_NAME_CACHE_KEY)
    if c.size==0
      all.each do |metric|
        locale_map={}
        c[metric.name]=locale_map
      end
    end
    c
  end
end
