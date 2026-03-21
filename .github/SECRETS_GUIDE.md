# Sonara — GitHub Actions Secrets

GitHub repo > Settings > Secrets > Actions'a ekle:

## Zorunlu (Last.fm)
- `LASTFM_API_KEY` — https://www.last.fm/api/account/create
- `LASTFM_SHARED_SECRET` — ayni sayfadan

## Opsiyonel (Gemini)
- `GEMINI_API_KEY` — https://aistudio.google.com/apikey

## Workflow yaml'ina ekle:
```yaml
env:
  LASTFM_API_KEY: ${{ secrets.LASTFM_API_KEY }}
  LASTFM_SHARED_SECRET: ${{ secrets.LASTFM_SHARED_SECRET }}
  GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
```
