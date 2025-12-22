Feature: Record Case Upload Document

  Scenario: Record material upload

    Given sjp prosecution received
    When you recordUploadCaseDocument on a ProsecutionCaseFile using a record upload case document
    Then upload case document recorded


  Scenario: No event occurs when no case creation via prosecution case file

     Given no previous events
     When you recordUploadCaseDocument on a ProsecutionCaseFile using a record upload case document
     Then no events occurred