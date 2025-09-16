# Acceptance Criteria - Voice Command Engine

## VoiceCommand contract
- Commands defined for clipboard domain: CopyLatest, Search(query), PinItem(id), ClearHistory, ToggleEncryption(bool), OpenBubble, StartServices, StopServices.
- Result types: Success, Rejected(reason), Error(message, cause?).

## Propose API
- Given transcript and context, returns 0..N commands ordered by confidence.
- Must not execute side effects.

## Execute API
- Enforces context policies (e.g., block destructive actions while driving).
- Returns Rejected with reason if policy denies.
- Returns Error on unexpected exceptions; never throws.

## Test cases (initial)
- Instantiate each command; verify type safety and equality semantics.
- Policy scaffold: destructive commands rejected when isDriving=true.

