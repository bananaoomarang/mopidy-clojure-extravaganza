# Mopidy Clojure Extravaganza 

Learning exercise/demo music player in need of tidying.

Connects to a local [Mopidy](https://www.mopidy.com/) server and uses [Reagent](https://reagent-project.github.io/) to display a UI.

Hey, [Clojurescript](http://clojurescript.org/) is neat, huh?

## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 
