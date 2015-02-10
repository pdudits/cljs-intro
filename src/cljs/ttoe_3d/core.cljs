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
; (swap! game assoc-in [:board 0 0] \x)
; (swap! game assoc-in [:board 0 1] \o)

;; Our global state
(def game (atom {:board (make-board 2 4)}))

(session/put! :size 4)
(session/put! :dimensions 2)

(defn reset-board! []
  (swap! game
         assoc :board
               (make-board (session/get :dimensions) (session/get :size))))

(defn set-size! [s] (session/put! :size s) (reset-board!))

(defn swap-player! [] (-> (swap! game update-in [:player] #(if (= % \x) \o \x)) :player))

;(swap-player!)

(defn move! [coord]
  (let [cell (get-in @game coord)]
    (if (= \space cell)
       (swap! game assoc-in coord (swap-player!)))))

;; The components
(defn nest [f coord data]
  (map-indexed #(f (conj coord %1) %2) data))

(defn render-cell [coord cell]
  [:td {:title coord
        :on-click #(move! coord)} cell])

(defn render-row [coord row]
  [:tr (nest render-cell coord row)])

(defn render-plane [coord plane]
  [:table.plane (nest render-row coord plane)])

(defn render-board [key game]
  [render-plane [key] (game key)])

;; -------------------------
;; Views

(defn tic-tac-toe-page []
  [:div [:h2 "Clojurescript Tic-Tac-Toe"]
   [render-board :board @game]
   [:div [:a {:href "#/settings"} "settings"]]])

(defn settings-page []
  [:div [:h2 "Settings"]
   [:table
    [:tr [:td [:label "Size "]]
         [:td [:select {:on-change #(set-size! (-> % .-target .-value))
                        :value (session/get :size)} ;react specific prop
                        (map #(vector :option {:value %} %)
                                              (range 3 6))]]]
    [:tr [:td [:label "Dimensions"]] [:td [:select [:option {:value 2} "2"]]]]]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'tic-tac-toe-page))

(secretary/defroute "/settings" []
  (session/put! :current-page #'settings-page))

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
