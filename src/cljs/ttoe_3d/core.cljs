(ns ttoe-3d.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [clairvoyant.core :as trace :include-macros true]
              [ttoe-3d.geom :as g]
              [goog.history.EventType :as EventType])
    (:import goog.History))

; one row would be:
;[\space \space \space \space]
(def SIZE 4)
(defn vecS [x] (vec (replicate SIZE x)))
(def DIMENSIONS 3)

;(vec4 \space)
;(vec4 (vec4 \space))

;(-> \space vec4 vec4 vec4)
;((comp vec4 vec4 vec4) \space)

(defn new-board [] ((apply comp (replicate DIMENSIONS vecS)) \space))

(def game (atom {:board (new-board) :player \o :score {\x 0 \o 0}}))

(defn turn-player! [] (:player (swap! game update-in [:player] #(if (= % \x) \o \x))))
(defn next-player [player] (if (= player \x) \o \x))
;(turn-player!)

;after you spend a day golfing the interface to consist of three simple functions, you need a
;tracer to actually understand what you've written,  and why it breaks so bad
(defn compute-score [board coord player]
;  (for [[start dir] (g/diagonals-over coord SIZE)
;        t (range 0 SIZE)])
   (count (filter (fn [[start dir]]
                    (every? #(= player (get-in board (g/line-point start dir %))) (range 0 SIZE)))
                  (g/diagonals-over coord SIZE))))

(defn move [game coord player]
  (if (= \space (get-in game (cons :board coord)))
    (let [new-board (assoc-in (:board game) coord player)
          new-player (next-player player)
          game-over? (not-any? #(= \space %) (flatten new-board))
          score (compute-score new-board coord player)]
    {:board new-board
     :player new-player
     :score (update (game :score) player + score)
     :game-over game-over?})))

;(trace/trace-forms {:tracer trace/default-tracer}
(defn show-cell [cell coords nest]
  [:td {:title (str coords)
        :on-click #(swap! game merge (move @game coords (:player @game)))}
       cell])

(defn show-row [row coords nest]
  (cons :tr (nest)))

(defn show-plane [plane coords nest]
  (cons :table.plane (nest)))

(defn show-cube [cube coords nest]
  (cons :div.cube (nest)))

(defn show-hypercube [hc coords nest]
  (cons :div.hypercube (nest)))

(merge {:a 1} {:a nil})
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

(defn show-score [score]
  [:h4 (cons "Score: " (map #(str (first %) ": " (second %) " ") score))])

;; -------------------------
;; Views
(defn home-page []
    [:div [:h2 "Tic Tac Toe 3D"]
     (show-score (@game :score))
     (let [depth (loop [[f & r] (@game :board) d 1]
                   (if (seqable? f) (recur f (inc d)) d))
           all-renderers [show-hypercube show-cube show-plane show-row show-cell]
           renderers (take-last (inc depth) all-renderers)]
       (render (@game :board) renderers))])

(defn about-page []
  [:div [:h2 "About ttoe-3d"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn game-over []
  [:div [:h1 "Game Over"] (show-score (@game :score)) [:a {:href "#/"
                                                           :on-click #(do (swap! game assoc
                                                                                 :board (new-board)
                                                                                 :game-over false)
                                                                        true)}
                                   "New game"]])

(defn current-page []
 (if (@game :game-over)
   (do (navigate! "/game-over") [:div [game-over]])
   [:div [(session/get :current-page)]]))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/game-over" []
  (session/put! :current-page game-over))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

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
