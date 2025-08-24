# WhisperTop User Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Initial Setup](#initial-setup)
4. [Basic Usage](#basic-usage)
5. [Advanced Features](#advanced-features)
6. [Settings and Customization](#settings-and-customization)
7. [Troubleshooting](#troubleshooting)
8. [Privacy and Security](#privacy-and-security)
9. [Tips and Best Practices](#tips-and-best-practices)
10. [FAQ](#faq)

## Introduction

WhisperTop is an Android overlay application that enables quick and accurate speech-to-text transcription anywhere on your device using OpenAI's Whisper API. It features a floating microphone button that works on top of any app, allowing you to transcribe speech directly into text fields.

### Key Features

- 🎤 **System-wide overlay**: Works on top of any Android app
- 🔊 **High-quality transcription**: Powered by OpenAI's Whisper API
- 🌐 **Multiple languages**: Supports 50+ languages with auto-detection
- 🎯 **Direct text insertion**: Automatically inserts transcribed text
- 🛡️ **Privacy-focused**: Your API key, your data, local processing
- 🎨 **Material You design**: Adaptive theming with dynamic colors
- ⚡ **Performance optimized**: Fast transcription with minimal battery usage

## Installation

### Requirements

- **Android 8.0 (API 26)** or higher
- **Internet connection** for API access
- **OpenAI API key** ([get one here](https://platform.openai.com/api-keys))
- **Microphone permission**
- **Overlay permission** (system alert window)
- **Accessibility permission** (for text insertion)

### Download and Install

1. **Download APK**: Get the latest release from [GitHub Releases](https://github.com/shekohex/WhisperTop/releases)
2. **Enable Unknown Sources**: Settings → Security → Install unknown apps → Enable for your browser
3. **Install**: Tap the downloaded APK file and follow prompts
4. **Launch**: Open WhisperTop from your app drawer

### Alternative Installation Methods

**Via ADB (for developers):**
```bash
# Debug build
./gradlew installDebug

# Release build  
./gradlew assembleRelease
adb install composeApp/build/outputs/apk/release/composeApp-release.apk
```

## Initial Setup

### Step 1: Welcome and Permissions

When you first open WhisperTop, you'll be guided through essential setup:

1. **Welcome Screen**: Introduction to WhisperTop's features
2. **Permission Requests**: Grant the following permissions:
   - 🎤 **Microphone**: Record audio for transcription
   - 📱 **Overlay Permission**: Display floating button system-wide
   - ♿ **Accessibility Service**: Insert transcribed text into apps

### Step 2: OpenAI API Configuration

1. **Get API Key**: 
   - Visit [OpenAI API Keys](https://platform.openai.com/api-keys)
   - Create account if needed
   - Generate a new secret key (starts with `sk-`)

2. **Enter API Key**:
   - Open WhisperTop Settings
   - Navigate to "API Configuration"
   - Paste your API key
   - Verify connection (green checkmark)

3. **Choose Model** (Optional):
   - **whisper-1**: Fast, good quality (default)
   - **whisper-3-turbo**: Fastest, lower cost
   - **gpt-4o-audio-preview**: Best accuracy, higher cost

### Step 3: Enable Services

**Enable Accessibility Service:**
1. Settings → Accessibility → WhisperTop
2. Toggle "On"
3. Confirm in dialog

**Grant Overlay Permission:**
1. Settings → Apps → Special app access → Display over other apps
2. Find WhisperTop
3. Toggle "Allow display over other apps"

### Step 4: Initial Testing

1. Tap "Start Service" in WhisperTop settings
2. Look for floating microphone button
3. Test recording in any text field
4. Adjust settings as needed

## Basic Usage

### Floating Microphone Button

The floating microphone button is your main interface:

**Button States:**
- 🔘 **Gray (Idle)**: Ready to record
- 🔴 **Red (Recording)**: Currently recording audio
- 🔵 **Blue (Processing)**: Transcribing audio via API
- 🟢 **Green (Success)**: Transcription complete
- 🟠 **Orange (Error)**: Error occurred

### Recording Audio

**Method 1: Long Press (Recommended)**
1. **Long press** floating button to start recording
2. Speak clearly into microphone
3. **Release** to stop and transcribe
4. Text appears in focused text field automatically

**Method 2: Tap to Toggle**
1. **Tap** floating button to start recording
2. **Tap again** to stop and transcribe
3. Text inserted automatically

### Moving the Button

- **Drag** the floating button anywhere on screen
- Button **snaps to edges** for convenient access
- Position is **remembered** between sessions

### Text Insertion

WhisperTop automatically inserts transcribed text into:
- ✅ **Text fields** in any app
- ✅ **Search boxes** in browsers and apps  
- ✅ **Message input** in messaging apps
- ✅ **Note-taking** apps
- ✅ **Social media** post composers
- ✅ **Email** composition fields

**Supported Apps**: Works with virtually all Android apps that use standard text input fields.

## Advanced Features

### Language Detection and Selection

**Automatic Detection** (Default):
- WhisperTop detects spoken language automatically
- Supports 50+ languages including English, Spanish, French, German, Chinese, Japanese, etc.

**Manual Language Selection**:
1. Settings → Language & Region
2. Toggle off "Auto-detect language"
3. Select preferred language from list
4. All recordings will use this language

**Supported Languages**: Arabic, Chinese, Dutch, English, French, German, Italian, Japanese, Korean, Polish, Portuguese, Russian, Spanish, Turkish, and many more.

### Custom Prompts and Context

Improve transcription accuracy with context:

1. **Settings → Transcription**
2. **Custom Prompt**: Add context about expected content
   - Example: "Medical terminology and patient notes"
   - Example: "Technical meeting with software terms"
   - Example: "Customer support conversation"

3. **Temperature Setting**: Control creativity vs accuracy
   - **0.0**: Most conservative, accurate
   - **0.5**: Balanced
   - **1.0**: More creative, less predictable

### Model Comparison

| Model | Speed | Quality | Cost | Best For |
|-------|--------|---------|------|----------|
| **whisper-1** | Fast | Good | Standard | General use |
| **whisper-3-turbo** | Fastest | Good | Lower | Quick notes |
| **gpt-4o-audio-preview** | Slower | Best | Higher | Professional use |

### Batch Processing and History

**Transcription History**:
- All transcriptions automatically saved
- Access via Settings → History
- Search transcriptions by text or date
- Export to text or JSON format

**Quick Actions**:
- **Copy**: Copy transcription to clipboard
- **Share**: Share via any app
- **Edit**: Modify transcription before use
- **Delete**: Remove from history

### Performance Optimization

**Audio Quality Settings**:
- **Sample Rate**: 16kHz (optimal for Whisper)
- **Bit Depth**: 16-bit PCM
- **Channels**: Mono (reduces file size)
- **Format**: WAV (best compatibility)

**Network Optimization**:
- **Compression**: Audio compressed before upload
- **Retry Logic**: Auto-retry failed requests
- **Timeout**: 30-second request timeout
- **Offline Handling**: Graceful degradation when offline

## Settings and Customization

### API Configuration

**OpenAI Settings**:
- **API Key**: Your OpenAI secret key
- **Model Selection**: Choose Whisper model
- **Base URL**: Custom endpoints supported
- **Timeout**: Request timeout (15-60 seconds)

**Custom Endpoints**:
- Support for Azure OpenAI
- Compatible API providers
- Self-hosted Whisper deployments

### Theme and Appearance

**Theme Options**:
- 🌞 **Light Mode**: Classic light theme
- 🌙 **Dark Mode**: OLED-friendly dark theme  
- 🎨 **System**: Follow system theme
- 🌈 **Material You**: Dynamic colors from wallpaper (Android 12+)

**Button Customization**:
- **Size**: Small, Medium, Large
- **Opacity**: 50-100% transparency
- **Colors**: Custom accent colors
- **Position**: Remember last position

### Audio Settings

**Recording Quality**:
- **Quality**: High (recommended) or Standard
- **Noise Reduction**: Built-in noise filtering
- **Auto Gain**: Automatic level adjustment
- **Wind Filtering**: Reduce wind noise

**Microphone**:
- **Source**: Voice Recognition (optimized)
- **Sensitivity**: Auto or manual adjustment
- **Echo Cancellation**: For speakerphone use

### Privacy and Data

**Privacy Controls**:
- **Local Processing**: Audio processed locally when possible
- **Data Retention**: Auto-delete old transcriptions
- **Analytics**: Opt-out of usage analytics
- **Crash Reports**: Help improve app quality

**Data Management**:
- **Storage Location**: Internal app storage
- **Encryption**: Database encrypted with SQLCipher
- **Export**: JSON, CSV, or plain text formats
- **Clear Data**: Reset app data completely

### Accessibility

**Accessibility Features**:
- **Screen Reader**: Full TalkBack support
- **High Contrast**: Enhanced contrast modes
- **Large Text**: Supports system text scaling
- **Voice Feedback**: Audio confirmation of actions

**Text Insertion**:
- **Insert Method**: Accessibility API or clipboard
- **Cursor Position**: Smart cursor placement
- **Text Replacement**: Replace selected text
- **Undo Support**: Undo transcription insertion

## Troubleshooting

### Common Issues and Solutions

#### 🚫 Floating Button Not Appearing

**Cause**: Overlay permission not granted
**Solution**:
1. Settings → Apps → WhisperTop → Advanced → Display over other apps
2. Enable "Allow display over other apps"
3. Restart WhisperTop service

**Alternative Solution**:
1. Android Settings → Special app access → Display over other apps
2. Find WhisperTop → Enable

#### 🎤 Recording Not Working  

**Cause**: Microphone permission or hardware issue
**Solution**:
1. Check microphone permission: Settings → Apps → WhisperTop → Permissions
2. Test with another recording app
3. Restart device if hardware issue suspected
4. Check if another app is using microphone

#### ❌ API Errors

**"Invalid API Key"**:
- Verify API key starts with `sk-`
- Check for extra spaces or characters
- Ensure API key is active on OpenAI dashboard
- Try regenerating API key

**"Rate Limit Exceeded"**:
- You've exceeded OpenAI's rate limits
- Wait a few minutes before trying again
- Consider upgrading OpenAI plan
- Check API usage in OpenAI dashboard

**"Model Not Available"**:
- Selected model not available in your region
- Try switching to `whisper-1` model
- Check OpenAI service status

#### 📝 Text Not Inserting

**Cause**: Accessibility service not enabled
**Solution**:
1. Settings → Accessibility → WhisperTop → Enable
2. Grant accessibility permission when prompted
3. Restart target app
4. Try different text field

**Alternative Methods**:
- Use clipboard insertion mode
- Manual copy-paste from notification
- Voice typing in target app

#### 🔊 Poor Audio Quality

**Environmental Factors**:
- Record in quiet environment
- Hold device 6-8 inches from mouth
- Avoid background noise and wind
- Use external microphone if available

**Settings Adjustments**:
- Enable noise reduction
- Increase microphone sensitivity
- Use high quality recording mode
- Adjust audio gain settings

#### 🐌 Slow Transcription

**Network Issues**:
- Check internet connection speed
- Use WiFi instead of mobile data
- Try different network connection
- Check OpenAI service status

**Audio Optimization**:
- Keep recordings under 30 seconds
- Use compressed audio format
- Reduce background noise
- Speak clearly and at normal pace

#### 🔋 Battery Drain

**Power Optimization**:
- Disable battery optimization for WhisperTop
- Use efficient recording mode
- Reduce overlay transparency
- Close unused background apps

**Settings Optimization**:
1. Settings → Battery → App optimization
2. Find WhisperTop → Don't optimize
3. Enable power-saving mode in app settings

### Error Messages and Solutions

| Error Message | Cause | Solution |
|---------------|--------|----------|
| "Network timeout" | Poor internet connection | Check connection, try again |
| "Audio file too large" | Recording too long | Keep under 25MB/30 seconds |
| "Permission denied" | Missing permissions | Grant required permissions |
| "Service unavailable" | OpenAI API down | Check status.openai.com |
| "Invalid audio format" | Audio processing error | Restart app, try again |
| "Accessibility blocked" | App blocking accessibility | Try different app or field |

### Advanced Troubleshooting

#### Logs and Diagnostics

**Enable Debug Logging**:
1. Settings → Advanced → Enable debug logging
2. Reproduce issue
3. Settings → Advanced → Export logs
4. Share logs with support

**Clear App Cache**:
1. Android Settings → Apps → WhisperTop → Storage
2. Tap "Clear Cache"
3. Restart app

**Reset App Data** (Last Resort):
1. Android Settings → Apps → WhisperTop → Storage
2. Tap "Clear Storage"
3. Reconfigure app from scratch

#### Performance Analysis

**Check System Resources**:
- Available RAM (need 100MB+ free)
- Storage space (need 50MB+ free)  
- CPU usage of other apps
- Network bandwidth

**Optimize Performance**:
- Close background apps
- Restart device weekly
- Update to latest Android version
- Free up storage space

## Privacy and Security

### Data Protection

**Local Data Security**:
- ✅ Database encrypted with SQLCipher
- ✅ API keys stored in Android Keystore
- ✅ Temporary files securely deleted
- ✅ No data shared with third parties

**Network Security**:
- ✅ TLS 1.2+ encryption for all API calls
- ✅ Certificate pinning for OpenAI API
- ✅ Request validation and sanitization
- ✅ No logging of audio content

### Privacy Controls

**Data Minimization**:
- Audio files automatically deleted after transcription
- Transcription history configurable retention
- Optional analytics with opt-out
- No personal data collected without consent

**User Control**:
- **Delete Anytime**: Remove transcriptions individually or in bulk
- **Export Data**: Full data export in standard formats
- **Clear History**: One-tap history clearing
- **Disable Analytics**: Complete opt-out option

### Compliance

**GDPR Compliance**:
- Right to access data
- Right to rectify data  
- Right to erase data
- Right to data portability
- Privacy by design

**Security Best Practices**:
- Regular security updates
- Minimal permission requests
- Secure key storage
- No unnecessary data retention

## Tips and Best Practices

### Getting Better Transcriptions

**Speaking Technique**:
- 🎯 Speak clearly and at normal pace
- 📏 Maintain 6-8 inch distance from microphone
- 🔇 Minimize background noise
- ⏸️ Pause between sentences for better punctuation

**Recording Environment**:
- 🏠 Record in quiet indoor spaces
- 🚗 Avoid cars, traffic, or machinery noise
- 🌬️ Minimize wind and air conditioning
- 📱 Hold device steady during recording

**Content Tips**:
- 📝 Use custom prompts for technical content
- 🗣️ Speak acronyms letter-by-letter
- 🔢 Say numbers clearly (e.g., "twenty-five" not "25")
- ⏸️ Pause after complex terms

### Productivity Workflows

**Meeting Notes**:
1. Set custom prompt: "Business meeting with action items"
2. Record key discussion points
3. Use automatic transcription for minutes
4. Export to document format

**Message Composition**:
1. Long-press in messaging app
2. Speak message content
3. Review and edit if needed
4. Send or continue typing

**Email Productivity**:
1. Use for email subject lines
2. Dictate email body content  
3. Record voice memos for later
4. Transcribe voice messages received

**Note-Taking**:
1. Quick capture of ideas on-the-go
2. Transcribe handwritten notes to digital
3. Voice journaling with automatic text
4. Meeting minutes and action items

### Battery and Performance

**Optimal Settings for Battery Life**:
- Use "Standard" recording quality
- Enable power-saving mode
- Set shorter retention periods
- Disable unnecessary features

**Performance Optimization**:
- Keep app updated to latest version
- Restart device weekly
- Clear cache monthly
- Monitor storage space

**Data Usage Management**:
- Use WiFi when possible for API calls
- Monitor data usage in settings
- Consider unlimited data plan for heavy use
- Enable compression for slower connections

## FAQ

### General Questions

**Q: Is WhisperTop free to use?**
A: The app is free, but you need your own OpenAI API key. OpenAI charges for API usage (typically $0.006 per minute of audio).

**Q: Does WhisperTop work offline?**
A: No, WhisperTop requires internet connection to access OpenAI's API for transcription. Offline support is planned for future versions.

**Q: What languages are supported?**
A: 50+ languages including English, Spanish, French, German, Italian, Portuguese, Dutch, Russian, Arabic, Chinese, Japanese, Korean, and many more.

**Q: Can I use my own Whisper model?**
A: Yes, WhisperTop supports custom endpoints. You can use Azure OpenAI or self-hosted Whisper deployments.

**Q: Does WhisperTop store my recordings?**
A: No, audio files are deleted immediately after transcription. Only the transcribed text is optionally saved locally.

### Technical Questions

**Q: Why do I need accessibility permission?**
A: Accessibility service allows WhisperTop to insert transcribed text directly into text fields across all apps, making the experience seamless.

**Q: Can other apps access WhisperTop's data?**
A: No, all data is stored in WhisperTop's private, encrypted database. Other apps cannot access this data.

**Q: What Android versions are supported?**
A: Android 8.0 (API 26) and higher. Some features require newer versions.

**Q: Does WhisperTop work with Samsung/Xiaomi/etc.?**
A: Yes, WhisperTop is designed to work with all Android manufacturers. Some may require additional permission setup.

**Q: Can I export my transcription history?**
A: Yes, you can export in JSON, CSV, or plain text formats via Settings → Data Management → Export.

### Troubleshooting Questions

**Q: The floating button disappeared, how do I get it back?**
A: Go to WhisperTop settings and tap "Start Service" or restart the app. Ensure overlay permission is granted.

**Q: Transcription is inaccurate, how can I improve it?**
A: Use custom prompts for context, speak clearly, record in quiet environments, and consider using the higher-quality GPT-4o model.

**Q: The app crashes frequently, what should I do?**
A: Update to the latest version, clear app cache, ensure sufficient free storage, and check for Android system updates.

**Q: API calls are expensive, how can I reduce costs?**
A: Use whisper-3-turbo model, keep recordings short, record in quiet environments for first-try accuracy, and monitor usage in OpenAI dashboard.

**Q: Text insertion doesn't work in certain apps, why?**
A: Some apps block accessibility services for security. Try using clipboard mode or manual copy-paste for these apps.

### Privacy Questions

**Q: Is my audio data sent to OpenAI?**
A: Yes, audio is sent to OpenAI's API for transcription, then immediately deleted. OpenAI's privacy policy applies to this data.

**Q: Can I prevent WhisperTop from saving transcriptions?**
A: Yes, disable transcription history in Settings → Privacy → Disable history saving.

**Q: How do I delete all my data from WhisperTop?**
A: Settings → Data Management → Clear All Data, or uninstall the app to remove all local data.

**Q: Does WhisperTop have access to other apps' data?**
A: No, WhisperTop only accesses text fields you actively transcribe into. It cannot read other app data or content.

---

## Getting Help

**Need more help?**
- 📧 **Email Support**: [Create an issue](https://github.com/shekohex/WhisperTop/issues)
- 📚 **Documentation**: Check our [GitHub repository](https://github.com/shekohex/WhisperTop)
- 💬 **Community**: Join discussions in GitHub issues
- 🐛 **Bug Reports**: Use GitHub issues with debug logs

**Before contacting support:**
1. Check this user guide thoroughly
2. Update to the latest app version
3. Try basic troubleshooting steps
4. Collect debug logs if possible

**Provide the following information:**
- Android version and device model
- WhisperTop app version
- Detailed description of issue
- Steps to reproduce the problem
- Debug logs (if available)

---

*This guide is regularly updated. Check GitHub for the latest version.*