(ns ttoe-3d.core-test
  (:require [cemerick.cljs.test :refer-macros [is are deftest testing use-fixtures done]]
            [reagent.core :as reagent :refer [atom]]
            [ttoe-3d.core :as rc]))


(def isClient (not (nil? (try (.-document js/window)
                              (catch js/Object e nil)))))

(def rflush reagent/flush)

(defn add-test-div [name]
  (let [doc     js/document
        body    (.-body js/document)
        div     (.createElement doc "div")]
    (.appendChild body div)
    div))

(defn with-mounted-component [comp f]
  (when isClient
    (let [div (add-test-div "_testreagent")]
      (let [comp (reagent/render-component comp div #(f comp div))]
        (reagent/unmount-component-at-node div)
        (reagent/flush)
        (.removeChild (.-body js/document) div)))))


(defn found-in [re div]
  (let [res (.-innerHTML div)]
    (if (re-find re res)
      true
      (do (println "Not found: " res)
          false))))

(deftest test-home
  (with-mounted-component (rc/home-page)
    (fn [c div]
      (is (found-in #"Tic Tac Toe" div)))))


; Test the refactoring. These are old definitions with event hanlder dropped
(defn show-row [i row]
  (into [:tr] (map-indexed (fn [j c]
                      [:td {:title (str "[" i " " j "]")} c]) row)))
(defn show-plane [plane]
  (into [:table.plane] (map-indexed show-row plane)))

(defn show-cell [cell coords nest]
  [:td {:title (str coords)} cell])

(deftest refactor-equal
  (is (=
       (show-plane rc/board)
       (rc/render rc/board [rc/show-plane rc/show-row show-cell]))))

(deftest refactor-equal-row
  (let [row [1 2 3 4]]
  (is (=
       (show-row 1 row)
       (rc/render row [1] [rc/show-row show-cell])))))
