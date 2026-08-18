# Voice pipeline rebuild — what changed and why

This documents the changes made to `amora-android`'s voice assistant pipeline
per request: remove the AI chat/conversation layer, keep the "Hey Google"
style continuous wake-word workflow, add a spoken end to the session on
"bye"/"exit", fix a mic reliability bug, and give the app's mic session
priority over other apps' audio.

## Removed

- `core/assistant/actions/AiConversationHandler.kt` — routed unmatched speech
  and weather queries to a backend chat/LLM endpoint. Deleted entirely; this
  assistant now only executes recognized device commands, no conversation.
- `core/assistant/intent/AssistantIntent.AiQuery` — the intent type that fed
  the chat handler. Removed from the sealed class.
- Dead code, confirmed unreferenced anywhere before deleting:
  `core/system/wakeword/{VoiceActivityGate,WakeWordDetectors,WakeWordEngine}.kt`,
  `core/assistant/wakeword/{DefaultWakeWordEngine,IWakeWordEngine}.kt`.

## Added

- `AssistantIntent.EndSession` — matched from phrases like "bye", "goodbye",
  "exit", "quit", "stop assistant", "go to sleep", checked before the
  generic "stop" (which only means "stop talking").
- `core/assistant/actions/WeatherActionHandler.kt` — "what's the weather"
  now calls the app's own `/v1/weather` endpoint directly (same one the
  Dashboard card uses) instead of going through chat. Named-location
  queries ("weather in Paris") aren't geocoded yet, so those get an honest
  "can't do that yet" response instead of silently answering for the wrong
  city.
- `core/assistant/actions/UnknownCommandHandler.kt` — replaces the AI
  fallback with a short spoken nudge toward a real supported command.
- `AssistantController.endSession()` — on `EndSession`, speaks a farewell,
  then turns the assistant off via the existing `MasterSwitchManager`
  (the same mechanism the Settings toggle uses) rather than a bespoke
  shutdown path. This stops `FloatingOrbService` cleanly, persists the off
  state, and keeps the Settings toggle in sync with what "bye" just did.

## Fixed

- **Microphone priority**: `AndroidSpeechRecognitionManager` now requests
  `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` for the duration of every listening
  session (wake-word and command), asking other apps to duck/pause while
  Amora's mic is open — giving this app's mic session priority the way
  "Hey Google" takes over audio the instant it starts listening.
- **"Communication breaks" / silent recognizer failures**: the recognizer
  was being destroyed and recreated in the same call stack on every
  restart. On several OEM builds the old `SpeechRecognizer` isn't fully
  released synchronously, which intermittently produces
  `ERROR_RECOGNIZER_BUSY` or a session that silently never calls back. Fixed
  with a short delay before recreation (`recognizerRecreateDelayMs`) plus
  exponential backoff on repeated consecutive failures
  (`consecutiveRestartFailures`, capped at 4s), so the always-on loop
  self-heals instead of getting stuck.

## Verified, and what wasn't

- Confirmed via `grep -r` across the whole Android source tree: nothing
  still references any removed class/package.
- All edited/created Kotlin files pass a basic brace/paren balance check.
- **Not verified**: this sandbox has no Android SDK or Gradle toolchain, so
  `./gradlew assembleDebug` could not actually be run here. Please build it
  locally — if anything doesn't compile, send me the error and I'll fix it.
