# Redirects the wrong-tracker issue (#40 on applists) and closes it.
# Prerequisite: glab auth login --hostname gitlab.com

glab issue comment 40 \
  --repo IzzyOnDroid/applists \
  --message 'Wrong tracker on my part, apologies. Filed for repo inclusion at:
https://gitlab.com/IzzyOnDroid/repo/-/issues (New App: OpenPay). Please close.
'

glab issue close 40 --repo IzzyOnDroid/applists
