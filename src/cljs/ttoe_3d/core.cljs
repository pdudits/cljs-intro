(ns ttoe-3d.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [ttoe-3d.geom :as g]
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
(def game (atom {}))

(session/put! :size 4)
(session/put! :dimensions 2)

(defn reset-board! []
  (reset! game
        {:board (make-board (session/get :dimensions) (session/get :size))
         :player \x
         :score {\x 0 \o 0}}))

(reset-board!)

(defn set-size! [s] (session/put! :size s) (reset-board!))
(defn set-dimensions! [d] (session/put! :dimensions d) (reset-board!))

(defn swap-player! [] (-> (swap! game update-in [:player] #(if (= % \x) \o \x)) :player))

;(swap-player!)
(defn compute-score [board coord player]
  (count
    (filter (fn [[start dir]]
              (every? #(= player (get-in board (g/line-point start dir %)))
                       (range 0 (count board))))
            (g/diagonals-over coord (count board)))))


(defn move! [coord]
  (let [cell (get-in @game coord)
        player (swap-player!)
        board-key (first coord)
        board-coord (drop 1 coord)]
    (if (= \space cell)
        (let [new-game (swap! game assoc-in coord player)
              new-board (get new-game board-key)
              score (compute-score new-board board-coord player)
              new-game (swap! game update-in [:score player] + score)
              game-over? (not-any? #(= \space %) (flatten new-board))]
          (if game-over? (navigate! "/game-over"))))))

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

(defn render-cube [coord cube]
  [:div.cube (nest render-plane coord cube)])

(defn render-hypercube [coord hc]
  [:div.hypercube (nest render-cube coord hc)])

(defn depth [b]
  (loop [[f & r] b d 0]
    (if (seqable? f) (recur f (inc d)) d)))

;(depth [[1 2][1 2]])

(defn render-board [key game]
  (let [dimensions [render-row render-plane render-cube render-hypercube]
        board (game key)
        depth (depth board)
        renderer (nth dimensions depth)]
  [renderer [key] board]))

(defn render-score [scores]
  [:table.score [:tr [:td {:rowSpan 2} "Score"]
                     (map #(vector :th %) (keys scores))]
                [:tr (map #(vector :td %) (vals scores))]])
;; -------------------------
;; Views

(defn tic-tac-toe-page []
  [:div [:h2 "Clojurescript Tic-Tac-Toe"]
   [render-score (:score @game)]
   [render-board :board @game]
   [:div [:a {:href "#/settings"} "settings"]]])

(defn select [change-handler curr-value values]
  [:select {:on-change #(change-handler (-> % .-target .-value))
            :value curr-value} ;react specific prop
           (map #(vector :option {:value %} %)
                values)])

(defn settings-page []
  [:div [:h2 "Settings"]
   [:table
    [:tr [:td [:label "Size "]]
         [:td [select set-size! (session/get :size) (range 3 6)]]]
    [:tr [:td [:label "Dimensions"]]
         [:td [select set-dimensions! (session/get :dimensions)
               (range 2 5)]]]]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn game-over-page []
  [:div [:h1 "Game Over"]
   [render-score (@game :score)]
   [:a {:href "#/" :on-click #(do (reset-board!) true)} "New game"]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'tic-tac-toe-page))

(secretary/defroute "/settings" []
  (session/put! :current-page #'settings-page))

(secretary/defroute "/game-over" []
  (session/put! :current-page #'game-over-page))
;; -------------------------
;; History
;; must be called after routes have been defined
(let [h (History.)]
  (defn  hook-browser-navigation! []
    (doto h
      (events/listen
        EventType/NAVIGATE
        (fn [event]
         (secretary/dispatch! (.-token event))))
      (.setEnabled true)))
  (defn navigate! [page]
    (.setToken h page)))

;; -------------------------
;; Initialize app
(defn init! []
  (hook-browser-navigation!)
  (reagent/render-component [current-page] (.getElementById js/document "app")))
