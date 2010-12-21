#
# Sonar, open source software quality management tool.
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
class Profile < ActiveRecord::Base
  set_table_name 'rules_profiles'

  has_many :alerts, :dependent => :delete_all
  has_many :active_rules, :class_name => 'ActiveRule', :foreign_key => 'profile_id', :dependent => :destroy, :include => ['rule']
  has_many :projects, :order => 'name asc'
  has_many :active_rules_with_params, :class_name => 'ActiveRule', :foreign_key => 'profile_id',
      :include => ['active_rule_parameters']

  validates_uniqueness_of :name, :scope => :language, :case_sensitive => false
  validates_presence_of :name
  
  DEFAULT_PROFILE_NAME = 'Sun checks'
  
  def active?
    active
  end
  
  def key
    "#{language}_#{name}"
  end
  
  def provided?
    provided
  end
  
  def validate_copy(name)
    new_rule_profile = RulesProfile.new(:name => name, :provided => false, :default_profile => false, :language => language)
    new_rule_profile.valid?
    new_rule_profile.errors
  end

  def self.find_by_name_and_language(name, language)
    Profile.find(:first, :conditions => {:name => name, :language => language})
  end

  def self.find_active_profile_by_language(language)
    Profile.find(:first, :conditions => {:default_profile => true, :language => language})
  end

  def self.default_profile
    Profile.find(:first, :conditions => {:default_profile => true})
  end

  def set_as_default
    default_profile=nil
    Profile.find(:all, :conditions => {:language => language}).each do |profile|
      if profile.id==id
        profile.default_profile=true
        default_profile=profile
      else
        profile.default_profile=false
      end
      profile.save
    end
    self
  end

  def active_by_rule_id(rule_id)
    active_hash_by_rule_id[rule_id]
  end

  def self.options_for_select
    array=[]
    RulesProfile.find(:all, :order => 'name').each do |profile|
      label = profile.name
      label = label + ' (active)' if profile.default_profile?
      array<<[label, profile.id]
    end
    array
  end

  @active_hash_by_rule_id=nil
  def active_hash_by_rule_id
    if @active_hash_by_rule_id.nil?
      @active_hash_by_rule_id={}
      active_rules_with_params.each do |active_rule|
        @active_hash_by_rule_id[active_rule.rule_id]=active_rule
      end
    end
    @active_hash_by_rule_id
  end
end
