package `in`.tatkalfast.assistant
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class) class BackupScreenTest {
 @get:Rule val compose=createAndroidComposeRule<BackupActivity>()
 @Test fun backupRequiresUnlockBeforeExposingPassphraseControls() {
  compose.onNodeWithText("Unlock saved details").assertIsDisplayed()
  compose.onNodeWithText("Backup passphrase (12+ characters)").assertDoesNotExist()
 }
}
