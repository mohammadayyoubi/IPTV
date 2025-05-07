cat <<EOF
# 📺 IPTV Android Player (Java)

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)
[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![SQLite](https://img.shields.io/badge/SQLite-07405E?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/index.html)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white)](https://firebase.google.com/docs/auth)
[![ExoPlayer](https://img.shields.io/badge/ExoPlayer-4DB6AC?style=for-the-badge&logoColor=black)](https://exoplayer.dev/)

This project is an Android IPTV streaming application crafted in Java. It leverages the power of SQLite for efficient local data storage and Firebase for secure administrative authentication. Enjoy seamless streaming of your favorite \`.m3u8\` channels, effortlessly browse content by country and category, switch between multiple servers for a single channel, and curate your personal list of favorite channels – all conveniently stored on your device.

## ✨ Key Features

* **📡 IPTV Streaming:** Plays \`.m3u8\` format streams smoothly.
* **🌍 Content Filtering:** Browse channels by country and category.
* **🔁 Multi-Server Support:** Each channel can have multiple server options for redundancy.
* **⭐ Local Favorites:** Save and access your preferred channels locally.
* **🔐 Secure Admin:** Firebase Authentication protects administrative functionalities.
* **🧱 Local Database:** Utilizes SQLite for persistent user data storage.

## 🏗️ Architecture

The application follows an Object-Oriented Programming (OOP) approach with a clear separation of concerns:

* **Data Models:** Well-defined Java classes representing core entities:
    * \`Channel.java\`
    * \`Country.java\`
    * \`Category.java\`
    * \`ChannelServer.java\`
    * \`Favorite.java\`
* **Data Access Object (DAO) Pattern:** Dedicated DAO classes handle all interactions with the SQLite database, promoting modularity and maintainability.
* **Smooth Playback:** Integrated ExoPlayer ensures a reliable and high-quality video streaming experience.
* **Admin Security:** Firebase Authentication secures administrative actions, ensuring data integrity.

## 🗄️ Database Structure (SQLite)

The local SQLite database is organized into the following tables:

| Table Name      | Description                                   | Columns                                                                 |
| --------------- | --------------------------------------------- | ----------------------------------------------------------------------- |
| \`Country\`       | Stores country names and their flag URLs.     | \`country_name\`, \`flag_url\`                                              |
| \`Category\`      | Stores channel categories.                    | \`category_name\`                                                         |
| \`Channel\`       | Stores basic information about IPTV channels. | \`channel_id\`, \`channel_name\`, \`category_id\`, \`country_id\`, \`logo_url\` |
| \`ChannelServer\` | Stores multiple streaming links for each channel. | \`server_id\`, \`channel_id\`, \`stream_url\`                                 |
| \`Favorite\`      | Stores user's favorited channels.             | \`channel_id\`                                                            |

## ⚙️ Classes Overview

Here's a quick look at the main Java classes:

* \`Country.java\`: Represents country information.
* \`Category.java\`: Represents channel categories.
* \`Channel.java\`: Represents individual IPTV channels.
* \`ChannelServer.java\`: Represents different streaming servers for a channel.
* \`Favorite.java\`: Represents a user's favorited channel.
* \`DatabaseHelper.java\`: Manages the SQLite database creation and upgrades.
* \`[Model]DAO.java\`: (e.g., \`ChannelDAO.java\`, \`CountryDAO.java\`) Classes responsible for database operations for each data model.

## 🛠️ Technologies Used

This project leverages the following technologies:

* **Java (Android Studio):** The primary programming language and IDE for Android development.
* **SQLite (with \`SQLiteOpenHelper\`):** A lightweight, disk-based database for local data storage.
* **Firebase Auth (email/password login):** Google's platform for secure user authentication (used for admin access).
* **ExoPlayer:** Google's extensible media player library for Android, supporting HLS streaming.
* **M3U8:** The supported playlist format for IPTV streams (with potential for future expansion).

## ▶️ Example Usage

\`\`\`java
// Assuming 'channel' is a Channel object and 'player' is an ExoPlayer instance

// Get the first available server for the channel
if (!channel.getServers().isEmpty()) {
    ChannelServer mainServer = channel.getServers().get(0);
    MediaItem mediaItem = MediaItem.fromUri(mainServer.getStreamUrl());

    // Set the media item to the player
    player.setMediaItem(mediaItem);

    // Prepare and play the stream
    player.prepare();
    player.play();
} else {
    Log.w("IPTVPlayer", "No servers available for this channel.");
}
\`\`\`

## 🚀 Getting Started

Ready to dive in? Follow these simple steps:

1.  **Clone the repository:**
    \`\`\`bash
    git clone <repository_url>
    \`\`\`
2.  **Open in Android Studio:** Launch Android Studio and open the cloned project.
3.  **Set up Firebase:**
    * Create a Firebase project on the [Firebase Console](https://console.firebase.google.com/).
    * Enable the **Authentication** service (Email/Password).
    * Download the \`google-services.json\` file and add it to your app's module (\`app/\`) directory.
4.  **Build and Run:** Build and run the project on an Android emulator or a physical device.

## 🔒 Firebase Admin Use

It's important to note that **Firebase Authentication is solely used to secure administrative functionalities**, such as updating the channel database. Regular users of the IPTV player do not need to register or log in with Firebase.

## 🌐 Future Improvements

Here are some exciting features planned for future development:

* Expand stream support to include **YouTube**, **MP4**, and **DASH** formats.
* Implement robust **channel search and filtering** capabilities.
* Introduce a sleek and user-friendly **dark mode UI**.
* Explore **remote configuration** options using Firebase Remote Config or a dedicated API backend for dynamic updates.

## 🤝 Contributing
This project will be done dy:
- Mohammad Al Ayoubi
- Mahmoud Al Natour
