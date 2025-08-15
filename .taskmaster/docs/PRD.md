# WhisperTop – PRD (Product Requirements Document)

---

## Overview

**WhisperTop** is an Android overlay application that enables quick and accurate speech-to-text transcription anywhere on the device, powered by OpenAI’s `/v1/audio/transcriptions` API using Whisper 3 Turbo or newer GPT‑4o transcribe models. It supports BYO (Bring Your Own) API keys for privacy and flexibility, allowing the user to leverage their own OpenAI account without relying on third-party intermediaries.

---

## Goals

1. **Seamless Overlay** – Always-available floating mic button to transcribe speech into the active text field.
2. **Fast Transcription** – Low-latency audio capture and upload, returning accurate text.
3. **BYO API Key Support** – Secure storage and usage of user’s own OpenAI key, and the base model can be selected from a list of available models or they can type in their own.
4. **OpenAI-Compatiable Endpoint** – Uses OpenAI’s official audio transcription endpoint for reliability and performance, but the user can also use their own endpoint if they prefer.
4. **Minimal UI Disruption** – Overlay works on top of any app without replacing Gboard or the system keyboard, allowing for easy text input.
5. **Privacy-First** – All processing via user’s own API key, no transcription data retained.

---

## Tech Stack

* **Language:** Kotlin 2 (K2 compiler)
* **UI:** Jetpack Compose, Material 3 design
* **Min SDK:** 26, **Target SDK:** 35 (Android 15)
* **Build Tools:** AGP 8.7

---

## Features

### P0 (Must-Have)

* Overlay mic button with draggable placement
* Foreground service with microphone capture and `.wav` file generation
* Audio file submission to OpenAI `/v1/audio/transcriptions` endpoint
* Paste transcription into focused field via Accessibility API
* API key entry & secure storage (EncryptedSharedPreferences)
* Multi-language auto-detection
* Proper runtime permissions handling for microphone, overlay, and accessibility

### P1 (Nice-to-Have)

* Quick Settings Tile trigger
* Push-to-talk hardware button mapping
* Theme customization (dark/light, accent colors)
* Hotword activation for hands-free mode
* Option to select transcription model and language manually

### P2 (Future Enhancements)

* Offline Whisper inference (faster-whisper)
* Translation mode (speech-to-text in a different language)
* Voice command macros to automate actions

---

## API Integration

* **Primary:** [OpenAI Audio Transcriptions API](https://platform.openai.com/docs/guides/speech-to-text)
* **Auth:** BYO API key stored locally
* **Request Format:** Multipart/form-data with `.wav` file (PCM16 mono @16kHz)
* **Response Handling:** Parse JSON response for `text` field
* **Error Handling:** Retry mechanism for network errors, clear error messages for invalid keys or quota issues

---

## Security

* Keys stored locally via EncryptedSharedPreferences
* All API requests sent directly to OpenAI’s servers using the user’s key
* No audio or transcription logs stored by WhisperTop
* Clear “Delete API Key” and “Clear History” options in settings

---

## UX/UI

* **Overlay:** Minimal mic icon, draggable, color-coded states (idle = gray, listening = red, processing = blue)
* **Recording Indicator:** Animated pulsing ring around mic icon during capture
* **Transcription Result:** Auto-insert into currently focused input field via Accessibility API
* **Settings Screen:**

  * API key entry with validation
  * OpenAI-Compatiable endpoint configuration with default endpoint
  * Model selection dropdown (Whisper 3 Turbo, GPT‑4o, etc., or manual entry)
  * Language preferences (auto or manual)
  * Theme customization
  * Privacy controls
* **Error Dialogs:** Clear, human-readable error messages with suggested fixes

---

## Delivery Plan

**Phase 1 (P0)**

1. Scaffold Compose app with overlay service
2. Implement mic capture service producing `.wav` files
3. Build API client for `/v1/audio/transcriptions`
4. Integrate Accessibility paste support
5. Add secure API key storage and retrieval

**Phase 2 (P1)**
6. Add Quick Settings Tile and hardware push-to-talk support
7. Implement theme customization
8. Add manual language/model selection

**Phase 3 (P2)**
9. Integrate offline Whisper model (faster-whisper)
10. Add translation mode
11. Implement voice macros

---

**Tagline:** *WhisperTop – Your voice, anywhere.*
