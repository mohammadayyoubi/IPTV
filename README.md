📺 IPTV Android Player (Java)
This project is an Android IPTV streaming app developed in Java using SQLite for local storage and Firebase for admin authentication. It allows users to stream .m3u8 channels, browse by country and category, switch between multiple servers, and manage favorites — all stored locally.

🔧 Features
📡 Stream IPTV Channels (.m3u8 format only)

🌍 Country and Category Filtering

🔁 Multiple Server Support per Channel

⭐ Favorites Support (Local)

🔐 Firebase Auth (Admin Login Only)

🧱 SQLite Local Database for all user data

🏗️ Architecture
OOP-based data models (Channel, Country, Category, ChannelServer, Favorite)

DAO (Data Access Object) pattern for SQLite queries

ExoPlayer for smooth video playback

Firebase Authentication for admin actions only

📁 Database Structure (SQLite)
Table	Description
Country	Stores country name and flag URL
Category	Stores channel categories
Channel	Stores basic info about channels
ChannelServer	Stores multiple stream links per channel
Favorite	Stores user-favorited channels

🧩 Classes Overview
Country.java

Category.java

Channel.java

ChannelServer.java

Favorite.java

DatabaseHelper.java

DAO classes for all models

🔌 Technologies Used
Java (Android Studio)

SQLite (with SQLiteOpenHelper)

Firebase Auth (email/password login)

ExoPlayer (for HLS streaming)

M3U8 only (for now, extensible later)

▶️ Example Usage
java
Copy
Edit
ChannelServer mainServer = channel.getServers().get(0);
MediaItem mediaItem = MediaItem.fromUri(mainServer.getStreamUrl());
player.setMediaItem(mediaItem);
player.prepare();
player.play();
🚀 Getting Started
Clone the project

Open in Android Studio

Set up Firebase in your project (add google-services.json)

Build & Run the project

🔒 Firebase Admin Use
Firebase Auth is only used to secure admin-only actions like database updates.

Normal users do not need to register or log in.

🌐 Future Improvements
Add support for YouTube, MP4, and DASH streams

Add channel search/filter

Add dark mode UI

Remote config (Firebase or API backend)

📸 Screenshots (Optional)
Add screenshots of the app UI here (channel list, player, favorites).

🤝 Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
