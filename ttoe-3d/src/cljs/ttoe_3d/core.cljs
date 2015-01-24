(ns ttoe-3d.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [clairvoyant.core :as trace :include-macros true]
              [goog.history.EventType :as EventType])
    (:import goog.History))

; one row would be:
;[\space \space \space \space]

(defn vec4 [x] (vec (replicate 4 x)))
(def DIMENSIONS 2)

;(vec4 \space)
;(vec4 (vec4 \space))

;(-> \space vec4 vec4 vec4)
;((comp vec4 vec4 vec4) \space)

(def board ((apply comp (replicate DIMENSIONS vec4)) \space))

(def game (atom {:board board :player \o}))

(defn turn-player! [] (:player (swap! game update-in [:player] #(if (= % \x) \o \x))))
;(turn-player!)

;after you spend a day golfing the interface to consist of three simple functions, you need a
;tracer to actually understand what you've written,  and why it breaks so bad

;(trace/trace-forms {:tracer trace/default-tracer}
(defn show-cell [cell coords nest]
  [:td {:title (str coords)
        :on-click #(swap! game assoc-in (cons :board coords) (turn-player!))}
       cell])

(defn show-row [row coords nest]
  (cons :tr (nest)))

(defn show-plane [plane coords nest]
  (cons :table.plane (nest)))

;(@game :board)
;(swap! game assoc-in [:board 0 1] \x)

;(defn logged [x] (.log js/console (clj->js x)) x)

(defn render
  "Iterate data structure and for each nesting level apply the next function from the
  fs collection. Functions take three arguments [data coordinates & nest-function], where
  data is corresponding part of data, coordinates is vector of all keys, with current
  key being the last, and optional nest function is the one to apply to call to process
  next level of data. nest-function is optional and is nil for last defined nesting level.
  Nest function returns a vector of children elements."

  ([data fs] (render data [] fs))
  ([data coords [f & fs]]
   ; the magic is the nesting function - iterates the data and applies the first function on
   ; each element
   (let [nest (defn nest [data coords [f & fs]]
                (let [iter-fn (cond (map? data)     map
                                    (seqable? data) map-indexed
                                    :else           (fn [f data] (f nil data)))
                      render-fn (fn [key d]
                                  (let [new-coords (if (nil? key) coords (conj coords key))]
                                    (vec (f d new-coords #(nest d new-coords fs)))))]
                  (iter-fn render-fn data)))]
   ; we convert the result into vector, as that's what reagent expects
   (vec (f data coords #(nest data coords fs))))))
;) ;trace-forms
;(render 1 [0 0] [show-cell])
;(render [1 2 3 4] [0] [show-row show-cell])
;(render (@game :board) [show-plane show-row show-cell])

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Tic Tac Toe 3D"]
   (render (@game :board) [show-plane show-row show-cell])])

(defn about-page []
  [:div [:h2 "About ttoe-3d"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn init! []
  (hook-browser-navigation!)
  (reagent/render-component [current-page] (.getElementById js/document "app")))
