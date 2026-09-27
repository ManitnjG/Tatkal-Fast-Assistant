package `in`.tatkalfast.assistant
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.tatkalfast.assistant.data.*
import `in`.tatkalfast.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class BookingViewModel @Inject constructor(private val store: VaultStore, private val settings: PreferenceStore) : ViewModel() {
 val vault=store.state
 val preferences=settings.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
 val error=MutableStateFlow<String?>(null)
 val busy=MutableStateFlow(false)
 val ready=MutableStateFlow(false)
 fun unlock() { if(!ready.value && !busy.value) action { store.load(); ready.value=true } }
 private fun action(block: suspend ()->Unit) {
  if(busy.value) return
  busy.value=true
  viewModelScope.launch { try { block() } catch(e: Exception) {
   if(e is kotlinx.coroutines.CancellationException) throw e
   error.value=when(e.message) { "unresolved_attempt", "invalid_pnr", "verify_history", "invalid_transition", "pending_note" -> e.message; else -> "storage_error" }
  } finally { busy.value=false } }
 }
 fun prefs(p: Preferences) = action { settings.save(p) }
 fun passenger(p: Passenger) = action { require(Rules.passenger(p).isEmpty()); store.update { it.copy(passengers=it.passengers.filterNot { x->x.id==p.id } + p) } }
 fun deletePassenger(id: String) = action { store.update { v->v.copy(passengers=v.passengers.filterNot { it.id==id },journeys=v.journeys.map { it.copy(passengerIds=it.passengerIds-id) }) } }
 fun journey(j: Journey) = action { require(Rules.journey(j,requireNotNull(vault.value).passengers).isEmpty()); store.update { it.copy(journeys=it.journeys.filterNot { x->x.id==j.id } + j,selectedJourney=j.id) } }
 fun select(id: String) = action { store.update { it.copy(selectedJourney=id) } }
 fun deleteJourney(id: String) = action { store.update { it.copy(journeys=it.journeys.filterNot { j->j.id==id },selectedJourney=it.selectedJourney.takeUnless { s->s==id }) } }
 fun payment(p: Payment) = action { store.update { it.copy(payment=p) } }
 fun prepare(j: Journey) = action { store.update { BookingEngine.start(it,j) } }
 fun handoff(id: String, open: ()->Unit) = action {
  store.update { v->v.copy(attempts=v.attempts.map { if(it.id==id) BookingEngine.handoff(it) else it }) }
  // Durable lock is committed BEFORE leaving the process. Even a failed launch remains conservative.
  try { open() } catch(e: Exception) { error.value="browser_error" }
 }
 fun change(id: String, transform: (Attempt)->Attempt) = action { store.update { v->v.copy(attempts=v.attempts.map { if(it.id==id) transform(it) else it }) } }
 fun networkUncertain(id: String) = change(id) { BookingEngine.recover(it) }
 fun clearError() { error.value=null }
}
