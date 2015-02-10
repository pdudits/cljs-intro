(ns ttoe-3d.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(defn vec-n [n x] (vec (repeat n x)))

; this is bit too high order for starter
;(defn make-board [dimensions size] ((apply comp (repeat dimensions (partial vec-n size))) \space))

; recursive version
(defn make-board [dimensions size]
  (if (= 1 dimensions) (vec-n size \space)
                       (vec-n size (make-board (dec dimensions) size))))
; (make-board 2 3)


;; -------------------------
;; Views

(defn tic-tac-toe-page []
  [:div [:h2 "Clojurescript Tic-Tac-Toe"]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About ttoe-3d"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'tic-tac-toe-page))

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
