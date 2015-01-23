(ns ttoe-3d.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
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

(def game (atom {:board board}))

(defn show-row [row]
  [:tr (map (fn [c] [:td c]) row)])
(defn show-plane [plane]
  [:table.plane (map show-row plane)])
;(vec4 \a)

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Tic Tac Toe 3D"]
   (show-plane (@game :board))])

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
