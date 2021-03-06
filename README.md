# twitter-personal-analyzer-bot (dev in progress...)
Analyze your interactions.

## Set up
Google Sheets is used to save the analyze result. To be able to use it, in `src\main\resources\google-credentials.json`, create the following file with your Google API account information :

```$json
{
"type":"service_account",
"project_id":"xxx",
"private_key_id":"xxx",
"private_key":"xxx",
"client_email":"xxx",
"client_id":"xxx",
"auth_uri":"http://accounts.google.com/o/oauth2/auth",
"token_uri":"https://oauth2.googleapis.com/token",
"auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs",
"client_x509_cert_url":"xxx",
"sheet_id":"<yourFileId>",
"tab_name":"<yourTabName>"
}
```

## Build args 
```
args[0] : userName [String]
args[1] : unfollowMode (false : analyzeMode) [boolean]
args[2] : includeFollowers [boolean]
args[3] : includeFollowings [boolean]
args[4] : onlyFollowBackFollowers [boolean]
args[5] : tweetArchivePath [String]
args[6] : useGoogleSheets (false: generate csv locally) [boolean]
```


## Resources :
- [Twitter API](https://developer.twitter.com/en/docs) : used to get users or tweets infos, follow & unfollow users, etc.
- [Redouane59/twitter-client](https://github.com/redouane59/twitter-client) : Custom JAVA Twitter Client
- [Google Sheets API](https://developers.google.com/sheets/api/) : used to write followed users information & statistics on an online google sheet document

