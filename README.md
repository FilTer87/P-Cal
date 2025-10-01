# P-Cal 📅
Leggi la [versione in italiano qui](./README.it.md)

A simple and modern **personal calendar** web application, featuring advanced event management and **programmable reminders**, with a **multi-channel notification** system.

**P-Cal** (Private Calendar) is designed as a useful self-hostable, modern, and multi-user calendar, aimed at individuals or small businesses who want to have **full control over their data**.

The software is **free and privacy-oriented**: completely free of trackers or data collection, it can be installed on your own servers to guarantee maximum confidentiality.  

**NOTE:** *As this is a personal project, maintenance and new features implementation will depends on my free available time. Receiving a tip or donation is definitely the best way to keep me on track!* 🙂 


### 📸 Screenshots

#### Desktop
<p>
    <img src="docs/images/month_dark.png" width="45%" alt="Month view" style="margin: 0.5rem;">
    <img src="docs/images/month_light.png" width="45%" alt="Month view light" style="margin: 0.5rem;">
    <img src="docs/images/week_dark.png" width="45%" alt="Week view" style="margin: 0.5rem;">
    <img src="docs/images/week_light.png" width="45%" alt="Week view light" style="margin: 0.5rem;">
    <img src="docs/images/agenda_dark.png" width="45%" alt="Agenda view" style="margin: 0.5rem;">
    <img src="docs/images/day_dark.png" width="45%" alt="Day view" style="margin: 0.5rem;">
    <img src="docs/images/user-settings_1.png" width="45%" alt="settings-1" style="margin: 0.5rem;">
    <img src="docs/images/user-settings_2.png" width="45%" alt="settings-2" style="margin: 0.5rem;">
    <img src="docs/images/update.png" width="45%" alt="Update" style="margin: 0.5rem;">
    <img src="docs/images/detail.png" width="45%" alt="Detail" style="margin: 0.5rem;">
</p>

#### Mobile
<p>
   <img src="docs/images/month_mobile.png" width="32%" alt="Month view mobile" style="margin: 0.5rem;">
   <img src="docs/images/week_mobile.png" width="32%" alt="Week view mobile" style="margin: 0.2rem;">
   <img src="docs/images/agenda_mobile.png" width="32%" alt="Agenda view mobile" style="margin: 0.5rem;">
   <img src="docs/images/day_mobile.png" width="32%" alt="Day view mobile" style="margin: 0.5rem;">
   <img src="docs/images/user-settings_dark_mobile.png" width="32%" alt="Settings mobile 1" style="margin: 0.5rem;">
   <img src="docs/images/user-settings_light_mobile.png" width="32%" alt="Settings mobile 2" style="margin: 0.5rem;">
</p>


### ✨ Main Features

#### 📊 **Complete Calendar Management**
- **Multiple views**: Month, Week, Day, and Agenda
- **Event/task management** with customizable colors, location, and descriptions
- **Smart time visualization** distinguishing past and future events
- **Precise hourly grid** in week view with visual indicators for off-screen activities
- **Informative tooltips**

#### ⏰ **Advanced Reminder System**
- **Multi-channel notifications**: currently implemented Email and NTFY server
- **Multiple reminders** per activity
- **Flexible scheduling** (minutes, hours, days before the event)
- **Automatic management** of expired notifications

#### 👤 **User and Preferences Management**
- **Registration and login**
- **User profile** with basic info
- **Security section** to change password and enable 2FA
- **Full data export** (GDPR-friendly)
- **Account deletion** with data removal

#### ⚙️ **Preferences** (per user):
  - **Theme** (light/dark/automatic)
  - **Time zone** (required for correct notifications delivery)
  - **Time format** (12h/24h)
  - **First day of the week** (Monday/Sunday)
  - **Enable/disable notifications** by type
  - **Edit personal NTFY topic**

#### 🎨 **User Experience**
- **Responsive design** optimized for desktop and mobile
- **Modern interface** with Tailwind CSS
- **Adaptive theme** based on OS
- **Optimized performance** with lazy loading
- **Past events** collapsible in day/agenda views

#### 🔐 Security
- **Password hashing** with BCrypt (strength 12)
- **JWT tokens** with configurable expiration
- **Complete server-side input validation**
- **Configurable CORS protection**
- **Two-Factor Authentication** TOTP
- **Secure password reset** via time-limited email
- **Data isolation** per user (API level)


### 🚀 Quick Start

#### Installation

```bash
# 1. Clone the repository
git clone https://github.com/FilTer87/P-Cal
cd P-Cal

# 2. Create and configure environment file
cp .env.example .env
nano .env  # Edit .env file with your configuration

# 3. Start the application
docker compose up --build -d

# 4. Access the application
# Frontend: http://localhost
```
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

### ⚙️ Configuration

**See [Installation.md](docs/Installation.md) for complete installation and configuration instructions.**


### 🏗️ Architecture

**Frontend - TypeScript / Vue.js 3** with Composition API, Pinia, Vite, Tailwind CSS

**Backend - Java 17 / Maven / Spring Boot 3.2** with Spring Security, JPA/Hibernate, PostgreSQL, Lombok

**Database - PostgreSQL 15** + H2 fot automatic tests


#### ⚡ Optimizations
- **DB indexing**
- **Lazy loading and pagination**
- **Code splitting** with Vite
- **Tree shaking**
- **Caching**


### 🎯 Future Roadmap

#### 🔧 Possible future features under evaluation
- **Event categories** with global filter (alternative to "multi-calendars")
- **Recurring events** with customizable patterns
- **Multilanguage support**
- **Drag & Drop** to move events/tasks within grids with automatic update
- **Advanced user session management**
- **Additional notification channels**: Gotify, Slack, Telegram, ...
- **CalDAV integration**
- **Data import** from user export
- **Data import** from other calendars
- **Event sharing** among users (invitation management)

### 📱 Possible expansions
- **Mobile API** for native apps
- **Shared calendar** for multiple users
- **Plugin system** for integrations


### 📄 License

This project is released under the **[MIT License](./LICENSE)**.


### 📞 Support

#### Documentation
- **Swagger UI**: available at `/swagger-ui.html`
- **OpenAPI JSON**: available at `/v3/api-docs`
- **Code comments**: Javadoc and TSDoc
- **Architecture docs**: Work in progress!

🐛 **Bug reports**: Use issue template


---

**Developed with ❤️ to manage events and activities to remember in a simple and effective way, while keeping full control over your data**