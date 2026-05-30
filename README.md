# crawlcipher-android

CrawlCipher Android wrapper projesi.

## Amaç
Bu repo, `https://github.com/rvoidex7/CrawlCipher` ana projesini Android'de çalıştırmak için sade bir wrapper uygulaması sunar.

- Harici Termux kurulumu gerektirmeyen gömülü terminal benzeri arayüz
- Dokunmatik komut girişi
- İlk sürüm için harici klavye ile Enter tabanlı kontrol desteği
- Oyunun paketlenmiş binary/assets entegrasyonu için köprü sınıfları

## Kurulum ve Çalıştırma
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

> Not: Gerçek CrawlCipher paketleri/binary bağlantısı bu repoda bilinçli olarak placeholder bırakılmıştır; kendi ortamında sonradan bağlanacak şekilde tasarlanmıştır.

## Play Store Dağıtım
Fastlane dosyaları hazırdır:

```bash
bundle install
bundle exec fastlane android build_release_bundle
bundle exec fastlane android deploy_internal
```

Gerekli secret:
- `GOOGLE_PLAY_JSON_KEY`: Google Play service account JSON anahtarı (workflow veya local env)

GitHub Actions workflow: `.github/workflows/android-release.yml`
