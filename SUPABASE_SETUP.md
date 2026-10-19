# UniFind — Supabase setup (replaces Firebase)

The app no longer uses Firebase at all (Auth, Firestore, Storage and the
`google-services` Gradle plugin have been removed). It now runs on
**Supabase** — Auth + Postgres + Realtime + Storage.

You need ~5 minutes and nothing but a browser.

## 1. Create the project

1. Go to <https://supabase.com> → **New project** (the free tier is plenty).
2. Pick a name/password/region and wait ~2 minutes for it to boot.

## 2. Copy the credentials into the project

1. In Supabase: **Project Settings → API**.
2. Copy the **Project URL** and the **anon public key**.
3. Open `gradle.properties` in the project root and replace the two placeholders:

```properties
supabase.url=https://YOUR-PROJECT-REF.supabase.co
supabase.anonKey=YOUR-ANON-KEY
```

They are baked into `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY`
at build time — no secrets in source code, and the anon key is safe to ship
(it only allows what the row-level security policies below permit).

## 3. Create the database

1. In Supabase: **SQL Editor → New query**.
2. Paste the entire contents of [`supabase/schema.sql`](supabase/schema.sql) and press **Run**.

This creates:

| Was (Firebase)       | Now (Supabase)                                   |
| -------------------- | ------------------------------------------------ |
| Firestore `users`    | table `public.users` + RLS (own row only)        |
| Firestore `items`    | table `public.items` + RLS (signed-in readable)  |
| Firestore `chats`    | table `public.chats` + RLS (participants only)   |
| `chats/{id}/messages`| table `public.messages` + RLS (participants)     |
| Firestore `notifs`   | table `public.notifications` + RLS (recipient)   |
| Snapshot listeners   | Supabase Realtime (added to the publication)     |
| ImgBB image uploads  | Storage bucket `item-images` (public read)       |

## 4. Turn email confirmation OFF (recommended)

**Authentication → Sign In / Providers → Email → Confirm email = OFF**

With confirmation on, sign-up still works — the app will show
*"Check your inbox to confirm your email, then sign in."* — but turning it off
keeps the original Firebase behavior (sign-up logs you straight in).

## 5. Google sign-in ("Continue with Google")

The button opens the phone's Google account picker (Android's Credential
Manager), then exchanges the chosen account's Google ID token for a Supabase
session. Three places must agree on the same Google Cloud OAuth project:
Google Cloud Console, the Supabase dashboard, and `gradle.properties`.

### 5.1 Google Cloud Console

1. Open the [Google Auth Platform](https://console.cloud.google.com/auth/overview)
   and create/select a project.
2. **Audience** → *External*. While it is in *Testing*, only the Google
   accounts you list as test users can sign in — publish it when you are ready
   for the whole campus.
3. **Data Access** → keep the default scopes (`openid`,
   `.../userinfo.email`, `.../userinfo.profile`).
4. **Clients** → *Create client* → **Web application**:
   - Under *Authorized redirect URIs* add the callback URL shown on Supabase's
     Google provider page: `https://<project-ref>.supabase.co/auth/v1/callback`
   - Save the **Client ID** and **Client secret**.
5. **Clients** → *Create client* → **Android**:
   - Package name: `com.example.campuslostfound`
   - SHA-1 of the keystore your build is signed with — for the debug build run:

     ```powershell
     .\gradlew.bat signingReport
     ```

     (before shipping a signed release, add that keystore's SHA-1 too)

### 5.2 Supabase dashboard

**Authentication → Sign In / Providers → Google** → enable, then:

| Field         | Value                                                        |
| ------------- | ------------------------------------------------------------ |
| Client IDs    | `<web client id>,<android client id>` — web first, comma-separated |
| Client secret | the **Web** client's secret                                   |

Leave *Skip nonce check* **off**: the app sends a nonce and Supabase verifies
it, which is what makes the ID-token exchange replay-proof.

### 5.3 gradle.properties

```properties
google.webClientId=1234-abcdef.apps.googleusercontent.com   # the WEB client ID
```

Without it the button still appears, but it tells the user Google sign-in is
not set up instead of opening an account picker Google would reject.

## 6. Restrict accounts to school (.edu.ph) emails

The app already refuses every non-`.edu.ph` address — in the sign-up / sign-in
forms, after the Google account picker, and when the email is edited on the
profile screen. To make the rule impossible to bypass with a modified APK:

1. SQL Editor → New query → paste
   [`supabase/restrict_edu_ph.sql`](supabase/restrict_edu_ph.sql) → **Run**.
2. **Authentication → Hooks** → add the **Before User Created** hook →
   *Postgres function* → `public.hook_restrict_signup_by_email_domain` →
   **Create**.
3. Try to sign up with a `@gmail.com` address: Supabase now answers with
   *"Please use your school email ending in .edu.ph …"* and no user is
   created.

Accounts that already exist keep working — the hook only inspects users that
are about to be created.

## 7. Build & run

```powershell
java -classpath ".\gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain :app:assembleDebug
```

## What changed in the code

- `SupabaseClient.kt` (new) — the single `SupabaseProvider.client`, plus
  `liveTableFlow()`, the Firestore-snapshot equivalent: it emits a query result
  and re-runs it whenever Realtime reports a change on that table.
- `AuthViewModel` — FirebaseAuth → Supabase Auth (`signUpWith/signInWith/
  updateUser/resetPasswordForEmail`). Public API unchanged, screens untouched.
- `ItemRepository` / `ChatRepository` / `NotificationRepository` — Firestore
  listeners/queries → Postgrest selects + `liveTableFlow`.
- `ImageRepository` — ImgBB → Supabase Storage (`item-images` bucket).
- Models (`Item`, `Message`, `Chat`, `Notification`, `User`) — now
  `@Serializable`, snake_case columns via `@SerialName`, timestamps changed
  from `com.google.firebase.Timestamp` to plain `Long` epoch milliseconds.
- `SchoolEmail.kt` (new) — the one place that defines the `.edu.ph` rule.
  The sign-up/sign-in forms, the Google picker result and the profile edit all
  call it, and `supabase/restrict_edu_ph.sql` applies the same rule server-side.
- `GoogleSignIn.kt` (new) — Credential Manager account picker; returns the
  chosen account's Google ID token + nonce, which
  `AuthViewModel.signInWithGoogle` exchanges for a Supabase session (the nonce
  is verified by Supabase).
- `LoginScreen` — "Continue with Google" is wired to the picker (it used to be
  a placeholder), and login/sign-up now label the field **School Email**.
- Deleted: `app/google-services.json`, all `com.google.firebase:*` deps,
  `com.google.gms.google-services` plugin, Retrofit/ImgBB code.

## Troubleshooting

- **Everything returns empty lists** → RLS blocked the query. Check you ran
  `schema.sql` and that you're signed in (policies target `authenticated`).
- **Data loads but never live-updates** → the tables must be part of the
  `supabase_realtime` publication (step 4 of the SQL does this; verify under
  **Database → Replication**).
- **Photos don't upload** → the `item-images` bucket must exist (created by the
  SQL) and the upload policy must allow `authenticated`.
- **"invalid login credentials"** → wrong email/password, or the user was
  created in the old Firebase project — Supabase starts empty.
- **"Google sign-in isn't set up yet"** → `google.webClientId` is still the
  placeholder; fill in the Web client ID (step 5.3) and rebuild.
- **Picker shows no accounts, or "Couldn't open Google sign-in"** → the Android
  OAuth client (package name + SHA-1) is missing from the Google Cloud project,
  or Google Play services is out of date on the device.
- **Account picked, but "use your school email" is shown** → that Google
  account is not an `.edu.ph` address. This is the intended rule.
- **"nonce mismatch" from Supabase** → *Skip nonce check* must stay off; the app
  hashes the nonce exactly once (SHA-256 hex to Google, the raw value to
  Supabase).
