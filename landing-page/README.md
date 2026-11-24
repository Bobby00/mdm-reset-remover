# MDM Reset Remover — Landing Page

Static landing page for the MDM Reset Remover Android app. Hosted on Netlify.

## Structure

```
landing-page/
├─ index.html          # Main landing page
├─ css/
│  └─ style.css        # Styles
├─ js/
│  └─ main.js          # Interactivity (FAQ accordion, smooth scroll)
├─ netlify.toml        # Netlify build config
├─ _redirects          # SPA fallback redirect
└─ README.md
```

## Deploy to Netlify

1. Create a new repository (separate from PawPrint)
2. Push this `landing-page/` folder to the repo
3. Connect the repo to Netlify
4. Set build command: `echo 'skip'` (or leave empty)
5. Set publish directory: `.` (root of the repo)
6. Deploy

## Custom Domain

Add your custom domain in Netlify Domain Management and update the APK download link in `index.html` to point to your actual APK hosting URL.

## APK Download Link

Update the download button href in `index.html`:

```html
<a href="https://your-apk-hosting.com/mdm-reset-remover-v1.0.0.apk" class="btn btn-primary btn-large">Download APK</a>
```

Currently points to a placeholder: `https://example.com/releases/mdm-reset-remover-v1.0.0.apk`