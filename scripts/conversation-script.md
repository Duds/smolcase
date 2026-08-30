# Conversation Test Script — SMOLCASE

Read these prompts aloud to the robot in order. Each tests a specific feature.
Run `bash scripts/test_conversation.sh` first, then open a second terminal for
`adb logcat SmolcaseMain:V SmolcaseEars:V SmolcaseVoice:V SmolcaseLLM:V *:S`.

Check the JSONL log for latency (`latency_ms`) and session tracking.

---

## 1. Basic Greeting & Response

> "Hello."
> "What's your name?"
> "How are you?"

**Check:** The creature responds in TARS register (deadpan, dry). No emoji, no lists.

---

## 2. Echo Loop (Cooldown)

Say three short things in rapid succession (one right after the other):

> "Hi."
> "What time is it?"
> "Tell me a joke."

Then wait 3+ seconds and say:

> "Hello again."

**Check:** In logcat (`SmolcaseEars`), look for:
```
📝 heard (cooldown): ...    ← ignored during 2.5s window
📝 heard: ...               ← only first utterance processed normally
```
Only **one** LLM reply should fire for the burst, or the last one post-cooldown.
Verify the JSONL log only has one `heard` → `replied` pair (or the cooldown-gated one).

---

## 3. Prompt Repetition Guard

> "What directives do you have?"

Wait for reply, then immediately:

> "What are your rules?"

Wait for reply, then:

> "List your directives."

**Check:** The creature never says "directives", "rules", "acknowledged", or
repeats background context back at you. Replies should be natural in-character
TARS responses, not parroting the system prompt.

---

## 4. IntentRouter — Canned Commands

> "Remind me to feed the cat."
> "What are my reminders?"
> "Clear my reminders."

**Check:** These are handled by `IntentRouter` (instant, no LLM). Logcat shows
no LLM call. JSONL entries may not appear (they bypass the LLM). Reply should
be immediate.

---

## 5. Dial Commands

> "Set humor to 90."
> "Tell me a joke."

Then:

> "Set honesty to 30."
> "Be honest with me."

Then:

> "Set humor to 10."
> "Tell me a joke."

**Check:** The tone changes noticeably between high and low humor settings.
High humor = drier deadpan sarcasm. Low humor = literal, mission-focused.
The `latency_ms` field in the JSONL log shows LLM response time.

---

## 6. Memory — Session Persistence

Say something memorable:

> "My favourite colour is blue."

Then stop the app (`adb shell am force-stop com.smolcase.companion`),
restart via the script, and ask:

> "Do you remember my favourite colour?"

**Check:** The `soulSummary()` includes reminders and total session count.
If memory persistence works, the LLM context carries the reminder. The JSONL
log should show `session` incrementing by 1 after the restart.

---

## 7. Long Reply Latency Test

> "Tell me about yourself."
> "What do you think about desk robots?"
> "Explain the meaning of life, briefly."

**Check:** For each, check the `latency_ms` field in the JSONL log:
```
{"ts":"...","dir":"replied","text":"...","latency_ms":7234,...}
```
Record each value. If consistently >5000ms, Gemma is the bottleneck.

---

## 8. Edge Cases — Empty / Nonsense Input

Click the mic button (tap face slowly) so no audio is sent, then:

Stay silent for 5 seconds, then say something random:

> "Blorb flargle zorp."
> "What?"

**Check:** No crash. Unknown inputs either get a deadpan "You'll have to
rephrase that" or a confused but in-character reply. Logcat shows no errors.

---

## 9. Multi-turn Context

Have a short back-and-forth:

> "I'm working on a project."
> "What project?"
> "Building a robot."
> "What should I name it?"

**Check:** The creature maintains conversational thread across turns.
Shouldn't greet you again mid-conversation.

---

## 10. Mojave Test — Unusual

> "I'm going to be gone for a while."
> "Say goodbye."

**Check:** The creature acknowledges the departure in character. No cheerful
"Have a great day!" — stays in TARS register.

---

## Results Log

| # | Test | Pass/Fail | Notes | Latency (ms) |
|---|------|-----------|-------|-------------|
| 1 | Greeting | | | |
| 2 | Echo loop | | | |
| 3 | Prompt repetition | | | |
| 4 | IntentRouter | | | |
| 5 | Dial commands | | | |
| 6 | Memory | | | |
| 7 | Latency | | | |
| 8 | Edge cases | | | |
| 9 | Multi-turn | | | |
| 10 | Departure | | | |