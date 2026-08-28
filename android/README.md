# สั่งครับ Android

Native Android shell for restaurant staff/kitchen devices.

- Package: `com.sangkub.kitchen`
- Kotlin + Jetpack Compose
- Opens the production kitchen experience at `https://sangkub.vercel.app/kitchen`
- JavaScript and DOM storage enabled so Supabase Auth sessions persist.
- Audio permission requested for future native voice/notification integration.

This first shell deliberately reuses the production web kitchen so Android and web share the same auth, realtime order queue, and business rules while the native notification/voice service is added next.
