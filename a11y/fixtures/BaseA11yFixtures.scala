
package fixtures

import models.{Sector, SicCode, SicCodeChoice}

trait BaseA11yFixtures {

  val testJourneyId = "testJourneyId"
  val testSicDescription = "testSicDescription"
  val testCode = "12345"
  val testIndex = "testIndex"
  val testSicCode: SicCode = SicCode(testCode, testSicDescription)
  val testSectorName = "testSectorName"
  val testSector: Sector = Sector(testCode, testSectorName, testSectorName, 1)
  val testSicCodeChoice: SicCodeChoice = SicCodeChoice(testCode, testSicDescription, testSicDescription, List(testIndex))

}
