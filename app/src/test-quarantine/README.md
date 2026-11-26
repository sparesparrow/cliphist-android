# Quarantined unit tests

These test files were written against APIs that the app does not have, such as:
- `AdvancedBubbleType.TOOLBELT` and `shouldBeVisible()`;
- `TextSelectionManager.pasteText`;
- `VoiceBubble.addTranscription` called on a collaboration bubble;
- `MainViewModel` constructor arguments in a different order;
- `SmartAction` fields that were renamed.

They never compiled: `main` itself did not compile until #47, so no unit test ran before then. They were moved here, outside the `test` source set, so the rest of the suite can compile and run.

To bring one back:
1. Update it to the current API.
2. `git mv` it back under `app/src/test/java/`.
3. Run `./gradlew testDebugUnitTest`.

| File | Why it does not compile |
|---|---|
| `presentation/viewmodels/MainViewModelTest.kt` | Use cases passed in the wrong order and to parameters that don't exist |
| `domain/model/SmartActionTest.kt` | `action`/`ActionType` API changed; `BubbleState` passed where an `ActionType` is expected |
| `data/database/ClipboardItemEntityTest.kt` | `contentType` is now an enum, not a `String`; other fields renamed |
| `presentation/ui/components/HighlightedAreaViewTest.kt` | Same `SmartAction` changes |
| `TestUtils.kt`, `TestSuite.kt` | Builders for the old `ClipboardItem`/`SmartAction` shapes |
| `VoiceRecognitionManagerTest.kt` | `getMaxResults` and `getEnablePartialResults` don't exist |
| `KeyboardVisibilityDetectorTest.kt` | `keyboardHeight` doesn't exist |
| `domain/model/ContentAnalyzerTest.kt` | `bubbleState` doesn't exist on the analysis result |
| `AdvancedBubbleTypesTest.kt` | Tests entries and methods that `AdvancedBubbleType` doesn't have |
| `CollaborationBubbleTest.kt` | Voice-bubble API on a collaboration bubble; `Collaborator(position)` |
| `TextSelectionManagerTest.kt` | `pasteText` doesn't exist |
