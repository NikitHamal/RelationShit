# RelationShit - AI-Powered Relationship Conflict Resolver

An Android application that provides AI-powered relationship counseling, conflict resolution, and psychological support through intelligent conversation agents.

## Features

- **AI-Powered Chat**: Multiple AI agents (Deepseek, Gemini) for relationship counseling
- **Conversation Management**: Track and manage relationship conversations
- **Insights & Analytics**: Generate relationship insights and psychological surveys
- **Knowledge Base**: Store and retrieve relationship knowledge and insights
- **Customizable Agents**: Create and configure AI agents with specific prompts and models
- **Journaling**: Personal journal entries for relationship reflection
- **Settings Management**: Configure API keys and app preferences

## Architecture

### Core Components

- **MainActivity**: Main navigation with bottom navigation
- **ChatActivity**: AI conversation interface with context management
- **InsightsActivity**: Relationship analytics and survey generation
- **KnowledgeActivity**: Knowledge base management
- **SurveyActivity**: Psychological survey creation and analysis
- **SettingsFragment**: App configuration and agent management

### API Integration

- **Deepseek API**: Integration with Deepseek AI models
- **Gemini API**: Integration with Google's Gemini models
- **BaseApiService**: Abstract service for API communication

### Data Models

- **Agent**: AI agent configuration and settings
- **Conversation**: Chat conversation management
- **Message**: Individual message handling
- **Knowledge**: Relationship insights storage
- **Insight**: Analytics and relationship insights
- **Survey**: Psychological assessment tools

## Setup

### Prerequisites

- Android Studio
- JDK 17
- Android SDK (API 21+)

### Installation

1. Clone the repository
2. Open in Android Studio
3. Configure API keys in Settings:
   - Deepseek API Key
   - Gemini API Key
4. Build and run the application

### API Configuration

The app requires API keys for AI services:

1. **Deepseek API**: Get your API key from [Deepseek](https://platform.deepseek.com/)
2. **Gemini API**: Get your API key from [Google AI Studio](https://aistudio.google.com/)

Add your API keys in the Settings section of the app.

## Build Configuration

### Gradle Setup

The project uses Gradle 8.7 with the following configuration:

- **compileSdk**: 34
- **minSdkVersion**: 21
- **targetSdkVersion**: 34
- **versionCode**: 1
- **versionName**: "1.0"

### Dependencies

- AndroidX AppCompat
- Material Design Components
- ViewBinding enabled
- Apache HTTP Legacy support

### Signing

The app is configured with a release signing configuration using the included keystore.

## CI/CD

### GitHub Actions

The repository includes a GitHub Actions workflow that:

1. Triggers on any push to any branch
2. Sets up JDK 17 and Gradle
3. Builds a signed release APK
4. Renames the APK with commit hash
5. Uploads the artifact for download

### Workflow Features

- **Automatic Builds**: Builds on every push
- **Signed APKs**: Creates properly signed release builds
- **Commit Hash Naming**: APKs are named with commit hash for tracking
- **Artifact Upload**: Builds are available as downloadable artifacts

## Development

### Project Structure

```
app/
├── src/main/
│   ├── java/com/relation/shit/
│   │   ├── adapter/          # RecyclerView adapters
│   │   ├── api/             # API service classes
│   │   ├── model/           # Data models
│   │   ├── utils/           # Utility classes
│   │   ├── view/            # Custom views
│   │   └── *.java           # Activity and Fragment classes
│   ├── res/                 # Resources (layouts, strings, etc.)
│   └── AndroidManifest.xml
├── build.gradle             # App-level build configuration
└── keystore.jks            # Signing keystore
```

### Key Files

- **MainActivity.java**: Main navigation and fragment management
- **ChatActivity.java**: AI conversation handling
- **SettingsFragment.java**: Configuration management
- **InsightsActivity.java**: Analytics and survey features
- **BaseApiService.java**: Abstract API service
- **SharedPrefManager.java**: Data persistence

## Features in Detail

### AI Chat System

- Supports multiple AI providers (Deepseek, Gemini)
- Context-aware conversations with memory management
- Automatic conversation summarization
- Knowledge generation from conversations

### Relationship Insights

- Psychological survey generation
- Relationship health analytics
- Progress tracking and visualization
- Customizable assessment tools

### Knowledge Management

- Store relationship insights
- Categorize and tag knowledge
- Search and retrieve information
- Export capabilities

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is open source. See the LICENSE file for details.

## Support

For issues and feature requests, please use the GitHub Issues section.

---

**Note**: This app is designed to provide relationship support and counseling through AI. It is not a substitute for professional therapy or medical advice. For serious relationship issues, please consult with qualified professionals.