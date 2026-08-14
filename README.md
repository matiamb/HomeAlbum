**Welcome to HomeAlbum!**

HomeAlbum is a self-hosted Android application for browsing and backing up photos and videos from your Android device to your own private server.

Beta: HomeAlbum is currently in early development and may contain bugs or incomplete functionality.

✨ Features
- Browse photos and videos captured with your device camera
- View photos with pinch-to-zoom and double-tap zoom
- Share and delete local photos and videos
- Configure your HomeAlbum Server connection
- Check whether a media file already exists on the server
- Upload photos and videos to your private server
- Background uploads using Android WorkManager
- Network and battery constraints for background uploads

<img width="250" height="505" alt="Screenshot_20260814_112758_HomeAlbum" src="https://github.com/user-attachments/assets/56c91478-c599-49f0-a341-c991a258ce90" />
<img width="250" height="505" alt="Screenshot_20260814_112827_HomeAlbum" src="https://github.com/user-attachments/assets/d79b6b87-902d-4e4b-ab0c-9178d0b89df2" />
<img width="250" height="505" alt="Screenshot_20260814_112853_HomeAlbum" src="https://github.com/user-attachments/assets/fa0d9b01-0c94-4af0-95fa-709f0cea1923" />

📱 Requirements
- Android device
- HomeAlbum Server
- A private LAN connection or Tailscale connection between the Android device and the server

🌐 HomeAlbum Server

HomeAlbum requires HomeAlbum Server for backup and server-related functionality.
HomeAlbum is currently designed to communicate with a server available only through:

- Your private local network (LAN)
- Tailscale

**Important**: HomeAlbum and HomeAlbum Server are not currently intended to be exposed directly to the public Internet.

Server repository:

https://github.com/matiamb/homealbumserver.git

📦 Installation

GitHub Release
- Go to the latest HomeAlbum release.
- Download the APK from the release assets.
- Install the APK on your Android device.
- Allow installation from unknown sources if Android requests it.
- Open HomeAlbum and configure your server connection from the Settings screen.

⚙️ Configuration

From the Settings screen you can configure:

- HomeAlbum Server address
- Destination folder on the server

Example server address:

`http://100.0.0.1:8080`

or when using Tailscale:

`http://tailscale-ip:8080`

**Do not expose the server directly to the public Internet with the current version of HomeAlbum.**

⚠️ Current limitations

HomeAlbum is currently a beta project.
Known limitations include:

- Only one media file can currently be uploaded manually at a time
- Media file server existence checks are currently performed one file at a time
- Some gallery animations may behave incorrectly
- Pending uploads may not always resume correctly after losing network connectivity

These limitations are expected to change during development.

🔐 Security

HomeAlbum is designed as a **self-hosted application for private networks**.

The current version should be used only with a HomeAlbum Server accessible through a trusted LAN or Tailscale network.

**Do not expose HomeAlbum Server directly to the public Internet or use the app with an internet exposed server**

Never publish server credentials, signing keys, or other sensitive configuration when reporting issues.

🤝 Contributing

Contributions are welcome.

For normal development:

- Fork the repository.
- Create your branch from dev.
- Use a descriptive branch name such as:
    feature/add-something
    fix/gallery-crash
- Make your changes.
- Run the relevant tests.
- Open a Pull Request against dev.

**Please do not open feature Pull Requests directly against master.**

See CONTRIBUTING.md for more information.

🐛 Reporting bugs

If you encounter a bug, please open a GitHub Issue and include, when possible:

- HomeAlbum version
- Android version
- Device model
- Steps to reproduce
- Expected behavior
- Actual behavior
- Relevant screenshots or logs

Please remove personal or sensitive information before attaching logs or screenshots.

🗺️ Project status

HomeAlbum is currently under active development.
Current release:
v0.1.0-beta.1
The API and application behavior may change while the project remains in beta.

📄 License

This project is licensed under the <LICENSE NAME>.

See the LICENSE file for details.
