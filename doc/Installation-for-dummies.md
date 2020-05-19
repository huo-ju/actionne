# Install `actionne-twitter` for Dummies  

An absolutely user-friendly guide for dummies to deploy `actionne-twitter`.

## Prerequisites

* Clojure >= 1.9.0
* JDK >= 11
* A valid [Twitter Developer account](https://developer.twitter.com/en) with at least one active app

## Step 1: Get the Essentials

Once confirmed that prerequisites have all been satisfied, you'll need to download the following executes:

* [actionne_twitter.jar](https://github.com/virushuo/actionne-twitter/releases): the actual actionne Twitter plugin
* [actionne-0.1.0-SNAPSHOT-standalone.jar](https://github.com/virushuo/actionne/releases): the underlying actionne framework

## Step 2: Run `actionne` & Confirm Folder Structure

Command to run actionne: `java -Dhomedir="/YOUR_PATH" -jar actionne-0.1.0-SNAPSHOT-standalone.jar`

actionne will create a directory called `/YOUR_PATH` and exit with errors due to missing configuration files. Before calling actionne again, ensure that `/YOUR_PATH` has a similar structure as below:

```
├── /YOUR_PATH
│   ├── config
│   │   ├── default.edn
│   ├── data
│   ├── plugins
│   │   ├── actionne_twitter.jar
│   ├── scripts
│   │   ├── xxxx.act
│   └── actionne-0.1.0-SNAPSHOT-standalone.jar
```

## Step 3: Edit Files

### Scripts
Rule scripts should be saved under `scripts/`. The header must start with the following lines (case sensitive):

```
Ver 1.0.0
Namespace your_user_name/actionne_twitter
Desc your_rule
```

Substitute "your_user_name" and "your_rule" above based on your needs.

An example rule: `Do delete created_at laterthan 12 hour category = str:reply` - this rule will call the `delete` function from the `actionne_twitter` plugin, and remove tweets that meet the criteria:

* Tweets that are created 12 hours ago, AND
* Category of the tweet = "reply"

All conditions within a rule are joint through AND. This script ([myscript.act](../examples/scripts/myscript.act)) contains more complex instances.

### Config Files

The purpose of config files is to determine the name of the script, execution cycle, environment variables, etc. It should be stored under the `config/` folder with filename `default.edn` (otherwise, the code will panic and exit with errors).

A sample, bare-minimum config file:

```
{ :actionne_twitter {
    :consumer-key "your-consumer-key-here"
    :consumer-secret "your-consumer-secret-here"
    :screen_name "your-screen-name"
    :watching "5 days"
    :backup true
    :dryloop false
 }
 :scripts{
    :myscript 3600
 }

```
The `:actionne_twitter` block specifies config details for the `actionne_twitter` plugin.

* `consumer-key` and `consumer-secret` could be obtained through [Twitter developer site](https://developer.twitter.com/en) (a valid app is required). Substitute your `screen_name` (e.g. `@momopirin`) after `:screen_name`. For testing purpose, you could set `:dryloop true` so that the tool will only detect tweets that meet the designated criteria, but not take any actions (e.g. delete tweets).

Similarly, `:scripts` block stores information about available scripts. The example above assumes that there is a file called `myscript.act`, under `/YOUR_PATH/scripts`. The script should be run every hour (a.k.a. 3600 seconds).

## Step 4: Run `actionne`, for Real...

`cd` into `/YOUR_PATH`, and run the command again: `java -Dhomedir="/YOUR_PATH" -jar actionne-0.1.0-SNAPSHOT-standalone.jar`

If everything is set up correctly, you should see the following prompt on the screen:

`INFO: loading... /YOUR_PATH/plugins/actionne_twitter.jar`

It will then redirect you to an URL to finish Twitter OAuth setup. Log in with your credential, copy & paste the passcode into the command line.

actionne should be up and running then! You should be able to find user data under `/YOUR_PATH/data/`. As long as the session remains active, the script is supposed to check & perform corresponding actions continuously, subject to the cycle time stated in the configuration file.

## Tips

* If there exists a rule that will never be False (e.g. endless loop), its matching action will run forever. You may utilize this to remove all your tweets. actionne_twitter is not subject to the 3,200 tweet limit from the official Twitter API; instead, it obtains tweets from the web interface.
* Keep in mind that reserved words (e.g. `Namespace`) in a config file is case sensitive. Misspelling reserved words (e.g. `namespace`) will invoke weird errors, e.g. `nth not supported on this type`.
* Script name in the config file must match with the name of the `.act` file under `/YOUR_PATH/scripts`.
* A valid pair of `consumer-key` and `consumer-secret` is required. There are some unofficial consumer keys in the wild; try them at your risks.  

## Ways to Keep it Running

There are multiple ways to do so. I just listed a couple that I've tried so far:

1. Open a console window and have it running. Remember to re-run the command if you lose connection to the console, though.
2. Have the program running in `tmux` ([cheatsheet](https://tmuxcheatsheet.com/)). Not fun if you're a restart freak like me!
3. Set up `systemctl`. **Recommend, pretty stable.**
4. Ask the author to come up with a Docker version. Hopefully he will create one soon :)
