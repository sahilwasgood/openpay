# IzzyOnDroid: the real submission (repo inclusion)

The applists tracker was the wrong door (Izzy confirmed it only covers the listings site, not repo inclusion). Inclusion requests go to the repo's own issue tracker on GitLab as a "New App:" issue.

**Status:** the wrong-tracker issue (applists #40) needs a one line comment from you pointing here, then close it.

**The one human step: open this prefilled link, log in, press Create issue:**

https://gitlab.com/IzzyOnDroid/repo/-/issues/new?issue%5Btitle%5D=New+App%3A+OpenPay&issue%5Bdescription%5D=%2A+%5Bx%5D+The+app+complies+with+the+inclusion+criteria+as+listed+%5Bin+the+README%5D%28https%3A%2F%2Fgitlab.com%2FIzzyOnDroid%2Frepo%2Fblob%2Fmaster%2FREADME.md%23what-are-the-requirements-an-app-must-meet-to-be-included-with-the-repo%29%0A%2A+%5Bx%5D+The+app+is+not+already+listed+in+the+repo+or+issue+tracker%0A%0A---------------------%0A%0A%23%23%23+Link+to+the+source+code%3A%0Ahttps%3A%2F%2Fgithub.com%2Fsahilwasgood%2Fopenpay%0A%0A%23%23%23+Link+to+app+in+another+app+store%3A%0ANone+yet%2C+this+is+the+first+listing+request.%0A%0A%23%23%23+License+used%3A%0AApache-2.0%0A%0A%23%23%23+Category%3A%0AMoney%0A%0A%23%23%23+Summary%3A%0AUPI+payments+that+work+with+zero+signal.+OpenPay+hands+every+payment+to+the+strongest+rail+that+still+works%3A+UPI+intent+when+data+exists%2C+USSD+%2A99%23+when+only+signal+does%2C+the+UPI+123Pay+IVR+line+for+Jio%2C+and+Ed25519+signed+promissory+notes+verified+fully+offline+when+nothing+does.+Notes+settle+over+normal+UPI+later.%0A%0ANotes+for+review%3A%0A-+APKs+are+attached+to+tagged+GitHub+releases+%28vX.Y.Z%29+as+OpenPay-vX.Y.Z.apk%2C+signed+with+the+project%27s+release+key+%28no+debuggable%2FtestOnly%29%2C+SHA-256+printed+in+each+release%27s+notes%3A+https%3A%2F%2Fgithub.com%2Fsahilwasgood%2Fopenpay%2Freleases%0A-+The+app+ships+WITHOUT+the+INTERNET+permission%3A+it+cannot+reach+any+server%2C+verifiable+in+the+manifest.+No+trackers%2C+no+ads%2C+no+ATS+elements+%28it+processes+payment+records%2C+so+we+hold+it+to+the+strictest+ATS+reading%29.%0A-+No+cleartext+traffic+flag%2C+nothing+downloads+executables%2C+unique+packageName+%60openpay.app%60+and+displayName+%60OpenPay%60.%0A-+Fastlane+metadata+%28title%2C+short%2Ffull+description%2C+icon%2C+changelogs%29+lives+in+the+repo+at+%60fastlane%2Fmetadata%2Fandroid%2Fen-US%2F%60.%0A-+APK+size+~3.6+MB.%0A

If the prefill does not carry over, paste `docs/izzy-issue-body.md` (matches the format of successfully included apps, e.g. WebsocketCAM #1007).

Comment to paste on applists #40 before closing it:

```
Wrong tracker on my part, apologies. Filed for repo inclusion at:
https://gitlab.com/IzzyOnDroid/repo/-/issues (New App: OpenPay). Please close.
```

## Policy check (Inclusion-Policy)

- Apache-2.0, OSI approved
- End user app, code public on GitHub with description
- Zero proprietary components, zero trackers, zero ATS (the app processes payment records, so the strictest no-ATS rule applies and is met: there is no INTERNET permission at all)
- No downloaded executables, no cleartext flag
- Release-key signed APKs attached to tagged GitHub releases (their preferred source, enables reproducible builds later)
- Unique packageName `openpay.app`, displayName `OpenPay`
- ~3.6 MB, well under the 30 MB rule of thumb
- Not a game, all-ages content

## After acceptance

Their daily checker picks up every future vX.Y.Z tag automatically. F-Droid proper (reproducible builds with their key) remains the later roadmap item.
