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
class Rule < ActiveRecord::Base

  validates_presence_of :name, :rules_category_id, :plugin_rule_key, :plugin_config_key, :plugin_name

  has_many :rules_parameters
  belongs_to :rules_category
  has_many :rule_failures
  has_many :active_rules
  belongs_to :parent, :class_name => 'Rule', :foreign_key => 'parent_id'

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

  def rules_category
    category
  end

  def category
    @rules_category ||=
      begin
      RulesCategory.by_id(rules_category_id)
    end
  end

  def category=(c)
    @rules_category = c
  end

  def key
    "#{plugin_name}:#{plugin_rule_key}"
  end

  def template?
    cardinality=='MULTIPLE'
  end

  def editable?
    !parent_id.nil?
  end

  def <=>(rule)
    name<=>rule.name
  end

  def self.to_i(key_or_id)
    id=key_or_id.to_i
    if id<=0 && key_or_id
      parts=key_or_id.split(':')
      if parts.size==2
        rule=Rule.find(:first, :conditions => {:plugin_name => parts[0], :plugin_rule_key => parts[1]})
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
          rule=Rule.find(:first, :conditions => {:plugin_name => parts[0], :plugin_rule_key => parts[1]})
        end
      else
        rule=Rule.find(id)
      end
    end
    rule
  end

  def to_hash_json(profile)
    json = {'title' => name, 'key' => key, 'category' => rules_category.name, 'plugin' => plugin_name}
    json['description'] = description
    active_rule = nil
    if profile
      active_rule = profile.active_by_rule_id(id)
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
    json['params'] = parameters.collect{|parameter| parameter.to_hash_json(active_rule)} if not parameters.empty?
    json
  end

  def to_xml(profile)
    xml = Builder::XmlMarkup.new
    xml.rule do
      xml.title(name)
      xml.key(key)
      xml.category(rules_category.name)
      xml.plugin(plugin_name)
      xml.description {xml.cdata!(description)}
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
        xml << parameter.to_xml(active_rule)
      end
    end
  end

  def to_csv(profile)
    csv = [name.strip, plugin_rule_key, rules_category.name, plugin_name]
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


  # options :language => nil, :categories => [], :plugins => [], :searchtext => '', :profile => nil, :priorities => [], :status =>
  def self.search(java_facade, options={})
    conditions = ['enabled=:enabled']
    values = {:enabled => true}

    plugins=nil
    if remove_blank(options[:plugins])
      plugins = options[:plugins]
      unless options[:language].blank?
        plugins = plugins & java_facade.getRuleRepositoriesByLanguage(options[:language]).collect{ |repo| repo.getKey() }
      end
    elsif !options[:language].blank?
      plugins = java_facade.getRuleRepositoriesByLanguage(options[:language]).collect{ |repo| repo.getKey() }
    end

    if plugins
      if plugins.empty?
        conditions << "plugin_name IS NULL"
      else
        conditions << "plugin_name IN (:plugin_names)"
        values[:plugin_names] = plugins
      end
    end

    if remove_blank(options[:categories])
      conditions << "rules_category_id IN (:category_ids)"
      values[:category_ids] = RulesCategory.find(:all, :select => 'id', :conditions => { :name => options[:categories] }).map(&:id)
    end

    unless options[:searchtext].blank?
      conditions << "(UPPER(rules.name) LIKE :searchtext OR plugin_rule_key = :key)"
      searchtext = options[:searchtext].to_s.strip
      values[:searchtext] = "%" << searchtext.clone.upcase << "%"
      values[:key] = searchtext
    end

    includes=(options[:include_parameters] ? :rules_parameters : nil)
    rules = Rule.find(:all, :order => 'rules.name', :include => includes,
      :conditions => [conditions.join(" AND "), values])

    filter(rules, options)
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
    if profile
      inactive = (options[:status]=='INACTIVE')
      active = (options[:status]=='ACTIVE')

      rules = rules.reject do |rule|
        active_rule = profile.active_by_rule_id(rule.id)
        ((inactive and active_rule) or (active and active_rule.nil?))
      end

      if priorities
        rules = rules.select do |rule|
          active_rule = profile.active_by_rule_id(rule.id)
          (active_rule and priorities.include?(active_rule.priority_text)) or (active_rule.nil? and priorities.include?(rule.priority_text))
        end
      end

    elsif priorities
      rules = rules.select do |rule|
        priorities.include?(rule.priority_text)
      end
    end
    rules
  end


end
