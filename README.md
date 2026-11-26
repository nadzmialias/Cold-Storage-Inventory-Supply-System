# Cold Storage Inventory & Supply System (CSISS)

CSISS is a cold-chain inventory, supply management, and IoT temperature monitoring system built for **BITU3923 Workshop 2 (UTeM)**. It connects an Android Jetpack Compose app, an ESP32 DS18B20 temperature node, and a Supabase (PostgreSQL + Auth + Storage + Edge Functions + Realtime) backend.

## Repository Structure

| Folder | Description |
| :--- | :--- |
| `android/` | Native Android app (Kotlin, Jetpack Compose, Material 3, Supabase KT SDK) |
| `iot/csiss_esp32/` | ESP32 firmware reading DS18B20 temperature, driving a 16x2 I2C LCD, and syncing with Supabase |
| `supabase/` | Supabase CLI configuration, database migrations, full live schema snapshot (`schema_snapshot.sql`), and Deno Edge Functions |
| `docs/` | UML diagrams, system architecture, and project documentation |

## Quick Start

### 1. Android App (`android/`)
1. Copy `android/local.properties.example` to `android/local.properties`.
2. Fill in `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, and your local `sdk.dir` (Android Studio sets `sdk.dir` automatically on first open).
3. Open the `android/` directory in Android Studio and run **Sync Project with Gradle Files**.
   - **Build output note:** `android/build.gradle.kts` currently redirects Gradle build artifacts to `%TEMP%/csiss-build/` to avoid local file-locking issues. Always run Gradle Sync after pulling changes so Android Studio installs the freshly built APK rather than any stale local `android/app/build/` folder.
4. Build and run on an Android device/emulator (`minSdk 26`).

### 2. Supabase Backend (`supabase/`)
- **Migrations:** `supabase/migrations/` tracks recorded remote migrations.
- **Full Live Schema Snapshot:** `supabase/schema_snapshot.sql` captures the complete current PostgreSQL schema (all 15 tables, constraints, indexes, functions, triggers, RLS policies, and `product-images` bucket).
- **Edge Functions:**
  - `supabase/functions/ingest-temperature` (`verify_jwt = false`, device-key authenticated)
  - `supabase/functions/get-temperature-limits` (`verify_jwt = false`, device-key authenticated)
  - `supabase/functions/manage-users` (`verify_jwt = true`, Admin-only)

### 3. ESP32 Firmware (`iot/csiss_esp32/`)
See [`iot/README.md`](iot/README.md) for wiring, required Arduino libraries, and `secrets.h` setup.
