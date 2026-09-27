package `in`.tatkalfast.assistant
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import `in`.tatkalfast.assistant.ui.*
import `in`.tatkalfast.domain.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class) class PreparationUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun savesValidPassenger() {
  var result:Passenger?=null
  compose.setContent {MaterialTheme {PassengerEditor(Passenger(),Words(false),{}) {result=it}}}
  compose.onNodeWithText("Passenger name").performTextInput("Example Passenger")
  compose.onNodeWithText("Save").performScrollTo().performClick()
  compose.runOnIdle {assertEquals("Example Passenger",result?.name)}
 }
 @Test fun invalidNameCannotSave() {
  var saved=false
  compose.setContent {MaterialTheme {PassengerEditor(Passenger(),Words(false),{}) {saved=true}}}
  compose.onNodeWithText("Save").performScrollTo().performClick()
  compose.runOnIdle {assertFalse(saved)}
  compose.onNodeWithText("Enter a valid passenger name (2–60 letters).").assertExists()
 }
 @Test fun savesPreparedJourneyWithSelectedPassenger() {
  val p=Passenger(name="Example Passenger")
  val date=LocalDate.now(Rules.zone).plusDays(1).toString()
  val j=Journey(from="TJ",to="MS",boarding="TJ",date=date,originDate=date,train="16866",mobile="9000000000",passengerIds=listOf(p.id))
  var result:Journey?=null
  compose.setContent {MaterialTheme {JourneyEditor(j,listOf(p),Words(false),{}, {result=it},{})}}
  compose.onNodeWithText("Save").performScrollTo().performClick()
  compose.runOnIdle {assertEquals(j,result)}
 }
 @Test fun tamilEditorLabels() {
  compose.setContent {MaterialTheme {PassengerEditor(Passenger(),Words(true),{}) {}}}
  compose.onNodeWithText("பயணியின் பெயர்").assertExists()
 }
}
