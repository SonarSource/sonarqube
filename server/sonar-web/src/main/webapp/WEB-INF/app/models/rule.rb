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
class Rule < ActiveRecord::Base

  MANUAL_REPOSITORY_KEY = 'manual'

  STATUS_READY = "READY"
  STATUS_BETA = "BETA"
  STATUS_DEPRECATED = "DEPRECATED"
  STATUS_REMOVED = "REMOVED"

  SORT_BY_RULE_NAME = "SORT_BY_RULE_NAME"
  SORT_BY_CREATION_DATE = "SORT_BY_CREATION_DATE"

  validates_presence_of :name, :description, :plugin_name
  validates_presence_of :plugin_rule_key, :if => 'name.present?'
  validates_uniqueness_of :name

  has_many :rules_parameters, :inverse_of => :rule
  has_many :active_rules, :inverse_of => :rule
  belongs_to :parent, :class_name => 'Rule', :foreign_key => 'parent_id'

  def repository_key
    plugin_name
  end

  def parameters
    rules_parameters
  end

  def parameter(name)
    result=nil
    parameters.each do |param|
      result = param if param.name==name
    end
    result
  end

  def priority_text
    Sonar::RulePriority.to_s(priority)
  end

  def key
    "#{plugin_name}:#{plugin_rule_key}"
  end

  def template?
    is_template
  end

  def ready?
    status == STATUS_READY
  end

  def editable?
    !parent_id.nil?
  end

  def enabled?
    !removed?
  end

  def disabled?
    removed?
  end

  def removed?
    status == STATUS_REMOVED
  end

  def deprecated?
    status == STATUS_DEPRECATED
  end

  def beta?
    status == STATUS_BETA
  end

  def <=>(other)
    return -1 if other.nil?
    return 1 if other.name.nil?
    name.downcase<=>other.name.downcase
  end

  def name(unused_deprecated_l10n=true)
    @raw_name ||=
      begin
        # SONAR-4583
        # name should return an empty string instead of nil
        read_attribute(:name) || ''
      end
  end

  def name=(value)
    write_attribute(:name, value)
  end

  def description
    @raw_description ||=
      begin
        # SONAR-4583
        # description should return an empty string instead of nil
        read_attribute(:description) || ''
      end
  end

  def description=(value)
    write_attribute(:description, value)
  end

  def config_key
    plugin_config_key
  end

  def self.to_i(key_or_id)
    id=key_or_id.to_i
    if id<=0 && key_or_id
      parts=key_or_id.split(':')
      if parts.size==2
        rule=Rule.first(:conditions => {:plugin_name => parts[0], :plugin_rule_key => parts[1]})
        id=rule.id if rule
      end
    end
    id>0 ? id : nil
  end

  def self.by_key_or_id(key_or_id)
    rule=nil
    if key_or_id.present?
      id=key_or_id.to_i
      if id<=0
        parts=key_or_id.split(':')
        if parts.size==2
          rule=Rule.first(:conditions => {:plugin_name => parts[0], :plugin_rule_key => parts[1]})
        end
      else
        rule=Rule.find(id)
      end
    end
    rule
  end

  def self.manual_rules
    rules = Rule.all(:conditions => ['status=? and plugin_name=?', STATUS_READY, MANUAL_REPOSITORY_KEY])
    Api::Utils.insensitive_sort(rules) { |rule| rule.name }
  end

  def self.manual_rule(id)
    Rule.first(:conditions => ['status=? and plugin_name=? and id=?', STATUS_READY, MANUAL_REPOSITORY_KEY, id])
  end

  def self.create_manual_rule(options)
    key = options[:name].strip.downcase.gsub(/\s/, '_')
    creation_options = {:name => options[:name],
                        :description => options[:description],
                        :plugin_rule_key => key,
                        :status => STATUS_READY,
                        :is_template => false,
                        :plugin_name => MANUAL_REPOSITORY_KEY}
    Rule.create!(creation_options)
  end

  def self.to_hash(java_rule)
    l10n_name = java_rule.getName()
    l10n_desc = java_rule.getDescription()
    hash = {:key => java_rule.ruleKey().toString()}
    hash[:name] = l10n_name if l10n_name
    hash[:desc] = l10n_desc if l10n_desc
    hash[:status] = java_rule.getStatus() if java_rule.getStatus()
    hash
  end

  def to_hash_json(profile, active_rule = nil)
    json = {'title' => name, 'key' => key, 'plugin' => plugin_name, 'config_key' => config_key}
    json['description'] = description if description
    if profile
      active_rule = active_rule || profile.active_by_rule_id(id)
      if active_rule
        json['priority'] = active_rule.priority_text
        json['status'] = 'ACTIVE'
      else
        json['priority'] = priority_text
        json['status'] = 'INACTIVE'
      end
    else
      json['priority'] = priority_text
    end
    json['params'] = parameters.collect { |parameter| parameter.to_hash_json(active_rule) } unless parameters.empty?
    json
  end

  def to_xml(profile, xml)
    xml.rule do
      xml.title(name)
      xml.key(key)
      xml.config_key(config_key)
      xml.plugin(plugin_name)
      xml.description { xml.cdata!(description) } if description
      active_rule = nil
      if profile
        active_rule = profile.active_by_rule_id(id)
        if active_rule
          xml.priority(active_rule.priority_text)
          xml.status('ACTIVE')
        else
          xml.priority(priority_text)
          xml.status("INACTIVE")
        end
      else
        xml.priority(priority_text)
      end
      parameters.each do |parameter|
        parameter.to_xml(active_rule, xml)
      end
    end
  end

  def to_csv(profile)
    csv = [name.strip, plugin_rule_key, plugin_name]
    if profile
      active_rule = profile.active_by_rule_id(id)
      if active_rule
        csv << active_rule.priority_text
        csv << 'ACTIVE'
      else
        csv << priority_text
        csv << 'INACTIVE'
      end
    end
    csv
  end

  def self.remove_blank(array)
    if array
      array = array - ['']
      array.empty? ? nil : array
    else
      nil
    end
  end

  def self.filter(rules, options)
    priorities = remove_blank(options[:priorities])
    profile = options[:profile]
    inheritance = options[:inheritance]

    if profile
      inactive = (options[:activation]=='INACTIVE')
      active = (options[:activation]=='ACTIVE')

      rules = rules.reject do |rule|
        active_rule = profile.active_by_rule_id(rule.id)
        ((inactive && active_rule) || (active && active_rule.nil?))
      end

      if priorities
        rules = rules.select do |rule|
          active_rule = profile.active_by_rule_id(rule.id)
          (active_rule && priorities.include?(active_rule.priority_text)) || (active_rule.nil? && priorities.include?(rule.priority_text))
        end
      end

      if inheritance=='NOT'
        rules = rules.select do |rule|
          active_rule = profile.active_by_rule_id(rule.id)
          (active_rule.nil? || active_rule.inheritance.blank?)
        end
      elsif inheritance.present? && inheritance != 'any'
        rules = rules.select do |rule|
          active_rule = profile.active_by_rule_id(rule.id)
          (active_rule && active_rule.inheritance==inheritance)
        end
      end

    elsif priorities
      rules = rules.select do |rule|
        priorities.include?(rule.priority_text)
      end
    end
    rules
  end

  def self.sort_by(rules, sort_by)
    case sort_by
      when SORT_BY_CREATION_DATE
        rules = rules.sort_by { |rule| rule.created_at }.reverse
      else
        rules = rules.sort
    end
    rules
  end

end
