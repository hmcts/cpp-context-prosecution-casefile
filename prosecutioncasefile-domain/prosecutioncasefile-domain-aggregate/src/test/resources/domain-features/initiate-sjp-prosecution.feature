Feature: Initiate SJP Prosecution


#  Scenario: Receive a valid SJP case

  #  Given no previous events
  #  When you receiveSjpProsecution on a ProsecutionCaseFile using a initiate sjp prosecution
  #  Then sjp prosecution received


  Scenario: Associate enterpriseId to a received-case and initialise it

    Given sjp prosecution received
    When you associateEnterpriseId on a ProsecutionCaseFile using a associate enterprise id
    Then sjp case initiated


  Scenario: Case is accepted

    Given sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully

  Scenario:  No event occurs when no case creation via prosecution case file

    Given no previous events
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then no events occurred

#  Scenario: Duplicate case is rejected

  #  Given sjp prosecution received
  #  And sjp case created successfully
  #  When you receiveSjpProsecution on a ProsecutionCaseFile using a initiate sjp prosecution
  #  Then sjp prosecution rejected as duplicate


 # Scenario: Prosecution is rejected

  #  Given no previous events
  #  When you receiveSjpProsecution on a ProsecutionCaseFile using a invalid sjp prosecution
  #  Then sjp prosecution rejected

 # Scenario: Prosecution offence code is rejected

  #  Given no previous events
  #  When you receiveSjpProsecution on a ProsecutionCaseFile using a sjp prosecution with invalid offence code
  #  Then sjp prosecution offence code rejected


#  Scenario: Prosecution offence location is missing

 #   Given no previous events
 #   When you receiveSjpProsecution on a ProsecutionCaseFile using a sjp prosecution with missing offence location
 #   Then sjp prosecution offence location rejected

  Scenario: Pending material gets processed on case creation accepted

    Given sjp notice pending
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using a accept case
    Then sjp case created successfully
    And sjp notice added

  #Scenario: Prosecution nationalities code are invalid

  #  Given no previous events
  #  When you receiveSjpProsecution on a ProsecutionCaseFile using a sjp prosecution with invalid nationalities
  #  Then sjp prosecution nationalities rejected

  #Scenario: Prosecution nationalities code are valid

  #  Given no previous events
  #  When you receiveSjpProsecution on a ProsecutionCaseFile using a sjp prosecution with valid nationalities
  #  Then sjp prosecution nationalities accepted


#  Scenario: Prosecution is received with warning when defendant was under 18 on charge date

 #   Given no previous events
  #  When you receiveSjpProsecution on a ProsecutionCaseFile using a sjp prosecution with defendant under 18 on charge date
  #  Then sjp prosecution received with warning defendant under 18


  Scenario: Case is initiated with warning when defendant was under 18 on charge date

    Given sjp prosecution received with warning defendant under 18
    When you associateEnterpriseId on a ProsecutionCaseFile using an associate enterprise id
    Then sjp case initiated with warning defendant under 18

  Scenario: Case is created with warning when defendant was under 18 on charge date

    Given sjp prosecution received with warning defendant under 18
    And sjp case initiated with warning defendant under 18
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully with warning defendant under 18

#  Scenario: Case is created with warning when offence out of time

 #   Given no previous events
 #   When you receiveSjpProsecution on a ProsecutionCaseFile using a sjp prosecution with offence out of time
 #   Then sjp prosecution received with warning offence out of time