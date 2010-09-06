#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
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

  TYPE_LEVEL_OK = 'OK'
  TYPE_LEVEL_WARN = 'WARN'
  TYPE_LEVEL_ERROR = 'ERROR'

  ORIGIN_GUI='GUI'
  ORIGIN_JAVA='JAV'

  CACHE_KEY='metrics'
  
  validates_length_of       :name, :within => 1..64
  validates_uniqueness_of   :name
  validates_length_of       :short_name, :within => 1..64
  validates_inclusion_of    :val_type, :in => [VALUE_TYPE_INT,VALUE_TYPE_BOOLEAN,VALUE_TYPE_FLOAT,VALUE_TYPE_PERCENT,VALUE_TYPE_STRING,VALUE_TYPE_MILLISEC,VALUE_TYPE_LEVEL, VALUE_TYPE_DATA, VALUE_TYPE_DISTRIB], :message => "wrong value type"


  @@metrics_type_names = { VALUE_TYPE_INT => 'Integer', VALUE_TYPE_FLOAT => 'Float', VALUE_TYPE_PERCENT => 'Percent',
    VALUE_TYPE_BOOLEAN => 'Yes/No', VALUE_TYPE_STRING => 'Text', VALUE_TYPE_LEVEL => 'Level' }

  attr_accessible :name, :description, :direction, :domain, :short_name, :qualitative, :val_type, :user_managed

  def ==(other)
    name==other.name
  end

  def <=>(other)
    short_name<=>other.short_name
  end

  def self.domains
    all.collect{|metric|
      metric.domain
    }.compact.uniq.sort
  end

  def key
    name
  end

  def user_managed?
    user_managed==true
  end

  def value_type
    val_type
  end

  def numeric?
    val_type==VALUE_TYPE_INT || val_type==VALUE_TYPE_FLOAT || val_type==VALUE_TYPE_PERCENT || val_type==VALUE_TYPE_MILLISEC
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
    (!data?) && (!hidden) && (key!=Metric::ALERT_STATUS)
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

  def self.by_names(names)
    metrics = names.map do |name|
      by_name(name)
    end
    metrics
  end

  def self.by_domain(domain)
    all.select{|metric| metric.domain==domain}.sort
  end

  def self.major_metrics_id
    all.collect {|m| m.id}
  end

  def self.clear_cache
    Caches.clear(CACHE_KEY)
  end

  def self.default_time_machine_metrics
    [COMPLEXITY, COVERAGE, VIOLATIONS_DENSITY]
  end

  def self.review_types
    all.select{|metric| metric.user_managed?}
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

  def to_hash_json(options={})
    return {'key' => name, 'name' => short_name, 'description' => description, 'domain' => domain,
      'qualitative' => qualitative, 'user_managed' => self.user_managed,
      'direction' => direction.to_i, 'val_type' => val_type.to_s, 'hidden' => hidden}
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

  def created_online?
    origin==ORIGIN_GUI
  end

  def updatable_online?
    origin!=ORIGIN_JAVA
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
  UNCOVERED_CMPX_BY_TESTS = 'uncovered_complexity_by_tests'

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
  VIOLATIONS_DENSITY = 'violations_density'
  WEIGHTED_VIOLATIONS = 'weighted_violations'
  BLOCKER_VIOLATIONS='blocker_violations'
  CRITICAL_VIOLATIONS='critical_violations'
  MAJOR_VIOLATIONS='major_violations'
  MINOR_VIOLATIONS='minor_violations'
  INFO_VIOLATIONS='info_violations'

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
  PROFILE='profile'

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

end