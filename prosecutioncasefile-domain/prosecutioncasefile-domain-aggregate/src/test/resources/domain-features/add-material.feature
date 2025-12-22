Feature: Add Material

  Scenario: Add SJP Notice when no case received creates a material pending event

    Given no previous events
    When you addMaterial on a ProsecutionCaseFile using a sjp notice
    Then sjp notice pending add material

  Scenario: Add previous convictions when case received, but not yet created creates a material pending event

    Given sjp prosecution received
    When you addMaterial on a ProsecutionCaseFile using a previous convictions
    Then previous convictions pending add material

  Scenario: Creates a material pending event even though not a valid SJPN file
  type because rules should only be applied on MaterialAdded not MaterialPending events
    as we don't know which rules we need to apply until the case type is known via
  sjp case created successfully or similar event for other case types

    Given sjp prosecution received
    When you addMaterial on a ProsecutionCaseFile using a previous convictions with unsupported file type
    Then previous convictions pending with unsupported file type

  Scenario: Add SJP notice when case created

    Given sjp prosecution received
    And sjp case created successfully
    When you addMaterial on a ProsecutionCaseFile using a sjp notice
    Then sjp notice added with received date

  Scenario: Reject SJP material when file (MIME) type is invalid

    Given sjp prosecution received
    And sjp case created successfully
    When you addMaterial on a ProsecutionCaseFile using a sjp notice with unsupported file type
    Then sjp notice rejected with received date

  Scenario: Reject SJP material that is not an SJP notice or Previous Convictions

    Given sjp prosecution received
    And sjp case created successfully
    When you addMaterial on a ProsecutionCaseFile using a sjp notice with unsupported document type
    Then material rejected with unsupported document type


  Scenario: Add pending SJP Notice when case created

    Given sjp notice pending
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully, sjp notice added

  Scenario: Add pending Previous Convictions when case created

    Given previous convictions pending
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully, previous convictions added

  Scenario: Reject invalid pending SJPN Notice when case created

    Given sjp notice pending with unsupported file type
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully, sjp notice rejected

  Scenario: Reject invalid pending Previous Convictions when case created

    Given previous convictions pending with unsupported file type
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully, previous convictions rejected

  Scenario: Reject multiple invalid pending documents when case created

    Given previous convictions pending with unsupported file type
    And sjp notice pending with unsupported file type
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully, previous convictions rejected, sjp notice rejected

  Scenario: Add multiple valid pending documents when case created

    Given previous convictions pending
    And sjp notice pending
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully, previous convictions added, sjp notice added

  Scenario: Add valid pending document and reject invalid pending document when case created

    Given previous convictions pending
    And sjp notice pending with unsupported file type
    And sjp prosecution received
    When you acceptCase on a ProsecutionCaseFile using an accept case
    Then sjp case created successfully, previous convictions added, sjp notice rejected

  Scenario: Expire pending document

    Given sjp notice pending
    And sjp prosecution received
    When you expirePendingMaterial on a ProsecutionCaseFile using an expire pending sjpn
    Then sjp notice expired


  Scenario: Reject uploading a document when case is referred to open court

    Given sjp prosecution received
    And sjp case created successfully
    And record decision to refer case for court hearing saved
    When you addMaterial on a ProsecutionCaseFile using a sjp notice
    Then material rejected with case referred to open court
