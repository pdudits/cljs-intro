(ns ttoe-3d.geom)

(defn scalar* [vctr scalar] (map (partial * scalar) vctr))
(defn dot [vctr1 vctr2] (reduce + (map * vctr1 vctr2)))
(defn vec+ [vctr1 vctr2] (map + vctr1 vctr2))
(defn vec- [vctr1 vctr2] (map - vctr1 vctr2))
(defn line-point [start dir distance] (vec+ start (scalar* dir distance)))

(defn valid-line? [source dir bound]
  "Vector (source + bound*dir) is between [0] and [bound]"
  (every? #(<= 0 % (dec bound)) (line-point source dir (dec bound))))

(defn codirectional? [v1 v2]
  ;; vectors are codirectional if
  ;; v1 . v2 = ||v1|| ||v2||; given ||v||^2 = v . v
  ;; (v1 . v2)^2 = (v1 . v1) (v2 . v2)
  (let [prod (dot v1 v2)]
    (= (* prod prod) (* (dot v1 v1) (dot v2 v2)))))

; (codirectional? [1 1] [-1 -1])
; (codirectional? [1 1] [-1  1])
; (codirectional? [0 0 1] [1 0 1])

(defn coll-exp [coll exponent]
  (condp = exponent
    0 []
    1 (map vector coll)
    (mapcat (fn [o] (map #(conj o %) coll)) (coll-exp coll (dec exponent)))))

(comment
(defn coll-exp-for-comprehension [coll exponent]
  "Make cartesian exponent of collection as a vector"
  (condp = exponent
    0 []
    1 coll
    (for [r (coll-exp coll (dec exponent)) ;for each element of coll^n-1
          c coll] ; and each element of call
      (if (vector? r)
        (conj r c) ; construct [ coll^n-1 c ]
        (vector r c))))) ; handling of 2d case
  )

(defn diagonals "Return all diagonal direction in n dimensions"
  [n] {:pre (> 1 n)}
  (let [directions [1 0 -1]] ;thread last
    (->> (coll-exp directions n) ;all possible diagonals
        (filter (fn [v] (not-every? #(= % 0) v))) ;without zero vector
        (reduce (fn [ind v] (if (some #(codirectional? % v) ind)
                              ind
                              (conj ind v))) ; add any linearly independent direction
                #{})))) ; a set

; be very careful if you really defined a anonymous function or you just think so and forgot #()

(defn diagonals-over "All diagonal lines crossing the coordinate" [point bound]
  (let [in-bounds? (fn [v] (every? #(< -1 % bound) v))]
    (for [dir (diagonals (count point))
          t (range 0 bound)
          :let [start (line-point point dir (- t))
                end (line-point point dir (- bound 1 t))]
          :when (every? in-bounds? (list start end))]
            [(vec start) dir])))

;(diagonals-over [2 2] 5)
;(diagonals-over [0 0 0] 4)
;(every? (fn [v] (every? #(< -1 % 4) v)) (list '(0 0) '(3 3)))




