Feature: Cinema ticket booking
  As a cinema customer
  I want to book tickets for an account
  So that I pay the right amount and get the right number of seats

  Ticket prices: INFANT £0 (no seat, sits on an adult's lap), CHILD £17.50, ADULT £25.99.
  A booking may contain at most 25 tickets (infants included).

  Scenario: Adult booking
    Given I am account 1234
    And I want 1 ADULT tickets
    When I submit the booking
    Then the booking is created
    And the total cost is 25.99
    And 1 seats are reserved

  Scenario: Infants are free and do not get a seat
    Given I am account 1234
    And I want 2 ADULT tickets
    And I want 2 CHILD tickets
    And I want 1 INFANT tickets
    When I submit the booking
    Then the booking is created
    And the total cost is 86.98
    And 4 seats are reserved
    And account 1234 is charged 86.98

  Scenario Outline: Invalid bookings are rejected and nothing is paid for or reserved
    Given I am account <account>
    And I want <adults> ADULT tickets
    And I want <children> CHILD tickets
    And I want <infants> INFANT tickets
    When I submit the booking
    Then the booking is rejected with the message "<message>"
    And no payment is taken and no seats are reserved

    Examples:
      | account | adults | children | infants | message                                                             |
      | 1234    | 26     | 0        | 0       | A maximum of 25 tickets can be purchased at a time                  |
      | 1234    | 13     | 0        | 13      | A maximum of 25 tickets can be purchased at a time                  |
      | 1234    | 0      | 1        | 0       | Child and infant tickets cannot be purchased without an adult ticket|
      | 1234    | 0      | 0        | 1       | Child and infant tickets cannot be purchased without an adult ticket|
      | 1234    | 1      | 0        | 2       | Infants sit on an adult's lap, so infants cannot outnumber adults   |
      | 1234    | 0      | 0        | 0       | At least one ticket must be purchased                               |
      | 0       | 1      | 0        | 0       | Account id must be greater than zero                                |
      | -1      | 1      | 0        | 0       | Account id must be greater than zero                                |

  Scenario: Booking without any ticket requests is rejected
    Given I am account 1234
    When I submit the booking
    Then the booking is rejected with the message "At least one ticket request is required"
    And no payment is taken and no seats are reserved

  Scenario: Malformed request is rejected
    When I submit a malformed booking request
    Then the booking is rejected as a bad request
    And no payment is taken and no seats are reserved