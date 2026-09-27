package `in`.tatkalfast.domain
import org.junit.Assert.*
import org.junit.Test
class FillGroupingTest {
 private val n=FillCandidate("name",PreparedField.PASSENGER_NAME,"www.irctc.co.in","row1",true)
 private val age=FillCandidate("age",PreparedField.PASSENGER_AGE,n.host,n.group,false)
 @Test fun siblingNameAgeShareReview() {assertEquals(listOf("name","age"),FillGrouping.select(listOf(n,age)))}
 @Test fun otherPassengerRowExcluded() {assertEquals(listOf("name"),FillGrouping.select(listOf(n,age.copy(group="row2"))))}
 @Test fun ambiguousAgeExcluded() {assertEquals(listOf("name"),FillGrouping.select(listOf(n,age,age.copy(key="age2"))))}
 @Test fun ambiguousFocusedTypeUsesSingleField() {assertEquals(listOf("name"),FillGrouping.select(listOf(n,n.copy(key="name2",focused=false),age)))}
 @Test fun otherOriginExcluded() {assertEquals(listOf("name"),FillGrouping.select(listOf(n,age.copy(host="evil.test"))))}
 @Test fun contactPartitionNotMixedWithPassenger() {assertEquals(listOf("name"),FillGrouping.select(listOf(n,age.copy(field=PreparedField.MOBILE))))}
 @Test fun routeSiblingsGrouped() {assertEquals(2,FillGrouping.select(listOf(n.copy(field=PreparedField.FROM),age.copy(field=PreparedField.TO))).size)}
 @Test fun multipleFocusFailsClosed() {assertTrue(FillGrouping.select(listOf(n,age.copy(focused=true))).isEmpty())}
}
