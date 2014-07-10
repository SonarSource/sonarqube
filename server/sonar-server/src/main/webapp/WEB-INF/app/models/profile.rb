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
class Profile < ActiveRecord::Base
  set_table_name 'rules_profiles'

  has_many :active_rules, :class_name => 'ActiveRule', :foreign_key => 'profile_id', :dependent => :destroy, :include => ['rule']
  has_many :active_rules_with_params, :class_name => 'ActiveRule', :foreign_key => 'profile_id', :include => ['active_rule_parameters']
  has_many :changes, :class_name => 'ActiveRuleChange', :dependent => :destroy

  validates_uniqueness_of :name, :scope => :language, :case_sensitive => false, :message => Api::Utils.message('quality_profiles.already_exists')
  validates_presence_of :name, :message => Api::Utils.message('quality_profiles.please_type_profile_name')

  MAX_NAME_LENGTH = 100
  validates_length_of :name, :maximum => MAX_NAME_LENGTH, :message => Api::Utils.message('name_too_long_x', :params => [MAX_NAME_LENGTH])

  # The warnings that are set on this record, equivalent to normal ActiveRecord errors but does not prevent
  # the record from saving.
  def warnings
    @warnings ||= ActiveRecord::Errors.new(self)
  end

  def warnings?
    !warnings.empty?
  end

  def notices
    @notices ||= ActiveRecord::Errors.new(self)
  end

  def notices?
    not notices.empty?
  end

  def active?
    active
  end

  def key
    "#{language}_#{name}"
  end

  def default_profile?
    Property.value("sonar.profile.#{language}")==name
  end

  def active_by_rule_id(rule_id)
    active_hash_by_rule_id[rule_id]
  end

  def self.options_for_select
    array=[]
    Profile.all(:order => 'name').each do |profile|
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

  def deletable?
    !default_profile? && children.empty?
  end

  def count_overriding_rules
    @count_overriding_rules||=
      begin
        active_rules.count(:conditions => ['inheritance=?', 'OVERRIDES'])
      end
  end

  def inherited?
    parent_kee.present?
  end

  def parent
    @parent||=
      begin
        if parent_kee.present?
          Profile.first(:conditions => ['language=? and kee=?', language, parent_kee])
        else
          nil
        end
      end
  end

  def children
    @children ||=
      begin
        Profile.all(:conditions => ['parent_kee=? and language=?', kee, language])
      end
  end

  def count_active_rules
    active_rules.select { |ar| ar.rule.enabled? }.size
  end

  def ancestors
    @ancestors ||=
      begin
        array=[]
        if parent
          array<<parent
          array.concat(parent.ancestors)
        end
        array
      end
  end

  def before_destroy
    raise 'This profile can not be deleted' unless deletable?
    Property.clear_for_resources("sonar.profile.#{language}", name)
  end

  def rename(new_name)
    old_name=self.name
    Profile.transaction do
      self.name=new_name
      if save
        Property.with_key("sonar.profile.#{language}").each do |prop|
          if prop.text_value==old_name
            prop.text_value=new_name
            prop.save
          end
        end
      end
    end
    self
  end

  def projects?
    !projects.empty?
  end

  def projects
    @projects ||=
      begin
        Project.all(:joins => 'LEFT JOIN properties ON properties.resource_id = projects.id',
                     :conditions => ['properties.resource_id is not null and properties.prop_key=? and properties.text_value like ?', "sonar.profile.#{language}", name])
      end
  end

  def add_project_id(project_id)
    Property.set("sonar.profile.#{language}", name, project_id)
    @projects = nil
  end

  def remove_projects
    Property.clear_for_resources("sonar.profile.#{language}", name)
    @projects = nil
  end

  def to_hash_json
    {
      :name => name,
      :language => language,
      :version => version,
      :rules => active_rules.map { |active| active.rule.to_hash_json(self, active) }
    }
  end

  def self.reset_default_profile_for_project_id(lang, project_id)
    Property.clear("sonar.profile.#{lang}", project_id)
  end

  def self.by_project_id(language, project_id, returns_default_if_nil=false)
    profile_name=Property.value("sonar.profile.#{language}", project_id)
    profile = (profile_name.present? ? Profile.find_by_name_and_language(profile_name, language) : nil)

    if !profile && returns_default_if_nil
      profile = by_default(language)
    end
    profile
  end

  def self.by_default(language)
    default_name = Property.value("sonar.profile.#{language}")
    default_name.present? ? Profile.first(:conditions => {:name => default_name, :language => language}) : nil
  end

  # Results are NOT sorted
  def self.all_by_language(language)
    Profile.all(:conditions => {:language => language})
  end

  def self.find_by_name_and_language(name, language)
    Profile.first(:conditions => {:name => name, :language => language})
  end
end
