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
# License along with {library}; if not, write to the Free Software
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

class Sonar::RulePriority

  INFO = Java::OrgSonarApiRules::RulePriority::INFO
  MINOR = Java::OrgSonarApiRules::RulePriority::MINOR
  MAJOR = Java::OrgSonarApiRules::RulePriority::MAJOR
  CRITICAL = Java::OrgSonarApiRules::RulePriority::CRITICAL
  BLOCKER = Java::OrgSonarApiRules::RulePriority::BLOCKER

  PRIORITY_INFO = Java::OrgSonarApiRules::RulePriority::INFO.ordinal()
  PRIORITY_MINOR = Java::OrgSonarApiRules::RulePriority::MINOR.ordinal()
  PRIORITY_MAJOR = Java::OrgSonarApiRules::RulePriority::MAJOR.ordinal()
  PRIORITY_CRITICAL = Java::OrgSonarApiRules::RulePriority::CRITICAL.ordinal()
  PRIORITY_BLOCKER = Java::OrgSonarApiRules::RulePriority::BLOCKER.ordinal()

  def self.to_s(failure_level, translate=false)
    text = case failure_level
      when Java::OrgSonarApiRules::RulePriority::BLOCKER.ordinal()
        Java::OrgSonarApiRules::RulePriority::BLOCKER.to_s
      when Java::OrgSonarApiRules::RulePriority::CRITICAL.ordinal()
        Java::OrgSonarApiRules::RulePriority::CRITICAL.to_s
      when Java::OrgSonarApiRules::RulePriority::MAJOR.ordinal()
        Java::OrgSonarApiRules::RulePriority::MAJOR.to_s
      when Java::OrgSonarApiRules::RulePriority::MINOR.ordinal()
        Java::OrgSonarApiRules::RulePriority::MINOR.to_s
      when Java::OrgSonarApiRules::RulePriority::INFO.ordinal()
        Java::OrgSonarApiRules::RulePriority::INFO.to_s
    end

    return text unless translate
    
    i18n_key = 'severity.' + text
    result = Api::Utils.message(i18n_key, :default => as_text_map[text])
    result
  end
  
  def self.id(priority)
    begin
      javaPriority=Java::OrgSonarApiRules::RulePriority.valueOf(priority)
      javaPriority && javaPriority.ordinal
    rescue
      nil
    end
  end

  def self.as_array
    @@priorities_a ||= []
    return @@priorities_a if @@priorities_a.size > 0
    @@priorities_a << PRIORITY_INFO
    @@priorities_a << PRIORITY_MINOR
    @@priorities_a << PRIORITY_MAJOR
    @@priorities_a << PRIORITY_CRITICAL
    @@priorities_a << PRIORITY_BLOCKER
  end
  
  def self.info?(priority)
    priority==PRIORITY_INFO
  end

  def self.minor?(priority)
    priority==PRIORITY_MINOR
  end
  
  def self.major?(priority)
    priority==PRIORITY_MAJOR
  end
  
  def self.critical?(priority)
    priority==PRIORITY_CRITICAL
  end
  
  def self.blocker?(priority)
    priority==PRIORITY_BLOCKER
  end
  
  @@priority_text_map={}
  
  def self.as_text_map
    return @@priority_text_map if @@priority_text_map.size > 0
    @@priority_text_map[to_s(PRIORITY_INFO)]='Info'
    @@priority_text_map[to_s(PRIORITY_MINOR)]='Minor'
    @@priority_text_map[to_s(PRIORITY_MAJOR)]='Major'
    @@priority_text_map[to_s(PRIORITY_CRITICAL)]='Critical'
    @@priority_text_map[to_s(PRIORITY_BLOCKER)]='Blocker'
    @@priority_text_map    
  end
  
  def self.as_options
    @@priority_options ||= []
    return @@priority_options if @@priority_options.size > 0
    @@priority_options << [as_text_map[to_s(PRIORITY_INFO)], to_s(PRIORITY_INFO)]
    @@priority_options << [as_text_map[to_s(PRIORITY_MINOR)], to_s(PRIORITY_MINOR)]
    @@priority_options << [as_text_map[to_s(PRIORITY_MAJOR)], to_s(PRIORITY_MAJOR)]
    @@priority_options << [as_text_map[to_s(PRIORITY_CRITICAL)], to_s(PRIORITY_CRITICAL)]
    @@priority_options << [as_text_map[to_s(PRIORITY_BLOCKER)], to_s(PRIORITY_BLOCKER)]
    @@priority_options    
  end
end
