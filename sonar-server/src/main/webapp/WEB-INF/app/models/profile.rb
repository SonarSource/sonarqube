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

  validates_uniqueness_of :name, :scope => :language, :case_sensitive => false
  validates_presence_of :name
  validates_exclusion_of :name, :in => %w( active ), :message => "reserved"

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

  def active_rules_by_category_and_level(categ_id=nil, level=nil)
    result=[]
    active_rules.each do |active_rule|
      if categ_id.nil?
        result<<active_rule if active_rule.level==level or level.nil?
      elsif level.nil?
        result<<active_rule if active_rule.rule.rules_category_id==categ_id
      else
        result<<active_rule if active_rule.level==level and active_rule.rule.rules_category_id==categ_id
      end
    end
    result
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
end
