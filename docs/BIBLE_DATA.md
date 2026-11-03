# Bible data: translations, licensing, sourcing

**No Bible text is distributed in this repository.** The app ships the machinery
to read modules; you supply the modules.

That is a legal position, not a convenience. Getting this wrong is the single
most likely way this project causes you a real problem.

## Public domain / open texts

These can be bundled or hosted, subject to confirming the specific edition.

| Code | Translation | Status |
|---|---|---|
| KJV | King James Version (1611/1769) | Public domain. In the UK the Crown holds a perpetual patent; in practice this does not restrict app use elsewhere, but worth knowing. |
| ASV | American Standard Version (1901) | Public domain. |
| WEB | World English Bible | Released to the public domain by its editors. |
| TAB | Ang Biblia (1905) | Published 1905; public domain on age. I believe this is clear, but confirm the edition you source. |
| CEB | Cebuano Bugna (1917) | **Least certain of the five.** Published 1917, so age suggests public domain, but I have not verified the status of specific modern digitisations, which may carry their own claims. Check before distributing. |

I am not a lawyer and these are not legal opinions. For anything you plan to
distribute at scale, confirm with the publisher or a lawyer.

## Licensed translations

| Code | Publisher | My understanding — verify |
|---|---|---|
| ESV | Crossway | Offers an official API requiring a free key, with rate limits, caching restrictions and mandatory attribution. The most realistic of the three to integrate. |
| NIV | Biblica / Zondervan | I am **not** aware of a generally available developer API. Assume a commercial licence is required. |
| NKJV | Thomas Nelson / HarperCollins | Same — assume commercial licence. |

These change. Do not treat the presence of `ApiTranslationProvider` in this
codebase as evidence that any translation is cleared for your use.

**Never:**
- bundle licensed text in the APK
- ship an API key in the APK (users enter their own)
- cache licensed text to disk unless the licence explicitly permits it —
  `ApiTranslationProvider.cachePolicy` defaults to `NONE` for this reason

## User-supplied modules

`ModuleImporter` converts Zefania XML, OpenSong XML, or USFX files the user
already owns. The app converts a file the user has; it does not fetch or
redistribute. Keep it that way.

Implementation note: use `XmlPullParser` streaming, not DOM. A full Bible is
31,000+ verses and several MB; a DOM parse will OOM on the low-RAM devices this
app targets. Insert in batches inside one transaction.

## Attribution

`SettingsScreen` shows each module's copyright line next to the module, always
visible rather than buried in an About screen. Several licences require
attribution wherever the text appears — keep it there.
