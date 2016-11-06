(ns mopidy-clojure-extravaganza.core
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string :as str]))

(enable-console-print!)

(defn jlog [& args]
  (apply js/console.log args))

(defn jlog-error [err]
  (js/console.error err))


;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:playing? false
                          :current-track {}
                          :search-spotify false
                          :search-local false
                          :mopidy-online false
                          :queue []
                          :results [] }))

;; Setup mopidy
(def mopidy (new js/deps.Mopidy (clj->js { :webSocketUrl "ws://localhost:6680/mopidy/ws"})))
(.on mopidy "state:online" (fn []
                             (println "Mopidy online!!")
                             (swap! app-state assoc :mopidy-online true)))

(.on mopidy "event:trackPlaybackStarted"
     (fn [e]
       (println "Track changed!")
       (swap! app-state assoc :current-track (.. e -tl_track -track))))

;;(set! js/mopidy mopidy)
;;(.on mopidy jlog)

(defn mopidy-online? []
  (:mopidy-online @app-state))

(defn search-places []
  (let [spotify (:search-spotify @app-state) local (:search-local @app-state)]
    (filter some? [(if spotify "spotify:" nil) (if local "local:" nil)])))

(defn mopidy-safe [f]
  (fn [& args]
    (if (mopidy-online?)
      (apply (partial f) args)
      (do (println "Mopidy online!") (new js/Promise #())))))

(defn mopidy-search-unsafe [query]
  (jlog (clj->js (search-places)))
  (let [search (.. mopidy -library -search)]
    (let [res (search (clj->js { :any [query] }) (clj->js (search-places)))]
      (.catch res jlog-error)
      (.then res (fn [tracks]
                   (->> tracks
                        (js->clj)
                        (first)
                        (#(get % "tracks"))
                        ))))))

(defn mopidy-queue-unsafe [tracks]
  (js/console.log (get tracks 0))
  (let [res ((.-add (. mopidy -tracklist)) (clj->js tracks))]
    (.catch res #(js/console.error %))
    res))

(defn mopidy-play-unsafe []
  (if (:playing? @app-state)
    (.pause (. mopidy -playback))
    (.play (. mopidy -playback)))
  (swap! app-state assoc :playing? (not (:playing? @app-state))))

(defn mopidy-skip-unsafe [back?]
  (if back?
    (.previous (. mopidy -playback))
    (.next (. mopidy -playback))))

(defn mopidy-clear-queue-unsafe []
  (.clear (. mopidy -tracklist))
  (swap! app-state assoc :current-track {}))

(defn mopidy-get-track-list-unsafe []
     (let [res (.getTlTracks (. mopidy -tracklist))]
       (.catch res jlog-error)
       (.then res (fn [tracks]
                    (->> tracks
                        (js->clj)
                        (map #(get % "track")))))))

(def mopidy-search (mopidy-safe mopidy-search-unsafe))
(def mopidy-queue (mopidy-safe mopidy-queue-unsafe))
(def mopidy-play (mopidy-safe mopidy-play-unsafe))
(def mopidy-skip (mopidy-safe mopidy-skip-unsafe))
(def mopidy-clear-queue (mopidy-safe mopidy-clear-queue-unsafe))
(def mopidy-get-track-list (mopidy-safe mopidy-get-track-list-unsafe))

(defn clear-result []
  (swap! app-state assoc :results []))

(.on mopidy "event:tracklistChanged"
     (fn [e]
       (println "Tracklist changed!")
       (.then (mopidy-get-track-list) #(swap! app-state assoc :queue %))))


(defn hello-world []
  [:h1 (:text @app-state)])

(defn search-box []
  [:div.search-box
   [:input.field { :on-change #(if (str/blank? (.. % -target -value))
                           (swap! app-state assoc :results [])
                           (.then
                            (mopidy-search (.. % -target -value))
                            (fn [res]
                              (swap! app-state assoc :results res))))}]])

(defn search-check [label handler]
  [:label
   [:input.search-check {:type "checkbox"
                         :defaultChecked true
                         :on-click (if handler handler #(println "Check unhandled")) }] label])

(defn search-opts []
  [:div.search-opts
   [search-check "Local" #(do
                            (swap! app-state assoc :search-local (.. % -target -checked))
                            (clear-result))]
   [search-check "Spotify" #(do
                              (swap! app-state assoc :search-spotify (.. % -target -checked))
                              (clear-result))]])

(defn search-bar []
  [:div.search-bar
   [search-box]
   [search-opts]])

(defn get-track-at [el]
  (get (:results @app-state) (js/Number (.. el -dataset -track))))

(defn track-item [track, index]
  [:li.track
   {
    :key index
    :data-track index
    :on-click #(mopidy-queue [(get-track-at (. % -target))])}
   (str/join " - "
             [(get track "name")
              (get-in track ["album" "name"])
              (get-in track ["artists" 0 "name"])])]
  )

(defn track-list [elements clickable?]
  [:ul { :class (if clickable? "track-list -clickable" "track-list")}
   (map-indexed #(track-item %2 %1) elements)])

(defn controls []
  [:div
   [:button {:on-click #(mopidy-play)} (if (:playing? @app-state) "Pause" "Play!")]
   [:button {:on-click #(mopidy-clear-queue)} "Clear queue"]
   [:button {:on-click #(mopidy-skip true)} "Prev"]
   [:button {:on-click #(mopidy-skip false)} "Skip"]])

(defn app []
  [:div.container
   [:h1
    (str/join " - "
              ["Music"
              (. (:current-track @app-state) -name)])]
   [controls]
   [:hr]
   [:div.half
    [search-bar]
    [track-list (:results @app-state) true]]
   [:div.half
    [:h2 "Queue"]
    [track-list (:queue @app-state)]]])

(reagent/render-component [app]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! app-state update-in [:__figwheel_counter] inc)
)
