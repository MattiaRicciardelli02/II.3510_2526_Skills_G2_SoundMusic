# MyBeat
by Mattia Raffaele Ricciardelli e Federico Santavicca

⸻

 
MyBeat is an Android application for creating, managing, and sharing music beats.
The app allows users to record audio locally, organize their projects, and publish completed beats to a shared community where other users can browse and download them.

The project was developed as part of an academic assignment, with a focus on modern Android development practices, clean architecture, and testing.


**Project Overview**

MyBeat combines local audio production features with cloud-based sharing. Users can work offline by recording and editing beats on their device, then authenticate to publish their projects to the community. Each published beat can include a description and a musical reference, providing additional context and inspiration for other users.

The application is designed to remain functional without authentication, while transparently integrating user data once a login is performed.


**Architecture and Technologies**

The application follows the MVVM (Model–View–ViewModel) architectural pattern to ensure a clear separation of concerns.
The user interface is implemented entirely with Jetpack Compose and is driven by reactive state management using StateFlow. This approach simplifies UI updates and improves testability.

Local data persistence is handled with Room, which stores beats, recordings, and pad configurations. Cloud functionality is provided through Firebase: Authentication manages user accounts, Firestore stores community metadata, and Cloud Storage is used for audio files and cover images.

Asynchronous operations and background tasks are implemented using Kotlin Coroutines to ensure a responsive user experience.


**Community Features and Music References**

Published beats are shared through a community section backed by Firebase. Users can browse projects created by others, view detailed information, and download selected beats to their local library.

To associate musical inspiration with published content, MyBeat integrates the iTunes Search API. This API is used to search for reference tracks and to provide short preview samples. It was chosen as an alternative to Spotify, which was not available for integration at the time of development.


**Testing**

The project includes both unit tests and UI tests to ensure reliability and correctness.

Unit tests focus on core business logic and data access layers, such as Room DAOs, recording logic, and data migration between guest and authenticated users. External dependencies, including Firebase components, are mocked where appropriate to keep tests fast and deterministic.

UI tests are implemented using the Jetpack Compose testing framework and simulate complete user interaction flows. These tests validate authentication input handling, pad and sequencer interactions, and dialog-based workflows such as exporting and publishing beats. Semantic tags are used to make the tests robust against layout and visual changes.
