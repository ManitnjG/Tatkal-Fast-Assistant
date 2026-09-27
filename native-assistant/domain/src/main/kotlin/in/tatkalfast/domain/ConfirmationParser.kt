package `in`.tatkalfast.domain

/** Explicitly pasted text only. Never read SMS, clipboard, OTPs or browser contents. */
object ConfirmationParser {
 fun pnr(text:String):String? {
  if(text.length>12000) return null
  val matches=Regex("(?i)\\bPNR(?:\\s*(?:NO\\.?|NUMBER))?\\s*[:#=\\-]?\\s*([0-9]{10})(?![0-9])").findAll(text).map {it.groupValues[1]}.distinct().toList()
  return matches.singleOrNull()
 }
}
